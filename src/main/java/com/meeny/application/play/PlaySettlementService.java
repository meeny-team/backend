package com.meeny.application.play;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinRepository;
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

    // 정산 마감: 작성자만 가능, 모든 잔액이 0이어야 마감 가능. 마감 후엔 핀/멤버 mutation이 모두 차단됨
    @Transactional
    public PlaySettlementResponse close(Long playId, Long memberId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        List<Pin> pins = pinRepository.findAllByPlayId(playId);
        PlaySettlementResult result = PlaySettlementResult.of(play.getMemberIds(), pins);
        if (result.memberBalances().stream().anyMatch(b -> b.balance() != 0L)) {
            throw new BusinessException(ErrorCode.PLAY_NOT_SETTLEABLE);
        }

        play.close(memberId);
        Map<Long, String> nicknames = loadDisplayNicknames(result);
        return PlaySettlementResponse.from(playId, play.getSettledAt(), result, nicknames);
    }

    // Play 단위 정산 계산: 모든 핀의 결제/분배를 합산해 멤버별 잔액과 송금 내역을 산출, 닉네임은 일괄 조회로 주입
    public PlaySettlementResponse calculate(Long playId, Long memberId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        List<Pin> pins = pinRepository.findAllByPlayId(playId);
        PlaySettlementResult result = PlaySettlementResult.of(play.getMemberIds(), pins);
        Map<Long, String> nicknames = loadDisplayNicknames(result);
        return PlaySettlementResponse.from(playId, play.getSettledAt(), result, nicknames);
    }

    // 결과에 등장하는 모든 멤버 ID(탈퇴자 포함)를 한 번에 조회해 표시용 닉네임 맵 생성
    private Map<Long, String> loadDisplayNicknames(PlaySettlementResult result) {
        Set<Long> ids = result.memberBalances().stream()
                .map(MemberBalance::memberId)
                .collect(Collectors.toSet());
        return memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, Member::getDisplayNickname));
    }
}
