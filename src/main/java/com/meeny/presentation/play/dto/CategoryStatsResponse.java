package com.meeny.presentation.play.dto;

import com.meeny.domain.activity.pin.PinCategory;

import java.util.List;

public record CategoryStatsResponse(
        long totalAmount,
        long totalCount,
        List<CategoryStat> byCategory
) {
    public record CategoryStat(
            PinCategory category,
            long totalAmount,
            long count,
            double percentage
    ) {}
}
