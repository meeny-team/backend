package com.meeny.infrastructure.oauth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.common.exception.BusinessException;
import com.meeny.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class KakaoOAuthClient implements OAuthClient {

    private final WebClient webClient;

    public KakaoOAuthClient(
            WebClient.Builder builder,
            @Value("${oauth.kakao.profile-uri:https://kapi.kakao.com/v2/user/me}") String profileUri
    ) {
        this.webClient = builder.baseUrl(profileUri).build();
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

    // Kakao API 응답은 snake_case(`kakao_account`)이므로 명시적으로 매핑.
    // 기본 Spring Boot ObjectMapper는 camelCase만 매칭하므로, 어노테이션이 없으면 항상 null이 들어감.
    private record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {}

    private record KakaoAccount(String email, KakaoProfile profile) {}

    private record KakaoProfile(String nickname) {}
}
