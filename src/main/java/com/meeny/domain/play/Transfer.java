package com.meeny.domain.play;

public record Transfer(
        Long fromMemberId,
        Long toMemberId,
        long amount
) {}
