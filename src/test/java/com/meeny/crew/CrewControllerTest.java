package com.meeny.crew;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.infrastructure.oauth.OAuthClientRegistry;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.crew.dto.CreateCrewRequest;
import com.meeny.presentation.crew.dto.JoinByCodeRequest;
import com.meeny.presentation.crew.dto.UpdateCrewRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CrewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OAuthClientRegistry oauthClientRegistry;

    private String loginAndGetToken(String providerId, String email, String nickname) throws Exception {
        given(oauthClientRegistry.getUserInfo(any(SocialProvider.class), anyString()))
                .willReturn(new OAuthUserInfo(providerId, email, nickname));

        MvcResult result = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    private String createCrewAndGetInviteCode(String accessToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/crews")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest(name, null))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/inviteCode").asText();
    }

    private long createCrewAndGetId(String accessToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/crews")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest(name, null))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    @Test
    @DisplayName("크루 생성 성공 - 생성자가 멤버에 포함, 6자리 inviteCode 발급")
    void createCrew_success() throws Exception {
        String token = loginAndGetToken("g-create", "create@gmail.com", "생성유저");

        mockMvc.perform(post("/api/crews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest("제주여행", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("제주여행"))
                .andExpect(jsonPath("$.data.inviteCode").isString())
                .andExpect(jsonPath("$.data.members.length()").value(1))
                .andExpect(jsonPath("$.data.createdBy").isNumber());
    }

    @Test
    @DisplayName("크루 이름 누락 시 400 VALIDATION_ERROR")
    void createCrew_blankName() throws Exception {
        String token = loginAndGetToken("g-blank", "blank@gmail.com", "유저");

        mockMvc.perform(post("/api/crews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest("", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("내 크루 목록 조회 - 내가 멤버인 크루만 반환")
    void getMyCrews_returnsOnlyMine() throws Exception {
        String tokenA = loginAndGetToken("g-a", "a@gmail.com", "A");
        String tokenB = loginAndGetToken("g-b", "b@gmail.com", "B");

        createCrewAndGetId(tokenA, "A의크루1");
        createCrewAndGetId(tokenA, "A의크루2");
        createCrewAndGetId(tokenB, "B의크루");

        mockMvc.perform(get("/api/crews").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("초대코드로 크루 참여 성공 - 멤버 수 증가")
    void joinByCode_success() throws Exception {
        String tokenOwner = loginAndGetToken("g-owner", "owner@gmail.com", "오너");
        String tokenGuest = loginAndGetToken("g-guest", "guest@gmail.com", "게스트");

        String inviteCode = createCrewAndGetInviteCode(tokenOwner, "초대크루");

        mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + tokenGuest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest(inviteCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members.length()").value(2));
    }

    @Test
    @DisplayName("이미 가입된 크루 재참여 - 400 ALREADY_JOINED_CREW")
    void joinByCode_alreadyJoined() throws Exception {
        String token = loginAndGetToken("g-aj", "aj@gmail.com", "유저");
        String inviteCode = createCrewAndGetInviteCode(token, "내크루");

        mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest(inviteCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALREADY_JOINED_CREW"));
    }

    @Test
    @DisplayName("잘못된 초대코드 - 400 INVALID_INVITE_CODE")
    void joinByCode_invalidCode() throws Exception {
        String token = loginAndGetToken("g-inv", "inv@gmail.com", "유저");

        mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest("ZZZZZZ"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INVITE_CODE"));
    }

    @Test
    @DisplayName("크루 상세 조회 - 멤버만 가능")
    void getDetail_memberOnly() throws Exception {
        String tokenOwner = loginAndGetToken("g-d-owner", "downer@gmail.com", "오너");
        String tokenStranger = loginAndGetToken("g-d-stranger", "ds@gmail.com", "외부");

        long crewId = createCrewAndGetId(tokenOwner, "비공개크루");

        mockMvc.perform(get("/api/crews/" + crewId).header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(crewId));

        mockMvc.perform(get("/api/crews/" + crewId).header("Authorization", "Bearer " + tokenStranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("크루 수정 - 생성자만 가능")
    void update_ownerOnly() throws Exception {
        String tokenOwner = loginAndGetToken("g-u-owner", "uowner@gmail.com", "오너");
        String tokenMember = loginAndGetToken("g-u-member", "umem@gmail.com", "멤버");

        String inviteCode = createCrewAndGetInviteCode(tokenOwner, "수정전이름");
        long crewId = objectMapper.readTree(mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + tokenMember)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest(inviteCode))))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        // 일반 멤버는 수정 불가
        mockMvc.perform(patch("/api/crews/" + crewId)
                        .header("Authorization", "Bearer " + tokenMember)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCrewRequest("새이름", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_OWNER"));

        // 생성자는 수정 가능
        mockMvc.perform(patch("/api/crews/" + crewId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCrewRequest("새이름", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새이름"));
    }

    @Test
    @DisplayName("크루 탈퇴 성공 - 멤버에서 제거")
    void leave_success() throws Exception {
        String tokenOwner = loginAndGetToken("g-l-owner", "lowner@gmail.com", "오너");
        String tokenGuest = loginAndGetToken("g-l-guest", "lguest@gmail.com", "게스트");

        String inviteCode = createCrewAndGetInviteCode(tokenOwner, "탈퇴테스트크루");
        long crewId = objectMapper.readTree(mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + tokenGuest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest(inviteCode))))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        mockMvc.perform(delete("/api/crews/" + crewId + "/me")
                        .header("Authorization", "Bearer " + tokenGuest))
                .andExpect(status().isNoContent());

        // 탈퇴 후에는 비멤버라 상세 조회 403
        mockMvc.perform(get("/api/crews/" + crewId)
                        .header("Authorization", "Bearer " + tokenGuest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비멤버 탈퇴 시도 - 403 NOT_CREW_MEMBER")
    void leave_notMember() throws Exception {
        String tokenOwner = loginAndGetToken("g-ln-owner", "lnowner@gmail.com", "오너");
        String tokenStranger = loginAndGetToken("g-ln-stranger", "lns@gmail.com", "외부");

        long crewId = createCrewAndGetId(tokenOwner, "외부테스트크루");

        mockMvc.perform(delete("/api/crews/" + crewId + "/me")
                        .header("Authorization", "Bearer " + tokenStranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("토큰 없이 크루 조회 - 401")
    void getMyCrews_noToken() throws Exception {
        mockMvc.perform(get("/api/crews"))
                .andExpect(status().isUnauthorized());
    }

    private long myId(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private String transferBody(long newOwnerId) throws Exception {
        return "{\"newOwnerId\":" + newOwnerId + "}";
    }

    @Test
    @DisplayName("소유권 양도 성공 - 새 owner 가 크루 수정 가능")
    void transferOwnership_success() throws Exception {
        String tokenOwner = loginAndGetToken("g-to-o", "too@gmail.com", "오너");
        String tokenNew = loginAndGetToken("g-to-n", "ton@gmail.com", "새오너");

        String inviteCode = createCrewAndGetInviteCode(tokenOwner, "양도크루");
        MvcResult joined = mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + tokenNew)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest(inviteCode))))
                .andExpect(status().isOk()).andReturn();
        long crewId = objectMapper.readTree(joined.getResponse().getContentAsString())
                .at("/data/id").asLong();
        long newOwnerId = myId(tokenNew);

        mockMvc.perform(post("/api/crews/" + crewId + "/owner")
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(newOwnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdBy").value(newOwnerId));

        // 양도 후엔 새 owner 가 크루 수정 가능
        mockMvc.perform(patch("/api/crews/" + crewId)
                        .header("Authorization", "Bearer " + tokenNew)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCrewRequest("양도후이름", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("양도후이름"));

        // 이전 owner 는 더 이상 수정 불가
        mockMvc.perform(patch("/api/crews/" + crewId)
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCrewRequest("거절", null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_OWNER"));
    }

    @Test
    @DisplayName("비-owner 가 양도 시도 - 403 NOT_CREW_OWNER")
    void transferOwnership_notOwner() throws Exception {
        String tokenOwner = loginAndGetToken("g-tn-o", "tno@gmail.com", "오너");
        String tokenMember = loginAndGetToken("g-tn-m", "tnm@gmail.com", "멤버");

        String inviteCode = createCrewAndGetInviteCode(tokenOwner, "비오너양도크루");
        long crewId = objectMapper.readTree(mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + tokenMember)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest(inviteCode))))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
        long ownerId = myId(tokenOwner);

        mockMvc.perform(post("/api/crews/" + crewId + "/owner")
                        .header("Authorization", "Bearer " + tokenMember)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(ownerId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_OWNER"));
    }

    @Test
    @DisplayName("크루 멤버 아닌 자에게 양도 시도 - 403 NOT_CREW_MEMBER")
    void transferOwnership_notMember() throws Exception {
        String tokenOwner = loginAndGetToken("g-tnm-o", "tnmo@gmail.com", "오너");
        String tokenOutsider = loginAndGetToken("g-tnm-x", "tnmx@gmail.com", "외부");

        long crewId = createCrewAndGetId(tokenOwner, "외부양도크루");
        long outsiderId = myId(tokenOutsider);

        mockMvc.perform(post("/api/crews/" + crewId + "/owner")
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(outsiderId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("자기 자신에게 양도 시도 - 400 CREW_OWNER_SAME_AS_CURRENT")
    void transferOwnership_self() throws Exception {
        String tokenOwner = loginAndGetToken("g-ts-o", "tso@gmail.com", "오너");
        long crewId = createCrewAndGetId(tokenOwner, "자기양도크루");
        long ownerId = myId(tokenOwner);

        mockMvc.perform(post("/api/crews/" + crewId + "/owner")
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(ownerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CREW_OWNER_SAME_AS_CURRENT"));
    }
}
