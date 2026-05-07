package com.meeny.infrastructure.aws;

import lombok.RequiredArgsConstructor;
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

        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
