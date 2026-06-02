package com.meeny.infrastructure.aws;

import java.util.List;

// 핀 update/delete 등에서 빠져나간 이미지 URL 들의 S3 객체를 트랜잭션 commit 후에 정리하기 위한 이벤트.
// 트랜잭션이 롤백되면 객체가 살아남아야 하므로 AFTER_COMMIT 단계에서만 처리한다.
public record S3CleanupEvent(List<String> urls) {}
