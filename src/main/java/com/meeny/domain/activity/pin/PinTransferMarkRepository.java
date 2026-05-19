package com.meeny.domain.activity.pin;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PinTransferMarkRepository {
    PinTransferMark save(PinTransferMark mark);
    Optional<PinTransferMark> findByPinIdAndFromMemberIdAndToMemberId(Long pinId, Long fromMemberId, Long toMemberId);
    List<PinTransferMark> findAllByPinIdIn(Collection<Long> pinIds);
    void delete(PinTransferMark mark);
}
