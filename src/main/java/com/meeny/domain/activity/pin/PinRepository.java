package com.meeny.domain.activity.pin;

import java.util.List;
import java.util.Optional;

public interface PinRepository {
    Pin save(Pin pin);
    Optional<Pin> findById(Long id);
    List<Pin> findAllByPlayId(Long playId);
    void delete(Pin pin);
}
