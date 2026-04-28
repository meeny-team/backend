package com.meeny.domain.play;

public record MemberBalance(
        Long memberId,
        long totalPaid,
        long totalShare,
        long balance
) {}
