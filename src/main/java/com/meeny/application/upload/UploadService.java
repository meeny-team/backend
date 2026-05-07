package com.meeny.application.upload;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.infrastructure.aws.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    // 화이트리스트 외 컨텐츠 타입은 차단. 확장자는 컨텐츠 타입에서 도출 (클라이언트가 임의로 설정하지 못하게).
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final S3Presigner presigner;
    private final S3Properties properties;

    // presigned PUT URL 발급: 클라이언트가 이 URL로 직접 S3에 업로드하면 끝. 백엔드는 객체 키만 받아 저장 시 사용.
    public PresignedUpload createPresignedUpload(Long memberId, UploadPurpose purpose, String contentType) {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.UPLOAD_NOT_CONFIGURED);
        }
        if (purpose == null) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_PURPOSE);
        }
        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new BusinessException(ErrorCode.UPLOAD_INVALID_CONTENT_TYPE);
        }

        String objectKey = "%s/%d/%s.%s".formatted(
                purpose.prefix(), memberId, UUID.randomUUID(), extension);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        Duration expiry = Duration.ofMinutes(Math.max(1, properties.presignedUrlExpiryMinutes()));

        PresignedPutObjectRequest presigned = presigner.presignPutObject(b -> b
                .signatureDuration(expiry)
                .putObjectRequest(putRequest));

        String publicUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(
                properties.bucket(), properties.region(), objectKey);

        return new PresignedUpload(
                presigned.url().toString(),
                objectKey,
                publicUrl,
                contentType,
                Instant.now().plus(expiry)
        );
    }

    public record PresignedUpload(
            String uploadUrl,
            String objectKey,
            String publicUrl,
            String contentType,
            Instant expiresAt
    ) {}
}
