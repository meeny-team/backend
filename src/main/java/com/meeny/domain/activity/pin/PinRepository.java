package com.meeny.domain.activity.pin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PinRepository {
    Pin save(Pin pin);
    Optional<Pin> findById(Long id);
    List<Pin> findAllByPlayId(Long playId);
    void delete(Pin pin);

    // 필터 조건은 모두 null 허용 — null 인 필드는 무시. category, authorId 외에 title/memo LIKE 매칭.
    Page<Pin> search(Long playId, PinCategory category, Long authorId, String keyword, Pageable pageable);
}
