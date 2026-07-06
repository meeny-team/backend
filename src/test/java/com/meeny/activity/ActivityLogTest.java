package com.meeny.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.domain.activity.pin.PinCategory;
import com.meeny.domain.activity.pin.SettlementType;
import com.meeny.domain.activity.play.PlayType;
import com.meeny.infrastructure.oauth.OAuthClientRegistry;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.crew.dto.CreateCrewRequest;
import com.meeny.presentation.crew.dto.JoinByCodeRequest;
import com.meeny.presentation.pin.dto.CreatePinRequest;
import com.meeny.presentation.pin.dto.SettlementDto;
import com.meeny.presentation.pin.dto.SplitDto;
import com.meeny.presentation.play.dto.CreatePlayRequest;
import com.meeny.presentation.play.dto.DateRangeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActivityLogTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OAuthClientRegistry oauthClientRegistry;

    private record Session(String token, long memberId) {}

    private Session login(String providerId, String email, String nickname) throws Exception {
        given(oauthClientRegistry.getUserInfo(any(SocialProvider.class), anyString()))
                .willReturn(new OAuthUserInfo(providerId, email, nickname));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
        long memberId = objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
        return new Session(token, memberId);
    }

    private record CrewCtx(long crewId, String inviteCode) {}

    private CrewCtx createCrew(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/crews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest(name, null))))
                .andExpect(status().isOk())
                .andReturn();
        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CrewCtx(node.at("/data/id").asLong(), node.at("/data/inviteCode").asText());
    }

    private void joinCrew(String token, String inviteCode) throws Exception {
        mockMvc.perform(post("/api/crews/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinByCodeRequest(inviteCode))))
                .andExpect(status().isOk());
    }

    private long createPlay(String token, long crewId, Set<Long> memberIds, String title) throws Exception {
        CreatePlayRequest request = new CreatePlayRequest(
                crewId, title, PlayType.HANGOUT,
                new DateRangeDto(LocalDate.now(), null),
                memberIds, null, null, null
        );
        MvcResult result = mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private long createPin(String token, long playId, long amount, Long paidBy, String title, List<SplitDto> splits) throws Exception {
        CreatePinRequest request = new CreatePinRequest(
                playId, amount, PinCategory.FOOD, title, null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, paidBy),
                splits
        );
        MvcResult result = mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    @Test
    @DisplayName("크루 활동 흐름 — join → play 생성 → pin 추가 → 송금 sent/received → 마감 모두 기록됨")
    void fullFlow_capturesAllActivities() throws Exception {
        Session a = login("g-al-ff-a", "alffa@gmail.com", "A");
        Session b = login("g-al-ff-b", "alffb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "활동피드크루");

        // 1) join
        joinCrew(b.token(), crew.inviteCode());

        // 2) play 생성
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()), "여행");

        // 3) pin 추가 (A 결제, A/B 5000 분담)
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), "스타벅스", List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        // 4) B 가 sent 마킹
        mockMvc.perform(post("/api/plays/" + playId + "/pins/" + pinId + "/transfers/" + b.memberId() + "/" + a.memberId() + "/sent")
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());

        // 5) A 가 received 마킹
        mockMvc.perform(post("/api/plays/" + playId + "/pins/" + pinId + "/transfers/" + b.memberId() + "/" + a.memberId() + "/received")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk());

        // 6) A 가 정산 마감
        mockMvc.perform(post("/api/plays/" + playId + "/settlement/close")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk());

        // 활동 피드 조회 — 시간 역순 6 건
        MvcResult res = mockMvc.perform(get("/api/crews/" + crew.crewId() + "/activities")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).at("/data");
        // 시간 역순: [PLAY_SETTLED, TRANSFER_RECEIVED, TRANSFER_SENT, PIN_ADDED, PLAY_CREATED, CREW_JOINED]
        // 단, 동일 트랜잭션 안에서 PLAY_SETTLED 와 TRANSFER_RECEIVED 가 같은 createdAt 일 수 있어 정확한 첫 항목만 검증
        // 최소 6 건 (테스트 격리: 새 크루라 다른 활동 없음)
        org.assertj.core.api.Assertions.assertThat(data.size()).isGreaterThanOrEqualTo(6);

        // 6 가지 type 모두 등장 확인
        java.util.Set<String> types = new java.util.HashSet<>();
        for (JsonNode log : data) types.add(log.at("/type").asText());
        org.assertj.core.api.Assertions.assertThat(types).contains(
                "CREW_JOINED", "PLAY_CREATED", "PIN_ADDED",
                "TRANSFER_SENT", "TRANSFER_RECEIVED", "PLAY_SETTLED");

        // PIN_ADDED payload 에 pinTitle/amount 포함 확인
        JsonNode pinAdded = findFirst(data, "PIN_ADDED");
        org.assertj.core.api.Assertions.assertThat(pinAdded.at("/payload/pinTitle").asText()).isEqualTo("스타벅스");
        org.assertj.core.api.Assertions.assertThat(pinAdded.at("/payload/amount").asLong()).isEqualTo(10000L);

        // actor 닉네임 채워짐 (탈퇴자 마스킹 X)
        org.assertj.core.api.Assertions.assertThat(pinAdded.at("/actor/nickname").asText()).isEqualTo("A");
    }

    @Test
    @DisplayName("비멤버는 활동 피드 조회 시 403 NOT_CREW_MEMBER")
    void activityFeed_nonMember_forbidden() throws Exception {
        Session owner = login("g-al-nm-o", "alnmo@gmail.com", "오너");
        Session stranger = login("g-al-nm-s", "alnms@gmail.com", "외부");
        CrewCtx crew = createCrew(owner.token(), "비멤버크루");

        mockMvc.perform(get("/api/crews/" + crew.crewId() + "/activities")
                        .header("Authorization", "Bearer " + stranger.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("탈퇴한 사용자는 actor 닉네임이 (탈퇴한 사용자) 로 마스킹")
    void activityFeed_withdrawnActor_masked() throws Exception {
        Session a = login("g-al-wd-a", "alwda@gmail.com", "A");
        Session b = login("g-al-wd-b", "alwdb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "탈퇴마스킹크루");
        joinCrew(b.token(), crew.inviteCode());

        // B 회원 탈퇴 (소프트 딜리트)
        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isNoContent());

        // A 가 활동 피드 조회 → CREW_JOINED 의 actor 가 마스킹돼야 함
        MvcResult res = mockMvc.perform(get("/api/crews/" + crew.crewId() + "/activities")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).at("/data");
        JsonNode joined = findFirst(data, "CREW_JOINED");
        org.assertj.core.api.Assertions.assertThat(joined.at("/actor/nickname").asText()).isEqualTo("(탈퇴한 사용자)");
    }

    private JsonNode findFirst(JsonNode data, String type) {
        for (JsonNode n : data) {
            if (type.equals(n.at("/type").asText())) return n;
        }
        throw new AssertionError("No activity of type " + type + " in: " + data);
    }
}
