package com.meeny.presentation.auth.dto;

import com.meeny.domain.member.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DevLoginRequest(
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @NotBlank(message = "providerId는 필수입니다.")
        String providerId,

        String email,

        String nickname
) {}
