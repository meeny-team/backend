package com.meeny.domain.pin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Split {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private long amount;

    private Split(Long userId, long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public static Split of(Long userId, long amount) {
        return new Split(userId, amount);
    }
}
