package com.meeny.presentation.play.dto;

import com.meeny.domain.play.DateRange;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DateRangeDto(
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate start,

        LocalDate end
) {
    public DateRange toDomain() {
        return DateRange.of(start, end);
    }

    public static DateRangeDto from(DateRange dateRange) {
        if (dateRange == null) return null;
        return new DateRangeDto(dateRange.getStart(), dateRange.getEnd());
    }
}
