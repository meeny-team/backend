package com.meeny.application.play;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.activity.crew.Crew;
import com.meeny.domain.activity.crew.CrewRepository;
import com.meeny.domain.activity.pin.PinRepository;
import com.meeny.domain.activity.pin.PinRepository.CategoryAggregate;
import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.presentation.play.dto.CategoryStatsResponse;
import com.meeny.presentation.play.dto.CategoryStatsResponse.CategoryStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayStatsService {

    private final PlayRepository playRepository;
    private final CrewRepository crewRepository;
    private final PinRepository pinRepository;

    // 특정 플레이의 카테고리별 지출 통계. 크루 멤버에게만 노출.
    public CategoryStatsResponse statsByPlay(Long playId, Long memberId) {
        Play play = playRepository.findById(playId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAY_NOT_FOUND));
        Crew crew = crewRepository.findById(play.getCrewId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        return buildResponse(pinRepository.aggregateByPlay(playId));
    }

    // 크루 단위. 기간 옵셔널 (from 포함, to 제외).
    public CategoryStatsResponse statsByCrew(Long crewId, Long memberId, LocalDateTime from, LocalDateTime to) {
        Crew crew = crewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_NOT_FOUND));
        crew.verifyMember(memberId);

        return buildResponse(pinRepository.aggregateByCrew(crewId, from, to));
    }

    // 합계가 0이어도 안전: percentage 분모 가드.
    private CategoryStatsResponse buildResponse(List<CategoryAggregate> aggregates) {
        long totalAmount = aggregates.stream().mapToLong(CategoryAggregate::totalAmount).sum();
        long totalCount = aggregates.stream().mapToLong(CategoryAggregate::count).sum();
        List<CategoryStat> byCategory = aggregates.stream()
                .map(a -> new CategoryStat(
                        a.category(),
                        a.totalAmount(),
                        a.count(),
                        totalAmount == 0 ? 0.0 : (a.totalAmount() * 100.0 / totalAmount)))
                .sorted(Comparator.comparingLong(CategoryStat::totalAmount).reversed())
                .toList();
        return new CategoryStatsResponse(totalAmount, totalCount, byCategory);
    }
}
