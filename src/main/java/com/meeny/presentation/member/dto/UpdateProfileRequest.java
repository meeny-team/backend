package com.meeny.presentation.member.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname,

        String profileImage,

        @Size(max = 100, message = "소개는 100자 이하여야 합니다.")
        String bio
) {}
