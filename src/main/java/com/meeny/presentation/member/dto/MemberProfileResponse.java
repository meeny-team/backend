package com.meeny.presentation.member.dto;

import com.meeny.domain.member.Member;

public record MemberProfileResponse(
        Long id,
        String nickname,
        String email,
        String profileImage,
        String bio
) {
    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                member.getProfileImage(),
                member.getBio()
        );
    }
}
