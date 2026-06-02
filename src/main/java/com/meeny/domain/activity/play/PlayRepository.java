package com.meeny.domain.activity.play;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PlayRepository {
    Play save(Play play);
    Optional<Play> findById(Long id);
    List<Play> findAllByCrewId(Long crewId);
    void delete(Play play);

    // 필터 null 인 필드는 무시. keyword 는 title LIKE 매칭, type 은 정확 매칭.
    Page<Play> search(Long crewId, PlayType type, String keyword, Pageable pageable);
}
