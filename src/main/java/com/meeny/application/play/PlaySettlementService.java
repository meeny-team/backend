package com.meeny.application.play;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.crew.Crew;
import com.meeny.domain.crew.CrewRepository;
import com.meeny.domain.pin.Pin;
import com.meeny.domain.pin.PinRepository;
import com.meeny.domain.play.Play;
import com.meeny.domain.play.PlayRepository;
import com.meeny.domain.play.PlaySettlementResult;
import com.meeny.presentation.play.dto.PlaySettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaySettlementService {

    private final PlayRepository playRepository;
    private final PinRepository pinRepository;
    private final CrewRepository crewRepository;

    public PlaySettlementResponse calculate(Long playId, Long memberId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        List<Pin> pins = pinRepository.findAllByPlayId(playId);
        PlaySettlementResult result = PlaySettlementResult.of(play.getMemberIds(), pins);
        return PlaySettlementResponse.from(playId, result);
    }
}
