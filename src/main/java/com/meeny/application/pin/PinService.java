package com.meeny.application.pin;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.crew.Crew;
import com.meeny.domain.crew.CrewRepository;
import com.meeny.domain.pin.Pin;
import com.meeny.domain.pin.PinRepository;
import com.meeny.domain.pin.Split;
import com.meeny.domain.play.Play;
import com.meeny.domain.play.PlayRepository;
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

    @Transactional
    public PinResponse create(Long authorId, CreatePinRequest request) {
        Play play = findPlay(request.playId());
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
        return PinResponse.from(pinRepository.save(pin));
    }

    public List<PinResponse> getPinsByPlay(Long playId, Long memberId) {
        Play play = findPlay(playId);
        verifyPlayCrewMember(play, memberId);
        return pinRepository.findAllByPlayId(playId).stream()
                .map(PinResponse::from)
                .toList();
    }

    public PinResponse getPin(Long pinId, Long memberId) {
        Pin pin = findPin(pinId);
        Play play = findPlay(pin.getPlayId());
        verifyPlayCrewMember(play, memberId);
        return PinResponse.from(pin);
    }

    @Transactional
    public PinResponse update(Long pinId, Long memberId, UpdatePinRequest request) {
        Pin pin = findPin(pinId);
        Play play = findPlay(pin.getPlayId());

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
        return PinResponse.from(pin);
    }

    @Transactional
    public void delete(Long pinId, Long memberId) {
        Pin pin = findPin(pinId);
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
