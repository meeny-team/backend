package com.meeny.domain.auth;

import com.meeny.domain.identity.SocialProvider;

public interface OAuthClient {
    SocialProvider provider();
    OAuthUserInfo getUserInfo(String token);
}
