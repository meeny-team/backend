package com.meeny.domain.activity.pin;

public record MemberBalance(
        Long memberId,
        long totalPaid,
        long totalShare,
        long balance
) {}
