package com.meeny.domain.activity.play;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DateRange {

    @Column(name = "start_date", nullable = false)
    private LocalDate start;

    @Column(name = "end_date")
    private LocalDate end;

    private DateRange(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public static DateRange of(LocalDate start, LocalDate end) {
        if (start == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (end != null && end.isBefore(start)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        return new DateRange(start, end);
    }
}
