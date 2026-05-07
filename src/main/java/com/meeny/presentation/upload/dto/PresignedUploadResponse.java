package com.meeny.presentation.upload.dto;

import com.meeny.application.upload.UploadService;

import java.time.Instant;

public record PresignedUploadResponse(
        String uploadUrl,
        String objectKey,
        String publicUrl,
        String contentType,
        Instant expiresAt
) {
    public static PresignedUploadResponse from(UploadService.PresignedUpload upload) {
        return new PresignedUploadResponse(
                upload.uploadUrl(),
                upload.objectKey(),
                upload.publicUrl(),
                upload.contentType(),
                upload.expiresAt()
        );
    }
}
