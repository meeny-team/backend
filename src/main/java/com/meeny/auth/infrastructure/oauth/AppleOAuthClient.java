package com.meeny.auth.infrastructure.oauth;

import com.meeny.auth.domain.OAuthClient;
import com.meeny.auth.domain.OAuthUserInfo;
import com.meeny.global.exception.BusinessException;
import com.meeny.global.exception.ErrorCode;
import com.meeny.member.domain.SocialProvider;
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

@Component
public class AppleOAuthClient implements OAuthClient {

    private static final String APPLE_JWKS_URI = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final NimbusJwtDecoder decoder;

    public AppleOAuthClient(@Value("${oauth.apple.client-id}") String clientId) {
        this.decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_JWKS_URI).build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(APPLE_ISSUER);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_audience", "Apple 토큰의 audience가 올바르지 않습니다.", null)
            );
        };

        decoder.setJwtValidator(jwt -> {
            OAuth2TokenValidatorResult issuerResult = issuerValidator.validate(jwt);
            OAuth2TokenValidatorResult audienceResult = audienceValidator.validate(jwt);
            if (issuerResult.hasErrors() || audienceResult.hasErrors()) {
                return issuerResult.hasErrors() ? issuerResult : audienceResult;
            }
            return OAuth2TokenValidatorResult.success();
        });
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.APPLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String identityToken) {
        try {
            Jwt jwt = decoder.decode(identityToken);
            String sub = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            return new OAuthUserInfo(sub, email, null);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.OAUTH_ERROR);
        }
    }
}
