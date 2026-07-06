package com.meeny.presentation.pin.dto;

import com.meeny.domain.activity.pin.PinCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreatePinRequest(
        @NotNull(message = "playId는 필수입니다.")
        Long playId,

        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        long amount,

        @NotNull(message = "카테고리는 필수입니다.")
        PinCategory category,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
        String memo,

        @Size(max = 200, message = "장소는 200자 이하여야 합니다.")
        String location,

        // 지도 마커용 좌표 (선택). 위/경도는 쌍으로만 저장됨 — 하나만 보내면 무시된다.
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        Double longitude,

        List<@NotBlank String> images,

        @Valid
        @NotNull(message = "정산 정보는 필수입니다.")
        SettlementDto settlement,

        @Valid
        @NotEmpty(message = "분담 내역은 비어 있을 수 없습니다.")
        List<SplitDto> splits
) {}
