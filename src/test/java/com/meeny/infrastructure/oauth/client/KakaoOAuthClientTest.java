package com.meeny.infrastructure.oauth.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.auth.OAuthUserInfo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoOAuthClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer server;
    private KakaoOAuthClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new KakaoOAuthClient(WebClient.builder(), server.url("/v2/user/me").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    @DisplayName("정상 응답(snake_case): providerId, email, nickname 모두 추출 + Authorization 헤더 검증")
    void getUserInfo_success() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(Map.of(
                        "id", 123456L,
                        "kakao_account", Map.of(
                                "email", "user@kakao.com",
                                "profile", Map.of("nickname", "닉네임")
                        )
                ))));

        OAuthUserInfo info = client.getUserInfo("access-token");

        assertThat(info.providerId()).isEqualTo("123456");
        assertThat(info.email()).isEqualTo("user@kakao.com");
        assertThat(info.nickname()).isEqualTo("닉네임");

        RecordedRequest req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer access-token");
    }

    @Test
    @DisplayName("kakao_account 누락: providerId만 채워지고 email/nickname null")
    void getUserInfo_missingKakaoAccount_returnsProviderIdOnly() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(Map.of("id", 999L))));

        OAuthUserInfo info = client.getUserInfo("token");

        assertThat(info.providerId()).isEqualTo("999");
        assertThat(info.email()).isNull();
        assertThat(info.nickname()).isNull();
    }

    @Test
    @DisplayName("profile 누락: nickname만 null, email은 정상")
    void getUserInfo_missingProfile_keepsEmail() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(Map.of(
                        "id", 1L,
                        "kakao_account", Map.of("email", "u@kakao.com")
                ))));

        OAuthUserInfo info = client.getUserInfo("token");

        assertThat(info.email()).isEqualTo("u@kakao.com");
        assertThat(info.nickname()).isNull();
    }

    @Test
    @DisplayName("id 누락: OAUTH_ERROR")
    void getUserInfo_missingId_throwsOAuthError() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"kakao_account\":{}}"));

        assertThatThrownBy(() -> client.getUserInfo("token"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("4xx 응답(만료/유효하지 않은 access token): OAUTH_ERROR")
    void getUserInfo_4xx_mapsToOAuthError() {
        server.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> client.getUserInfo("invalid-token"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("5xx 응답(카카오 서버 오류): OAUTH_ERROR")
    void getUserInfo_5xx_mapsToOAuthError() {
        server.enqueue(new MockResponse().setResponseCode(503));

        assertThatThrownBy(() -> client.getUserInfo("token"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }
}
