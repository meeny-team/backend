package com.meeny.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.member.SocialProvider;
import com.meeny.presentation.auth.dto.DevLoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DevAuthControllerTest {

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
    @DisplayName("dev 로그인 성공 - 신규 가입 + 토큰 발급")
    void devLogin_newMember() throws Exception {
        DevLoginRequest request = new DevLoginRequest(
                SocialProvider.GOOGLE, "dev-uid-001", "dev@meeny.com", "데브유저"
        );

        String body = mockMvc.perform(post("/api/auth/dev/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // 발급받은 토큰으로 본인 프로필 조회 가능
        String accessToken = objectMapper.readTree(body).at("/data/accessToken").asText();
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("dev@meeny.com"))
                .andExpect(jsonPath("$.data.nickname").value("데브유저"));
    }

    @Test
    @DisplayName("dev 로그인 - 같은 providerId 재호출 시 동일 회원의 새 토큰")
    void devLogin_existingMember() throws Exception {
        DevLoginRequest request = new DevLoginRequest(
                SocialProvider.GOOGLE, "dev-uid-repeat", "repeat@meeny.com", "반복"
        );

        String first = mockMvc.perform(post("/api/auth/dev/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long firstMemberId = memberIdOf(first);

        String second = mockMvc.perform(post("/api/auth/dev/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long secondMemberId = memberIdOf(second);

        org.junit.jupiter.api.Assertions.assertEquals(firstMemberId, secondMemberId);
    }

    private long memberIdOf(String loginResponseBody) throws Exception {
        String accessToken = objectMapper.readTree(loginResponseBody).at("/data/accessToken").asText();
        String me = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(me).at("/data/id").asLong();
    }
}
