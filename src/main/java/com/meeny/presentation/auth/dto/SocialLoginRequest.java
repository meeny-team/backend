package com.meeny.presentation.auth.dto;

import com.meeny.domain.member.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(
        @NotNull(message = "provider는 필수입니다.")
        SocialProvider provider,

        @NotBlank(message = "token은 필수입니다.")
        String token,

        String nickname
) {}
