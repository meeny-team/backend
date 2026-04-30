package com.meeny.application.play;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinRepository;
import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.domain.activity.pin.PlaySettlementResult;
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

    // Play 단위 정산 계산: 모든 핀의 결제/분배를 합산해 멤버별 잔액과 송금 내역을 산출
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
