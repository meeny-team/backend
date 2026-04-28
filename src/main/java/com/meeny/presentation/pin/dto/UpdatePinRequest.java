package com.meeny.presentation.pin.dto;

import com.meeny.domain.pin.PinCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePinRequest(
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        Long amount,

        PinCategory category,

        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
        String memo,

        @Size(max = 200, message = "장소는 200자 이하여야 합니다.")
        String location,

        List<String> images,

        @Valid
        SettlementDto settlement,

        @Valid
        List<SplitDto> splits
) {}
