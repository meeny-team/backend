package com.meeny.application.pin;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinRepository;
import com.meeny.domain.activity.pin.Split;
import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.infrastructure.aws.S3UrlSigner;
import com.meeny.presentation.pin.dto.CreatePinRequest;
import com.meeny.presentation.pin.dto.PinResponse;
import com.meeny.presentation.pin.dto.SplitDto;
import com.meeny.presentation.pin.dto.UpdatePinRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinService {

    private final PinRepository pinRepository;
    private final PlayRepository playRepository;
    private final CrewRepository crewRepository;
    private final S3UrlSigner imageSigner;

    // 핀 생성: 작성자가 Play 멤버인지 검증한 뒤 정산 정보까지 포함해 저장; 마감된 Play엔 추가 불가
    @Transactional
    public PinResponse create(Long authorId, CreatePinRequest request) {
        Play play = findPlay(request.playId());
        play.verifyMutable();
        verifyPlayMember(play, authorId);

        List<Split> splits = request.splits().stream().map(SplitDto::toDomain).toList();

        Pin pin = Pin.create(
                play.getId(),
                authorId,
                request.amount(),
                request.category(),
                request.title(),
                request.memo(),
                request.location(),
                request.images(),
                request.settlement().toDomain(),
                splits,
                play.getMemberIds()
        );
        return PinResponse.from(pinRepository.save(pin), imageSigner);
    }

    // 특정 Play에 속한 핀 목록 조회 (크루 멤버에게만 노출)
    public List<PinResponse> getPinsByPlay(Long playId, Long memberId) {
        Play play = findPlay(playId);
        verifyPlayCrewMember(play, memberId);
        return pinRepository.findAllByPlayId(playId).stream()
                .map(p -> PinResponse.from(p, imageSigner))
                .toList();
    }

    // 핀 단건 조회 (크루 멤버에게만 노출)
    public PinResponse getPin(Long pinId, Long memberId) {
        Pin pin = findPin(pinId);
        Play play = findPlay(pin.getPlayId());
        verifyPlayCrewMember(play, memberId);
        return PinResponse.from(pin, imageSigner);
    }

    // 핀 수정: 작성자 권한 검증과 정산/분배 갱신은 도메인에서 수행; 마감된 Play의 핀은 수정 불가
    @Transactional
    public PinResponse update(Long pinId, Long memberId, UpdatePinRequest request) {
        Pin pin = findPin(pinId);
        Play play = findPlay(pin.getPlayId());
        play.verifyMutable();

        List<Split> splits = request.splits() == null
                ? null
                : request.splits().stream().map(SplitDto::toDomain).toList();

        pin.updateBy(
                memberId,
                request.amount(),
                request.category(),
                request.title(),
                request.memo(),
                request.location(),
                request.images(),
                request.settlement() == null ? null : request.settlement().toDomain(),
                splits,
                play.getMemberIds()
        );
        return PinResponse.from(pin, imageSigner);
    }

    // 핀 삭제: 작성자 본인만 가능; 마감된 Play의 핀은 삭제 불가
    @Transactional
    public void delete(Long pinId, Long memberId) {
        Pin pin = findPin(pinId);
        Play play = findPlay(pin.getPlayId());
        play.verifyMutable();
        pin.verifyAuthor(memberId);
        pinRepository.delete(pin);
    }

    private Pin findPin(Long pinId) {
        return pinRepository.findById(pinId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIN_NOT_FOUND));
    }

    private Play findPlay(Long playId) {
        return playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
    }

    private void verifyPlayMember(Play play, Long memberId) {
        if (!play.isMember(memberId)) {
            throw new BusinessException(ErrorCode.NOT_PLAY_MEMBER);
        }
    }

    private void verifyPlayCrewMember(Play play, Long memberId) {
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);
    }
}
