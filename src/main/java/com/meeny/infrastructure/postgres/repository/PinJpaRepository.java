package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinCategory;
import com.meeny.domain.activity.pin.PinRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PinJpaRepository extends JpaRepository<Pin, Long>, PinRepository {
    List<Pin> findAllByPlayIdOrderByCreatedAtDesc(Long playId);

    @Override
    default List<Pin> findAllByPlayId(Long playId) {
        return findAllByPlayIdOrderByCreatedAtDesc(playId);
    }

    // playId 는 필수, 나머지 필터는 null 이면 무시. title/memo LIKE 는 lowercase 비교.
    @Query("""
        SELECT p FROM Pin p
        WHERE p.playId = :playId
          AND (:category IS NULL OR p.category = :category)
          AND (:authorId IS NULL OR p.authorId = :authorId)
          AND (:keyword IS NULL
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(p.memo, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Pin> searchByPlay(
            @Param("playId") Long playId,
            @Param("category") PinCategory category,
            @Param("authorId") Long authorId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Override
    default Page<Pin> search(Long playId, PinCategory category, Long authorId, String keyword, Pageable pageable) {
        String trimmed = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return searchByPlay(playId, category, authorId, trimmed, pageable);
    }
}
