package com.meeny.domain.activity.pin;

public record Transfer(
        Long fromMemberId,
        Long toMemberId,
        long amount
) {}
