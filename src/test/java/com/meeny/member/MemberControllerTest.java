package com.meeny.member;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerTest {

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
    @DisplayName("내 프로필 조회 성공")
    void getProfile_success() throws Exception {
        given(googleOAuthClient.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthClient.getUserInfo(anyString()))
                .willReturn(new OAuthUserInfo("google-uid-profile", "profile@gmail.com", "프로필유저"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/accessToken").asText();

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profile@gmail.com"))
                .andExpect(jsonPath("$.data.nickname").value("프로필유저"))
                .andExpect(jsonPath("$.data.memberId").isNumber());
    }

    @Test
    @DisplayName("토큰 없이 프로필 조회 - 401")
    void getProfile_noToken() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료/위조된 토큰으로 프로필 조회 - 401 INVALID_TOKEN")
    void getProfile_invalidToken() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }
}
