package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PinJpaRepository extends JpaRepository<Pin, Long>, PinRepository {
    List<Pin> findAllByPlayIdOrderByCreatedAtDesc(Long playId);

    @Override
    default List<Pin> findAllByPlayId(Long playId) {
        return findAllByPlayIdOrderByCreatedAtDesc(playId);
    }
}
