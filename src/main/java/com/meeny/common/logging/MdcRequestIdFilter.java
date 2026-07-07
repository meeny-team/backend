package com.meeny.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 단위 MDC "requestId" 주입 필터.
 *
 * <p>왜 필요한가: prod 로그가 이미 JSON (LogstashEncoder + includeMdc) 인데,
 * 여러 시스템 (ALB / EC2 / RDS / SSM / Sentry) 을 걸친 디버깅 시 로그 라인
 * 사이 상관관계를 잇는 키가 없었다 (2026-06-01 인시던트 회고).
 *
 * <p>동작:
 * <ul>
 *   <li>클라이언트가 {@code X-Request-Id} 헤더를 보내면 그 값을 사용 (최대 128자, 화이트리스트 문자만),
 *       아니면 서버가 {@code UUID.randomUUID()} 생성</li>
 *   <li>MDC 에 "requestId" 로 주입 → LogstashEncoder 가 JSON 필드로 자동 emit</li>
 *   <li>응답 헤더에도 동일 값 반환 → 클라이언트가 Sentry 등에 첨부해 상관 가능</li>
 *   <li>{@code finally} 에서 MDC.clear 로 스레드 재사용 시 누출 방지</li>
 * </ul>
 *
 * <p>Ordered.HIGHEST_PRECEDENCE 로 등록해 Spring Security 필터 체인 이전에
 * 실행 — auth 실패 응답에도 requestId 가 포함된다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "requestId";
    public static final String HEADER_NAME = "X-Request-Id";

    private static final int MAX_INBOUND_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 클라이언트 헤더 값은 신뢰할 수 없으므로, 길이 + 문자 화이트리스트로 sanitize.
     * 문자셋: 영숫자, hyphen, underscore, dot — UUID / trace id 형식 커버.
     */
    private String resolveRequestId(HttpServletRequest request) {
        String inbound = request.getHeader(HEADER_NAME);
        if (inbound != null
                && !inbound.isEmpty()
                && inbound.length() <= MAX_INBOUND_LENGTH
                && inbound.chars().allMatch(this::isAllowed)) {
            return inbound;
        }
        return UUID.randomUUID().toString();
    }

    private boolean isAllowed(int ch) {
        return (ch >= '0' && ch <= '9')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || ch == '-' || ch == '_' || ch == '.';
    }
}
