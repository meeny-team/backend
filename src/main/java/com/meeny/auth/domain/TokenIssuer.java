package com.meeny.auth.domain;

import com.meeny.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtProvider jwtProvider;

    public AuthTokens issue(Long memberId) {
        String accessToken = jwtProvider.generateToken(memberId);
        String refreshToken = UUID.randomUUID().toString();
        return new AuthTokens(accessToken, refreshToken);
    }
}
