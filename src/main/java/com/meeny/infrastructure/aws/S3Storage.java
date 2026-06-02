package com.meeny.infrastructure.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Storage {

    private final S3Client s3Client;
    private final S3Properties properties;

    // signed/raw 어느 URL이든 우리 버킷의 객체 key 만 추출. 우리 버킷이 아니면 null.
    public String keyOf(String url) {
        if (url == null || url.isBlank() || !properties.isConfigured()) return null;
        String prefix = "https://%s.s3.%s.amazonaws.com/".formatted(properties.bucket(), properties.region());
        if (!url.startsWith(prefix)) return null;
        String rest = url.substring(prefix.length());
        int q = rest.indexOf('?');
        return q < 0 ? rest : rest.substring(0, q);
    }

    // 객체 삭제 실패는 핀 트랜잭션에 영향 주지 않게 swallow. orphan 은 S3 lifecycle 로 후행 정리.
    public void deleteQuietly(String key) {
        if (key == null || key.isBlank() || !properties.isConfigured()) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.warn("S3 객체 삭제 실패 (key={}): {}", key, e.getMessage());
        }
    }

    public void deleteAllQuietly(Collection<String> keys) {
        if (keys == null) return;
        for (String key : keys) deleteQuietly(key);
    }
}
