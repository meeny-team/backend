package com.meeny.presentation.pin.dto;

import com.meeny.domain.activity.pin.PinCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

        // 좌표는 위/경도 쌍으로만 갱신 — 하나만 보내면 도메인에서 무시된다.
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        Double longitude,

        List<String> images,

        @Valid
        SettlementDto settlement,

        @Valid
        List<SplitDto> splits
) {}
