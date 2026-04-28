package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.activity.play.Play;
import com.meeny.domain.activity.play.PlayRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayJpaRepository extends JpaRepository<Play, Long>, PlayRepository {
    List<Play> findAllByCrewIdOrderByCreatedAtDesc(Long crewId);

    @Override
    default List<Play> findAllByCrewId(Long crewId) {
        return findAllByCrewIdOrderByCreatedAtDesc(crewId);
    }
}
