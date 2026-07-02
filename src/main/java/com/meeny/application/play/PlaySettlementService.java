package com.meeny.application.play;

import com.meeny.application.activity.ActivityLogService;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.log.ActivityType;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinRepository;
import com.meeny.domain.activity.pin.PinTransferMark;
import com.meeny.domain.activity.pin.PinTransferMarkRepository;
import com.meeny.domain.activity.pin.Split;
import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.domain.activity.pin.MemberBalance;
import com.meeny.domain.activity.pin.PlaySettlementResult;
import com.meeny.domain.identity.Member;
import com.meeny.domain.identity.MemberRepository;
import com.meeny.presentation.play.dto.PlaySettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaySettlementService {

    private final PlayRepository playRepository;
    private final PinRepository pinRepository;
    private final CrewRepository crewRepository;
    private final MemberRepository memberRepository;
    private final PinTransferMarkRepository pinTransferMarkRepository;
    private final ActivityLogService activityLogService;

    // 정산 마감: 작성자만 가능. 모든 (paidBy != fromId, amount > 0) split 이 received 마킹되어 있어야 마감 가능.
    // 마감 후엔 핀/멤버 mutation 이 모두 차단됨.
    @Transactional
    public PlaySettlementResponse close(Long playId, Long memberId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        List<Pin> pins = pinRepository.findAllByPlayId(playId);
        List<PinTransferMark> marks = loadMarks(pins);
        if (!allTransfersReceived(pins, marks)) {
            throw new BusinessException(ErrorCode.PLAY_NOT_SETTLEABLE);
        }

        play.close(memberId);
        activityLogService.record(play.getCrewId(), memberId, ActivityType.PLAY_SETTLED,
                Map.of("playId", playId, "playTitle", play.getTitle()));
        PlaySettlementResult result = PlaySettlementResult.of(play.getMemberIds(), pins);
        Map<Long, Member> memberIndex = loadMemberIndex(result);
        return PlaySettlementResponse.from(playId, play.getSettledAt(), result, pins, marks, memberIndex);
    }

    // 작성자 강제 마감: 모든 송금이 received 되지 않아도 마감. 미수신 송금 카운트를 ActivityLog 에 남겨 감사 추적 가능.
    // 사용 시나리오: 수신자가 받음 체크를 안 해줘서 정산이 영구 lock 되는 데드락 해소.
    @Transactional
    public PlaySettlementResponse forceClose(Long playId, Long memberId, String reason) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        List<Pin> pins = pinRepository.findAllByPlayId(playId);
        List<PinTransferMark> marks = loadMarks(pins);
        int unreceived = countUnreceivedTransfers(pins, marks);

        play.close(memberId); // Play.close 가 verifyAuthor + 중복 마감 가드 수행
        activityLogService.record(play.getCrewId(), memberId, ActivityType.PLAY_FORCE_SETTLED,
                Map.of(
                        "playId", playId,
                        "playTitle", play.getTitle(),
                        "unreceivedTransferCount", unreceived,
                        "reason", reason == null ? "" : reason
                ));
        PlaySettlementResult result = PlaySettlementResult.of(play.getMemberIds(), pins);
        Map<Long, Member> memberIndex = loadMemberIndex(result);
        return PlaySettlementResponse.from(playId, play.getSettledAt(), result, pins, marks, memberIndex);
    }

    // Play 단위 정산 계산: 모든 핀의 결제/분배를 합산해 멤버별 잔액과 송금 내역을 산출, 닉네임은 일괄 조회로 주입
    public PlaySettlementResponse calculate(Long playId, Long memberId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        List<Pin> pins = pinRepository.findAllByPlayId(playId);
        List<PinTransferMark> marks = loadMarks(pins);
        PlaySettlementResult result = PlaySettlementResult.of(play.getMemberIds(), pins);
        Map<Long, Member> memberIndex = loadMemberIndex(result);
        return PlaySettlementResponse.from(playId, play.getSettledAt(), result, pins, marks, memberIndex);
    }

    // 송신자가 "보냈음" 표시. 권한: from = caller. row 없으면 생성, 있으면 idempotent.
    @Transactional
    public PlaySettlementResponse markTransferSent(Long playId, Long pinId, Long fromMemberId, Long toMemberId, Long callerId) {
        if (!fromMemberId.equals(callerId)) {
            throw new BusinessException(ErrorCode.TRANSFER_FORBIDDEN);
        }
        Pin pin = preparePinForMark(playId, pinId, callerId);
        verifyTransferExists(pin, fromMemberId, toMemberId);

        boolean created = pinTransferMarkRepository
                .findByPinIdAndFromMemberIdAndToMemberId(pinId, fromMemberId, toMemberId)
                .isEmpty();
        if (created) {
            pinTransferMarkRepository.save(PinTransferMark.createSent(pinId, fromMemberId, toMemberId));
            long amount = transferAmount(pin, fromMemberId);
            activityLogService.record(preparePinCrew(pin), callerId, ActivityType.TRANSFER_SENT,
                    Map.of("playId", playId, "pinId", pinId, "pinTitle", pin.getTitle(),
                            "fromMemberId", fromMemberId, "toMemberId", toMemberId, "amount", amount));
        }
        // 이미 sent 상태였으면 idempotent — 로그 중복 적재 안 함

        return calculate(playId, callerId);
    }

    // 수신자(paidBy) 가 "받았음" 표시. 권한: to = caller. sent row 가 있어야 함.
    @Transactional
    public PlaySettlementResponse markTransferReceived(Long playId, Long pinId, Long fromMemberId, Long toMemberId, Long callerId) {
        if (!toMemberId.equals(callerId)) {
            throw new BusinessException(ErrorCode.TRANSFER_FORBIDDEN);
        }
        Pin pin = preparePinForMark(playId, pinId, callerId);
        verifyTransferExists(pin, fromMemberId, toMemberId);

        PinTransferMark mark = pinTransferMarkRepository
                .findByPinIdAndFromMemberIdAndToMemberId(pinId, fromMemberId, toMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
        mark.markReceived();
        long amount = transferAmount(pin, fromMemberId);
        activityLogService.record(preparePinCrew(pin), callerId, ActivityType.TRANSFER_RECEIVED,
                Map.of("playId", playId, "pinId", pinId, "pinTitle", pin.getTitle(),
                        "fromMemberId", fromMemberId, "toMemberId", toMemberId, "amount", amount));

        return calculate(playId, callerId);
    }

    // 수신자(paidBy) 가 "받음" 표시를 잘못 누른 경우 되돌림. 권한: to = caller. sent row 는 유지(수신자만 다시 받음 표시 가능).
    @Transactional
    public PlaySettlementResponse cancelTransferReceived(Long playId, Long pinId, Long fromMemberId, Long toMemberId, Long callerId) {
        if (!toMemberId.equals(callerId)) {
            throw new BusinessException(ErrorCode.TRANSFER_FORBIDDEN);
        }
        Pin pin = preparePinForMark(playId, pinId, callerId);
        verifyTransferExists(pin, fromMemberId, toMemberId);

        PinTransferMark mark = pinTransferMarkRepository
                .findByPinIdAndFromMemberIdAndToMemberId(pinId, fromMemberId, toMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
        mark.cancelReceived();
        long amount = transferAmount(pin, fromMemberId);
        activityLogService.record(preparePinCrew(pin), callerId, ActivityType.TRANSFER_RECEIVE_CANCELED,
                Map.of("playId", playId, "pinId", pinId, "pinTitle", pin.getTitle(),
                        "fromMemberId", fromMemberId, "toMemberId", toMemberId, "amount", amount));

        return calculate(playId, callerId);
    }

    // 송신자가 "보냈음" 을 취소. 권한: from = caller. received 이후엔 거절. row 자체 삭제.
    @Transactional
    public PlaySettlementResponse cancelTransferSent(Long playId, Long pinId, Long fromMemberId, Long toMemberId, Long callerId) {
        if (!fromMemberId.equals(callerId)) {
            throw new BusinessException(ErrorCode.TRANSFER_FORBIDDEN);
        }
        preparePinForMark(playId, pinId, callerId);

        PinTransferMark mark = pinTransferMarkRepository
                .findByPinIdAndFromMemberIdAndToMemberId(pinId, fromMemberId, toMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
        mark.verifyCancellable();
        pinTransferMarkRepository.delete(mark);

        return calculate(playId, callerId);
    }

    // 마킹 endpoint 공통 전처리: play 존재/멤버/mutable + pin 존재/소속 검증
    private Pin preparePinForMark(Long playId, Long pinId, Long callerId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(callerId);
        play.verifyMutable();

        Pin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));
        if (!pin.getPlayId().equals(playId)) {
            throw new BusinessException(ErrorCode.PIN_NOT_FOUND);
        }
        return pin;
    }

    // pin 의 splits 에 (fromMemberId → paidBy=toMemberId) 송금이 실제 존재해야 마킹 가능
    private void verifyTransferExists(Pin pin, Long fromMemberId, Long toMemberId) {
        Long paidBy = pin.getSettlement().getPaidBy();
        if (!paidBy.equals(toMemberId)) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_SPLIT);
        }
        boolean exists = pin.getSplits().stream()
                .anyMatch(s -> s.getUserId().equals(fromMemberId) && s.getAmount() > 0);
        if (!exists) {
            throw new BusinessException(ErrorCode.TRANSFER_INVALID_SPLIT);
        }
    }

    // 모든 pin 의 모든 (fromMemberId, paidBy) split 이 received 마킹됨을 확인. 송금이 0건이면 true.
    private boolean allTransfersReceived(List<Pin> pins, List<PinTransferMark> marks) {
        return countUnreceivedTransfers(pins, marks) == 0;
    }

    // 아직 received 마킹되지 않은 (fromMemberId, paidBy) split 의 수. forceClose 의 감사 로그에 사용.
    private int countUnreceivedTransfers(List<Pin> pins, List<PinTransferMark> marks) {
        Map<String, PinTransferMark> markMap = marks.stream()
                .collect(Collectors.toMap(
                        m -> markKey(m.getPinId(), m.getFromMemberId(), m.getToMemberId()),
                        m -> m
                ));
        int unreceived = 0;
        for (Pin pin : pins) {
            Long paidBy = pin.getSettlement().getPaidBy();
            for (Split split : pin.getSplits()) {
                if (split.getUserId().equals(paidBy) || split.getAmount() <= 0) continue;
                PinTransferMark mark = markMap.get(markKey(pin.getId(), split.getUserId(), paidBy));
                if (mark == null || !mark.isReceived()) {
                    unreceived++;
                }
            }
        }
        return unreceived;
    }

    private List<PinTransferMark> loadMarks(List<Pin> pins) {
        if (pins.isEmpty()) return List.of();
        List<Long> pinIds = pins.stream().map(Pin::getId).toList();
        return pinTransferMarkRepository.findAllByPinIdIn(pinIds);
    }

    private static String markKey(Long pinId, Long fromMemberId, Long toMemberId) {
        return pinId + ":" + fromMemberId + ":" + toMemberId;
    }

    // pin → crewId 한 번 더 조회. 활동 로그 적재 시 crewId 필요.
    private Long preparePinCrew(Pin pin) {
        return playRepository.findById(pin.getPlayId())
                .map(Play::getCrewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
    }

    // pin 의 splits 에서 fromMemberId 의 분담액 합산 (보통 1건)
    private long transferAmount(Pin pin, Long fromMemberId) {
        return pin.getSplits().stream()
                .filter(s -> s.getUserId().equals(fromMemberId))
                .mapToLong(Split::getAmount)
                .sum();
    }

    // 결과에 등장하는 모든 멤버 ID(탈퇴자 포함)를 한 번에 조회해 memberId → Member 맵 생성.
    // DTO 팩토리에서 표시용 닉네임과 계좌 정보를 함께 추출한다.
    private Map<Long, Member> loadMemberIndex(PlaySettlementResult result) {
        Set<Long> ids = result.memberBalances().stream()
                .map(MemberBalance::memberId)
                .collect(Collectors.toSet());
        return memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, m -> m));
    }
}
