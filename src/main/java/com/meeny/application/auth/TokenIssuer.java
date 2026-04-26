package com.meeny.application.auth;

import com.meeny.domain.auth.AuthTokens;
import com.meeny.domain.auth.RefreshToken;
import com.meeny.security.JwtProperties;
import com.meeny.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    public AuthTokens issue(Long memberId) {
        String accessToken = jwtProvider.generateToken(memberId);
        String refreshToken = UUID.randomUUID().toString();
        return new AuthTokens(accessToken, refreshToken);
    }

    public RefreshToken buildRefreshToken(Long memberId, String tokenValue) {
        return RefreshToken.create(memberId, tokenValue, jwtProperties.refreshTokenExpireMs());
    }
}
