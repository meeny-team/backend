package com.meeny.infrastructure.aws;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    private final S3Properties properties;

    // S3 presigned URL 발급용 클라이언트. dev에서 bucket 미설정이어도 부팅이 깨지지 않도록 익명 credential로 폴백한다 (실제 호출 시 UPLOAD_NOT_CONFIGURED로 차단됨).
    @Bean
    public S3Presigner s3Presigner() {
        Region region = Region.of(properties.region() == null || properties.region().isBlank()
                ? "ap-northeast-2"
                : properties.region());

        AwsCredentialsProvider credentialsProvider = properties.isConfigured()
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        properties.accessKeyId(), properties.secretAccessKey()))
                : AnonymousCredentialsProvider.create();

        // 진단용 부팅 로그: secret 자체는 절대 찍지 않고 길이/접두만 찍어 키 짝 일치 여부 판별만 가능하게.
        String accessKeyId = properties.accessKeyId();
        String secret = properties.secretAccessKey();
        log.info("[S3Config] configured={} bucket={} region={} accessKeyIdPrefix={} accessKeyIdLen={} secretLen={}",
                properties.isConfigured(),
                properties.bucket(),
                region.id(),
                accessKeyId == null ? "(null)" : accessKeyId.substring(0, Math.min(6, accessKeyId.length())),
                accessKeyId == null ? 0 : accessKeyId.length(),
                secret == null ? 0 : secret.length());

        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
