package com.meeny.config;

import io.sentry.SentryOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sentry 추가 설정 — yaml 로 표현 어려운 부분만 코드로.
 *
 * BeforeSendCallback: 이벤트 송신 전에 Authorization / Cookie 헤더를 스크럽.
 * send-default-pii=false 만으로는 헤더 자체가 제거되지 않으므로 명시적으로 지운다.
 *
 * dsn 이 비어 있으면 (@ConditionalOnProperty 로) 이 빈은 등록조차 되지 않는다.
 */
@Configuration
@ConditionalOnProperty(prefix = "sentry", name = "dsn")
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback sentryBeforeSendCallback() {
        return (event, hint) -> {
            if (event.getRequest() != null && event.getRequest().getHeaders() != null) {
                event.getRequest().getHeaders().keySet().removeIf(name -> {
                    String lower = name.toLowerCase();
                    return lower.equals("authorization")
                            || lower.equals("cookie")
                            || lower.equals("set-cookie");
                });
            }
            return event;
        };
    }
}
