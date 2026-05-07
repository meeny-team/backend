package com.meeny.infrastructure.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3UrlSigner {

    private final S3Presigner presigner;
    private final S3Properties properties;

    // DB 에 저장된 이미지 URL 을 클라이언트가 GET 가능한 presigned URL 로 변환. 우리 버킷 URL 만 변환하고 외부 URL(예: 카카오 프로필) 은 그대로.
    public String sign(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank()) return storedUrl;
        if (!properties.isConfigured()) return storedUrl;

        String prefix = "https://%s.s3.%s.amazonaws.com/".formatted(properties.bucket(), properties.region());
        if (!storedUrl.startsWith(prefix)) return storedUrl;

        String objectKey = storedUrl.substring(prefix.length());
        // 이미 presigned 인 입력은 재서명하지 않음 (DB 에는 보통 raw publicUrl 만 들어가지만 안전 차원).
        if (objectKey.contains("?")) return storedUrl;

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(objectKey)
                .build();

        Duration validity = Duration.ofMinutes(Math.max(1, properties.readUrlExpiryMinutes()));

        return presigner.presignGetObject(b -> b
                .signatureDuration(validity)
                .getObjectRequest(getRequest))
                .url()
                .toString();
    }
}
