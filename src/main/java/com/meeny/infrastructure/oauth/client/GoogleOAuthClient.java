package com.meeny.infrastructure.oauth.client;

import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GoogleOAuthClient implements OAuthClient {

    // Google ID 토큰의 iss는 두 가지 형태가 모두 유효하다고 공식 문서에 명시
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final NimbusJwtDecoder decoder;
    private final Set<String> allowedAudiences;

    // GOOGLE_CLIENT_IDS는 콤마 구분(웹/iOS/Android 각각의 client_id) — 빈 값이면 모든 토큰을 audience 검증에서 거절
    // jwks-uri는 yaml 디폴트로 Google 공식 엔드포인트, 테스트에선 MockWebServer URL로 교체
    public GoogleOAuthClient(
            @Value("${oauth.google.client-ids:}") List<String> clientIds,
            @Value("${oauth.google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwksUri
    ) {
        this.allowedAudiences = clientIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());

        this.decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();

        // jwt.getIssuer()는 URL 타입이라 "accounts.google.com"(scheme 없음, 공식 문서가 인정하는 형태)에서
        // URL 파싱이 실패함. iss 클레임을 String으로 읽어 두 형태 모두 안전하게 비교.
        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            if (iss != null && GOOGLE_ISSUERS.contains(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", "Google ID 토큰의 issuer가 올바르지 않습니다.", null)
            );
        };

        // 다중 audience 허용: Google은 플랫폼별로 client_id가 다르므로(web/iOS/Android), 토큰의 aud가 허용 리스트 중 하나라도 일치하면 통과
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.stream().anyMatch(allowedAudiences::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_audience", "Google ID 토큰의 audience가 허용된 client_id가 아닙니다.", null)
            );
        };

        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefault();
        decoder.setJwtValidator(jwt -> {
            OAuth2TokenValidatorResult def = defaults.validate(jwt);
            if (def.hasErrors()) return def;
            OAuth2TokenValidatorResult iss = issuerValidator.validate(jwt);
            if (iss.hasErrors()) return iss;
            return audienceValidator.validate(jwt);
        });
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    // tokeninfo 엔드포인트(Google 권장 X) 대신 JWKS로 로컬 검증; 서명·만료·issuer·audience 모두 검증
    @Override
    public OAuthUserInfo getUserInfo(String idToken) {
        try {
            Jwt jwt = decoder.decode(idToken);
            return new OAuthUserInfo(
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("name")
            );
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.OAUTH_ERROR);
        }
    }
}
