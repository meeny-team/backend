package com.meeny.auth.infrastructure.oauth;

import com.meeny.auth.domain.OAuthClient;
import com.meeny.auth.domain.OAuthUserInfo;
import com.meeny.global.exception.BusinessException;
import com.meeny.global.exception.ErrorCode;
import com.meeny.member.domain.SocialProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class KakaoOAuthClient implements OAuthClient {

    private static final String PROFILE_URI = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient;

    public KakaoOAuthClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl(PROFILE_URI).build();
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        KakaoUserResponse response = webClient.get()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .onErrorMap(WebClientResponseException.class, e -> new BusinessException(ErrorCode.OAUTH_ERROR))
                .block();

        if (response == null || response.id() == null) {
            throw new BusinessException(ErrorCode.OAUTH_ERROR);
        }

        String email = response.kakaoAccount() != null ? response.kakaoAccount().email() : null;
        String nickname = response.kakaoAccount() != null && response.kakaoAccount().profile() != null
                ? response.kakaoAccount().profile().nickname()
                : null;

        return new OAuthUserInfo(String.valueOf(response.id()), email, nickname);
    }

    private record KakaoUserResponse(Long id, KakaoAccount kakaoAccount) {}

    private record KakaoAccount(String email, KakaoProfile profile) {}

    private record KakaoProfile(String nickname) {}
}
