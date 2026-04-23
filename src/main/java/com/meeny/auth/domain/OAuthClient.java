package com.meeny.auth.domain;

import com.meeny.member.domain.SocialProvider;

public interface OAuthClient {
    SocialProvider provider();
    OAuthUserInfo getUserInfo(String token);
}
