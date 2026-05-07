package com.meeny.infrastructure.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        String bucket,
        String region,
        String accessKeyId,
        String secretAccessKey,
        int presignedUrlExpiryMinutes,
        int readUrlExpiryMinutes
) {
    // bucket만 있으면 설정 완료로 본다. 자격증명은 정적 키(local/dev) 또는 EC2/ECS instance role(prod) 어느 쪽이든 OK.
    public boolean isConfigured() {
        return bucket != null && !bucket.isBlank();
    }

    public boolean hasStaticCredentials() {
        return accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank();
    }
}
