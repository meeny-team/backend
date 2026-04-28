package com.meeny.domain.pin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false)
    private SettlementType type;

    @Column(name = "paid_by", nullable = false)
    private Long paidBy;

    private Settlement(SettlementType type, Long paidBy) {
        this.type = type;
        this.paidBy = paidBy;
    }

    public static Settlement of(SettlementType type, Long paidBy) {
        return new Settlement(type, paidBy);
    }
}
