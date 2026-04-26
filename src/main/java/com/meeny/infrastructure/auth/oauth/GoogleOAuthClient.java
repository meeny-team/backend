package com.meeny.infrastructure.auth.oauth;

import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.member.SocialProvider;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class GoogleOAuthClient implements OAuthClient {

    private static final String TOKEN_INFO_URI = "https://oauth2.googleapis.com/tokeninfo";

    private final WebClient webClient;

    public GoogleOAuthClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(TOKEN_INFO_URI).build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String idToken) {
        GoogleTokenInfo info = webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParam("id_token", idToken).build())
                .retrieve()
                .bodyToMono(GoogleTokenInfo.class)
                .onErrorMap(WebClientResponseException.class, e -> new BusinessException(ErrorCode.OAUTH_ERROR))
                .block();

        if (info == null || info.sub() == null) {
            throw new BusinessException(ErrorCode.OAUTH_ERROR);
        }

        return new OAuthUserInfo(info.sub(), info.email(), info.name());
    }

    private record GoogleTokenInfo(String sub, String email, String name) {}
}
