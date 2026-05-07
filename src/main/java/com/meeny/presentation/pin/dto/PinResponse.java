package com.meeny.presentation.pin.dto;

import com.meeny.domain.activity.pin.Pin;
import com.meeny.domain.activity.pin.PinCategory;
import com.meeny.infrastructure.aws.S3UrlSigner;

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
    public static PinResponse from(Pin pin, S3UrlSigner imageSigner) {
        List<String> signedImages = pin.getImages() == null
                ? null
                : pin.getImages().stream().map(imageSigner::sign).toList();
        return new PinResponse(
                pin.getId(),
                pin.getPlayId(),
                pin.getAuthorId(),
                pin.getAmount(),
                pin.getCategory(),
                pin.getTitle(),
                pin.getMemo(),
                pin.getLocation(),
                signedImages,
                SettlementDto.from(pin.getSettlement()),
                pin.getSplits().stream().map(SplitDto::from).toList(),
                pin.getCreatedAt()
        );
    }
}
