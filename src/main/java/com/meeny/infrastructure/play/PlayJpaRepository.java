package com.meeny.infrastructure.play;

import com.meeny.domain.play.Play;
import com.meeny.domain.play.PlayRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayJpaRepository extends JpaRepository<Play, Long>, PlayRepository {
    List<Play> findAllByCrewIdOrderByCreatedAtDesc(Long crewId);

    @Override
    default List<Play> findAllByCrewId(Long crewId) {
        return findAllByCrewIdOrderByCreatedAtDesc(crewId);
    }
}
