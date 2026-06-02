package com.meeny.infrastructure.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class S3CleanupListener {

    private final S3Storage s3Storage;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCleanup(S3CleanupEvent event) {
        if (event.urls() == null) return;
        for (String url : event.urls()) {
            String key = s3Storage.keyOf(url);
            if (key != null) s3Storage.deleteQuietly(key);
        }
    }
}
