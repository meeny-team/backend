package com.meeny.infrastructure.oauth.client;

import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import com.meeny.domain.auth.OAuthUserInfo;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleOAuthClientTest {

    private static final String CLIENT_ID = "com.meeny.app";
    private static final String VALID_ISSUER = "https://appleid.apple.com";

    private JwksTestServer jwks;
    private AppleOAuthClient client;

    @BeforeEach
    void setUp() throws Exception {
        jwks = new JwksTestServer();
        client = new AppleOAuthClient(CLIENT_ID, jwks.url());
    }

    @AfterEach
    void tearDown() throws Exception {
        jwks.shutdown();
    }

    private JWTClaimsSet.Builder validClaimsBuilder() {
        long now = System.currentTimeMillis();
        return new JWTClaimsSet.Builder()
                .issuer(VALID_ISSUER)
                .audience(CLIENT_ID)
                .subject("apple-uid-99999")
                .claim("email", "u@privaterelay.appleid.com")
                .issueTime(new Date(now))
                .expirationTime(new Date(now + 60_000));
    }

    @Test
    @DisplayName("정상 토큰: sub/email 추출, nickname은 항상 null(애플 미제공)")
    void getUserInfo_validToken_returnsSubAndEmail() throws Exception {
        String token = jwks.sign(validClaimsBuilder().build());

        OAuthUserInfo info = client.getUserInfo(token);

        assertThat(info.providerId()).isEqualTo("apple-uid-99999");
        assertThat(info.email()).isEqualTo("u@privaterelay.appleid.com");
        assertThat(info.nickname()).isNull();
    }

    @Test
    @DisplayName("email 클레임 없음(첫 로그인 이후 시나리오): email=null로 정상 반환")
    void getUserInfo_noEmailClaim_returnsNullEmail() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(VALID_ISSUER)
                .audience(CLIENT_ID)
                .subject("apple-uid-no-email")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();
        String token = jwks.sign(claims);

        OAuthUserInfo info = client.getUserInfo(token);

        assertThat(info.providerId()).isEqualTo("apple-uid-no-email");
        assertThat(info.email()).isNull();
        assertThat(info.nickname()).isNull();
    }

    @Test
    @DisplayName("issuer가 Apple 아니면 OAUTH_ERROR")
    void getUserInfo_invalidIssuer_throws() throws Exception {
        String token = jwks.sign(validClaimsBuilder().issuer("https://accounts.google.com").build());

        assertThatThrownBy(() -> client.getUserInfo(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("audience가 설정된 client-id와 다르면 OAUTH_ERROR")
    void getUserInfo_audienceMismatch_throws() throws Exception {
        String token = jwks.sign(validClaimsBuilder().audience("com.other.app").build());

        assertThatThrownBy(() -> client.getUserInfo(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("만료된 토큰: OAUTH_ERROR")
    void getUserInfo_expiredToken_throws() throws Exception {
        long past = System.currentTimeMillis() - 120_000;
        String token = jwks.sign(validClaimsBuilder()
                .issueTime(new Date(past))
                .expirationTime(new Date(past + 60_000))
                .build());

        assertThatThrownBy(() -> client.getUserInfo(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("형식이 깨진 토큰: OAUTH_ERROR")
    void getUserInfo_malformedToken_throws() {
        assertThatThrownBy(() -> client.getUserInfo("not-a-jwt"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }
}
