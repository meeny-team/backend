package com.meeny.presentation.pin.dto;

import com.meeny.domain.activity.pin.Settlement;
import com.meeny.domain.activity.pin.SettlementType;
import jakarta.validation.constraints.NotNull;

public record SettlementDto(
        @NotNull(message = "정산 타입은 필수입니다.")
        SettlementType type,

        @NotNull(message = "결제자(paidBy)는 필수입니다.")
        Long paidBy
) {
    public Settlement toDomain() {
        return Settlement.of(type, paidBy);
    }

    public static SettlementDto from(Settlement settlement) {
        return new SettlementDto(settlement.getType(), settlement.getPaidBy());
    }
}
