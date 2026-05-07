package com.meeny.presentation.member.dto;

import com.meeny.domain.identity.Member;
import com.meeny.infrastructure.aws.S3UrlSigner;

public record MemberProfileResponse(
        Long id,
        String nickname,
        String email,
        String profileImage,
        String bio
) {
    public static MemberProfileResponse from(Member member, S3UrlSigner imageSigner) {
        return new MemberProfileResponse(
                member.getId(),
                member.getNickname(),
                member.getEmail(),
                imageSigner.sign(member.getProfileImage()),
                member.getBio()
        );
    }
}
