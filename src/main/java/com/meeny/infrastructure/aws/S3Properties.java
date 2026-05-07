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
    public boolean isConfigured() {
        return bucket != null && !bucket.isBlank()
                && accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank();
    }
}
