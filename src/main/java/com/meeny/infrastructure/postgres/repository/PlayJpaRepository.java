package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import com.meeny.domain.activity.play.PlayType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayJpaRepository extends JpaRepository<Play, Long>, PlayRepository {
    List<Play> findAllByCrewIdOrderByCreatedAtDesc(Long crewId);

    @Override
    default List<Play> findAllByCrewId(Long crewId) {
        return findAllByCrewIdOrderByCreatedAtDesc(crewId);
    }

    @Query("""
        SELECT p FROM Play p
        WHERE p.crewId = :crewId
          AND (:type IS NULL OR p.type = :type)
          AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Play> searchByCrew(
            @Param("crewId") Long crewId,
            @Param("type") PlayType type,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Override
    default Page<Play> search(Long crewId, PlayType type, String keyword, Pageable pageable) {
        String trimmed = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return searchByCrew(crewId, type, trimmed, pageable);
    }
}
