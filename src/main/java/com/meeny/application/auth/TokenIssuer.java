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

    // 액세스 토큰(JWT) + 리프레시 토큰(UUID) 쌍을 발급
    public AuthTokens issue(Long memberId) {
        String accessToken = jwtProvider.generateToken(memberId);
        String refreshToken = UUID.randomUUID().toString();
        return new AuthTokens(accessToken, refreshToken);
    }

    // 만료 시각이 설정된 RefreshToken 엔티티를 생성 (저장은 호출자 책임)
    public RefreshToken buildRefreshToken(Long memberId, String tokenValue) {
        return RefreshToken.create(memberId, tokenValue, jwtProperties.refreshTokenExpireMs());
    }
}
