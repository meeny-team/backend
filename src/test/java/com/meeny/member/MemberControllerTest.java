package com.meeny.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.member.dto.UpdateProfileRequest;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.infrastructure.oauth.OAuthClientRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OAuthClientRegistry oauthClientRegistry;

    private String loginAndGetAccessToken(String providerId, String email, String nickname) throws Exception {
        given(oauthClientRegistry.getUserInfo(any(SocialProvider.class), anyString()))
                .willReturn(new OAuthUserInfo(providerId, email, nickname));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    @Test
    @DisplayName("내 프로필 조회 성공")
    void getProfile_success() throws Exception {
        String accessToken = loginAndGetAccessToken("google-uid-profile", "profile@gmail.com", "프로필유저");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profile@gmail.com"))
                .andExpect(jsonPath("$.data.nickname").value("프로필유저"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("토큰 없이 프로필 조회 - 401")
    void getProfile_noToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료/위조된 토큰으로 프로필 조회 - 401 INVALID_TOKEN")
    void getProfile_invalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("프로필 수정 성공 - nickname/bio/profileImage 변경")
    void updateProfile_success() throws Exception {
        String accessToken = loginAndGetAccessToken("google-uid-update", "update@gmail.com", "원래닉네임");

        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", "https://img.example/u.png", "안녕하세요", null, null, null);

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.data.profileImage").value("https://img.example/u.png"))
                .andExpect(jsonPath("$.data.bio").value("안녕하세요"));
    }

    @Test
    @DisplayName("프로필 수정 - 닉네임 20자 초과 시 400 VALIDATION_ERROR")
    void updateProfile_nicknameTooLong() throws Exception {
        String accessToken = loginAndGetAccessToken("google-uid-long", "long@gmail.com", "닉");

        String longNickname = "가".repeat(21);
        UpdateProfileRequest request = new UpdateProfileRequest(longNickname, null, null, null, null, null);

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 204, 이후 프로필 조회 시 404 MEMBER_NOT_FOUND")
    void withdraw_success() throws Exception {
        String accessToken = loginAndGetAccessToken("google-uid-withdraw", "withdraw@gmail.com", "탈퇴유저");

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("탈퇴 후 같은 소셜 계정으로 재로그인 - 같은 memberId로 재활성화")
    void withdraw_thenRelogin_reactivatesSameMember() throws Exception {
        String firstToken = loginAndGetAccessToken("google-uid-react", "react@gmail.com", "원래유저");
        long originalId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + firstToken))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isNoContent());

        // 같은 provider+providerId로 재로그인하면 새 계정이 아니라 같은 row가 부활해야 함
        String secondToken = loginAndGetAccessToken("google-uid-react", "react@gmail.com", "복귀유저");
        long reactivatedId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("복귀유저"))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        org.junit.jupiter.api.Assertions.assertEquals(originalId, reactivatedId);
    }
}
