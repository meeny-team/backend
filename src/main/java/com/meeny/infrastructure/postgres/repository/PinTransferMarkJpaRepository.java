package com.meeny.infrastructure.postgres.repository;

import com.meeny.domain.activity.pin.PinTransferMark;
import com.meeny.domain.activity.pin.PinTransferMarkRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinTransferMarkJpaRepository
        extends JpaRepository<PinTransferMark, Long>, PinTransferMarkRepository {
}
