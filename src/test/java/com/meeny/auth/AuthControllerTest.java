package com.meeny.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.auth.application.dto.LogoutRequest;
import com.meeny.auth.application.dto.RefreshRequest;
import com.meeny.auth.application.dto.SocialLoginRequest;
import com.meeny.auth.domain.OAuthClient;
import com.meeny.auth.domain.OAuthUserInfo;
import com.meeny.member.domain.SocialProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean(name = "googleOAuthClient")
    private OAuthClient googleOAuthClient;

    @MockitoBean(name = "kakaoOAuthClient")
    private OAuthClient kakaoOAuthClient;

    @MockitoBean(name = "appleOAuthClient")
    private OAuthClient appleOAuthClient;

    @Test
    @DisplayName("구글 소셜 로그인 성공 - accessToken, refreshToken 반환")
    void socialLogin_google_success() throws Exception {
        given(googleOAuthClient.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthClient.getUserInfo(anyString()))
                .willReturn(new OAuthUserInfo("google-uid-001", "user@gmail.com", "구글유저"));

        SocialLoginRequest request = new SocialLoginRequest(SocialProvider.GOOGLE, "valid-google-id-token", null);

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."));
    }

    @Test
    @DisplayName("카카오 소셜 로그인 성공 - accessToken, refreshToken 반환")
    void socialLogin_kakao_success() throws Exception {
        given(kakaoOAuthClient.provider()).willReturn(SocialProvider.KAKAO);
        given(kakaoOAuthClient.getUserInfo(anyString()))
                .willReturn(new OAuthUserInfo("kakao-uid-001", "user@kakao.com", "카카오유저"));

        SocialLoginRequest request = new SocialLoginRequest(SocialProvider.KAKAO, "valid-kakao-access-token", null);

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Apple 소셜 로그인 성공 - 이메일 없어도 가입 가능")
    void socialLogin_apple_noEmail_success() throws Exception {
        given(appleOAuthClient.provider()).willReturn(SocialProvider.APPLE);
        given(appleOAuthClient.getUserInfo(anyString()))
                .willReturn(new OAuthUserInfo("apple-uid-001", null, null));

        SocialLoginRequest request = new SocialLoginRequest(SocialProvider.APPLE, "valid-apple-identity-token", "애플유저");

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("같은 소셜 계정으로 재로그인 - 새 토큰 반환")
    void socialLogin_existingMember_returnsNewTokens() throws Exception {
        given(googleOAuthClient.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthClient.getUserInfo(anyString()))
                .willReturn(new OAuthUserInfo("google-uid-repeat", "repeat@gmail.com", "반복유저"));

        SocialLoginRequest request = new SocialLoginRequest(SocialProvider.GOOGLE, "token", null);

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("provider 누락 - 400 VALIDATION_ERROR")
    void socialLogin_missingProvider_validationFail() throws Exception {
        String body = "{\"token\":\"some-token\"}";

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 갱신 성공 - 새 accessToken, refreshToken 반환")
    void refresh_success() throws Exception {
        given(googleOAuthClient.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthClient.getUserInfo(anyString()))
                .willReturn(new OAuthUserInfo("google-uid-refresh", "refresh@gmail.com", "갱신유저"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("유효하지 않은 refreshToken으로 갱신 - 401 INVALID_TOKEN")
    void refresh_invalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("invalid-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("로그아웃 성공 - 204 No Content")
    void logout_success() throws Exception {
        given(googleOAuthClient.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthClient.getUserInfo(anyString()))
                .willReturn(new OAuthUserInfo("google-uid-logout", "logout@gmail.com", "로그아웃유저"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
                .andExpect(status().isNoContent());
    }
}
