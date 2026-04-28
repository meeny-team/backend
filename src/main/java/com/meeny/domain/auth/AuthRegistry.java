package com.meeny.domain.auth;

import com.meeny.domain.identity.SocialProvider;

public interface AuthRegistry {
    RefreshTokenRepository refreshTokenRepository();
    OAuthClient oauthClient(SocialProvider provider);
}
