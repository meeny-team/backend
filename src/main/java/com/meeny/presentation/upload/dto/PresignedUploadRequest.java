package com.meeny.presentation.upload.dto;

import com.meeny.application.upload.UploadPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUploadRequest(
        @NotNull(message = "용도(purpose)는 필수입니다.")
        UploadPurpose purpose,

        @NotBlank(message = "contentType은 필수입니다.")
        String contentType
) {}
