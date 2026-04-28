package com.meeny.presentation.crew.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinByCodeRequest(
        @NotBlank(message = "초대 코드는 필수입니다.")
        String inviteCode
) {}
