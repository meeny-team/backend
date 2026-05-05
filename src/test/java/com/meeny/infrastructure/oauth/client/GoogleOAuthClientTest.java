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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleOAuthClientTest {

    private static final String VALID_ISSUER = "https://accounts.google.com";
    private static final String LEGACY_ISSUER = "accounts.google.com"; // 공식 문서 기준 둘 다 유효
    private static final String CLIENT_ID_WEB = "web-client-id.apps.googleusercontent.com";
    private static final String CLIENT_ID_IOS = "ios-client-id.apps.googleusercontent.com";

    private JwksTestServer jwks;

    @BeforeEach
    void setUp() throws Exception {
        jwks = new JwksTestServer();
    }

    @AfterEach
    void tearDown() throws Exception {
        jwks.shutdown();
    }

    private GoogleOAuthClient client(List<String> clientIds) {
        return new GoogleOAuthClient(clientIds, jwks.url());
    }

    private JWTClaimsSet.Builder validClaimsBuilder() {
        long now = System.currentTimeMillis();
        return new JWTClaimsSet.Builder()
                .issuer(VALID_ISSUER)
                .audience(CLIENT_ID_WEB)
                .subject("google-uid-12345")
                .claim("email", "u@gmail.com")
                .claim("name", "구글유저")
                .issueTime(new Date(now))
                .expirationTime(new Date(now + 60_000));
    }

    @Test
    @DisplayName("정상 토큰: sub/email/name이 OAuthUserInfo로 매핑")
    void getUserInfo_validToken_extractsClaims() throws Exception {
        String token = jwks.sign(validClaimsBuilder().build());

        OAuthUserInfo info = client(List.of(CLIENT_ID_WEB)).getUserInfo(token);

        assertThat(info.providerId()).isEqualTo("google-uid-12345");
        assertThat(info.email()).isEqualTo("u@gmail.com");
        assertThat(info.nickname()).isEqualTo("구글유저");
    }

    @Test
    @DisplayName("legacy issuer(accounts.google.com)도 허용")
    void getUserInfo_legacyIssuer_isAccepted() throws Exception {
        String token = jwks.sign(validClaimsBuilder().issuer(LEGACY_ISSUER).build());

        OAuthUserInfo info = client(List.of(CLIENT_ID_WEB)).getUserInfo(token);

        assertThat(info.providerId()).isEqualTo("google-uid-12345");
    }

    @Test
    @DisplayName("issuer가 Google이 아니면 OAUTH_ERROR")
    void getUserInfo_invalidIssuer_throws() throws Exception {
        String token = jwks.sign(validClaimsBuilder().issuer("https://malicious.example.com").build());

        assertThatThrownBy(() -> client(List.of(CLIENT_ID_WEB)).getUserInfo(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("audience가 허용 client_id 중 어느 것과도 매칭 안 되면 OAUTH_ERROR")
    void getUserInfo_audienceMismatch_throws() throws Exception {
        String token = jwks.sign(validClaimsBuilder().audience("other-client-id").build());

        assertThatThrownBy(() -> client(List.of(CLIENT_ID_WEB, CLIENT_ID_IOS)).getUserInfo(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("다중 audience(web/iOS) 중 하나라도 일치하면 통과")
    void getUserInfo_multipleAudiencesAllowed_oneMatchesPasses() throws Exception {
        String token = jwks.sign(validClaimsBuilder().audience(CLIENT_ID_IOS).build());

        OAuthUserInfo info = client(List.of(CLIENT_ID_WEB, CLIENT_ID_IOS)).getUserInfo(token);

        assertThat(info.providerId()).isEqualTo("google-uid-12345");
    }

    @Test
    @DisplayName("client-ids 미설정(빈 리스트)이면 모든 토큰을 거절")
    void getUserInfo_emptyAllowedAudiences_rejectsEverything() throws Exception {
        String token = jwks.sign(validClaimsBuilder().build());

        assertThatThrownBy(() -> client(List.of()).getUserInfo(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("만료된 토큰: defaults validator에서 OAUTH_ERROR")
    void getUserInfo_expiredToken_throws() throws Exception {
        long past = System.currentTimeMillis() - 120_000;
        String token = jwks.sign(validClaimsBuilder()
                .issueTime(new Date(past))
                .expirationTime(new Date(past + 60_000)) // 60초 전에 이미 만료
                .build());

        assertThatThrownBy(() -> client(List.of(CLIENT_ID_WEB)).getUserInfo(token))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }

    @Test
    @DisplayName("형식이 깨진 토큰 문자열: OAUTH_ERROR")
    void getUserInfo_malformedToken_throws() {
        assertThatThrownBy(() -> client(List.of(CLIENT_ID_WEB)).getUserInfo("not-a-jwt"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ERROR);
    }
}
