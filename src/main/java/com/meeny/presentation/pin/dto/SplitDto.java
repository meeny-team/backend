package com.meeny.presentation.pin.dto;

import com.meeny.domain.pin.Split;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SplitDto(
        @NotNull(message = "userId는 필수입니다.")
        Long userId,

        @PositiveOrZero(message = "분담 금액은 0 이상이어야 합니다.")
        long amount
) {
    public Split toDomain() {
        return Split.of(userId, amount);
    }

    public static SplitDto from(Split split) {
        return new SplitDto(split.getUserId(), split.getAmount());
    }
}
