package com.meeny.infrastructure.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
public class S3Config {

    private final S3Properties properties;

    // S3 presigned URL 발급용 클라이언트.
    //   - 정적 키 있음(local/dev .env)        → StaticCredentialsProvider
    //   - 키 없고 bucket만 있음(EC2/ECS prod) → DefaultCredentialsProvider (env → 컨테이너 메타 → IMDS 인스턴스 role)
    //   - 아무것도 없음(dev 부팅 fallback)     → AnonymousCredentialsProvider (실제 호출 시 UPLOAD_NOT_CONFIGURED로 차단)
    @Bean
    public S3Presigner s3Presigner() {
        Region region = Region.of(properties.region() == null || properties.region().isBlank()
                ? "ap-northeast-2"
                : properties.region());

        AwsCredentialsProvider credentialsProvider;
        if (properties.hasStaticCredentials()) {
            credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    properties.accessKeyId(), properties.secretAccessKey()));
        } else if (properties.isConfigured()) {
            credentialsProvider = DefaultCredentialsProvider.create();
        } else {
            credentialsProvider = AnonymousCredentialsProvider.create();
        }

        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
