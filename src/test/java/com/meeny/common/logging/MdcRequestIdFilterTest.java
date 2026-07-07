package com.meeny.common.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcRequestIdFilterTest {

    private final MdcRequestIdFilter filter = new MdcRequestIdFilter();

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    @DisplayName("헤더 없으면 UUID 생성 + MDC + 응답 헤더 세팅")
    void generatesUuid_whenNoInboundHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> capturedDuringChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                capturedDuringChain.set(MDC.get(MdcRequestIdFilter.MDC_KEY)));

        String header = response.getHeader(MdcRequestIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();
        assertThat(capturedDuringChain.get()).isEqualTo(header);
    }

    @Test
    @DisplayName("유효한 헤더 값이 있으면 그대로 사용")
    void reusesInboundHeader_whenValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcRequestIdFilter.HEADER_NAME, "abc-123_XYZ.7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                captured.set(MDC.get(MdcRequestIdFilter.MDC_KEY)));

        assertThat(captured.get()).isEqualTo("abc-123_XYZ.7");
        assertThat(response.getHeader(MdcRequestIdFilter.HEADER_NAME)).isEqualTo("abc-123_XYZ.7");
    }

    @Test
    @DisplayName("허용되지 않는 문자 포함 → 무시하고 UUID 생성")
    void ignoresInboundHeader_whenContainsDisallowedChars() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcRequestIdFilter.HEADER_NAME, "bad value with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                captured.set(MDC.get(MdcRequestIdFilter.MDC_KEY)));

        assertThat(captured.get()).isNotEqualTo("bad value with spaces");
        assertThat(captured.get()).matches("[0-9a-f-]{36}"); // UUID
    }

    @Test
    @DisplayName("128자 초과 헤더 값 → 무시")
    void ignoresInboundHeader_whenTooLong() throws Exception {
        String tooLong = "a".repeat(129);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcRequestIdFilter.HEADER_NAME, tooLong);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
                captured.set(MDC.get(MdcRequestIdFilter.MDC_KEY)));

        assertThat(captured.get()).isNotEqualTo(tooLong);
    }

    @Test
    @DisplayName("chain 이후 MDC 는 반드시 clear")
    void clearsMdc_afterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { /* no-op */ });

        assertThat(MDC.get(MdcRequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("chain 이 예외를 던져도 MDC 는 clear")
    void clearsMdc_evenOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain throwing = (req, res) -> {
            throw new RuntimeException("boom");
        };

        try {
            filter.doFilter(request, response, throwing);
        } catch (Exception ignored) {
            // expected
        }

        assertThat(MDC.get(MdcRequestIdFilter.MDC_KEY)).isNull();
    }
}
