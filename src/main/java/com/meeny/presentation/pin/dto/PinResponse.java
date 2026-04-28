package com.meeny.presentation.pin.dto;

import com.meeny.domain.pin.Pin;
import com.meeny.domain.pin.PinCategory;

import java.time.LocalDateTime;
import java.util.List;

public record PinResponse(
        Long id,
        Long playId,
        Long authorId,
        long amount,
        PinCategory category,
        String title,
        String memo,
        String location,
        List<String> images,
        SettlementDto settlement,
        List<SplitDto> splits,
        LocalDateTime createdAt
) {
    public static PinResponse from(Pin pin) {
        return new PinResponse(
                pin.getId(),
                pin.getPlayId(),
                pin.getAuthorId(),
                pin.getAmount(),
                pin.getCategory(),
                pin.getTitle(),
                pin.getMemo(),
                pin.getLocation(),
                pin.getImages(),
                SettlementDto.from(pin.getSettlement()),
                pin.getSplits().stream().map(SplitDto::from).toList(),
                pin.getCreatedAt()
        );
    }
}
