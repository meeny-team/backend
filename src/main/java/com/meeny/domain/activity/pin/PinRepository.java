package com.meeny.domain.activity.pin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PinRepository {
    Pin save(Pin pin);
    Optional<Pin> findById(Long id);
    List<Pin> findAllByPlayId(Long playId);
    void delete(Pin pin);

    // 필터 조건은 모두 null 허용 — null 인 필드는 무시. category, authorId 외에 title/memo LIKE 매칭.
    Page<Pin> search(Long playId, PinCategory category, Long authorId, String keyword, Pageable pageable);

    // 카테고리별 합계/개수 집계. (category, totalAmount, count)
    List<CategoryAggregate> aggregateByPlay(Long playId);

    // 크루 단위. from/to null 이면 무제한. createdAt 기준.
    List<CategoryAggregate> aggregateByCrew(Long crewId, LocalDateTime from, LocalDateTime to);

    record CategoryAggregate(PinCategory category, long totalAmount, long count) {}
}
