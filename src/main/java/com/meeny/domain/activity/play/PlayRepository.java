package com.meeny.domain.activity.play;

import java.util.List;
import java.util.Optional;

public interface PlayRepository {
    Play save(Play play);
    Optional<Play> findById(Long id);
    List<Play> findAllByCrewId(Long crewId);
    void delete(Play play);
}
