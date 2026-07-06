package com.meeny.pin;

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
import com.meeny.presentation.pin.dto.UpdatePinRequest;
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
class PinControllerTest {

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

    private long createPlay(String token, long crewId, Set<Long> memberIds) throws Exception {
        CreatePlayRequest request = new CreatePlayRequest(
                crewId, "테스트플레이", PlayType.HANGOUT,
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

    @Test
    @DisplayName("핀 생성 성공 - 정산/분담 검증 통과")
    void createPin_success() throws Exception {
        Session author = login("g-pin-c-a", "pinca@gmail.com", "작성자");
        Session member = login("g-pin-c-m", "pincm@gmail.com", "멤버");
        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(member.token(), crew.inviteCode());
        long playId = createPlay(author.token(), crew.crewId(), Set.of(member.memberId()));

        CreatePinRequest request = new CreatePinRequest(
                playId,
                40000L,
                PinCategory.FOOD,
                "삼겹살",
                "맛있었음",
                "성수동",
                null,
                null,
                List.of("https://example.com/img1.jpg"),
                new SettlementDto(SettlementType.EQUAL, author.memberId()),
                List.of(
                        new SplitDto(author.memberId(), 20000L),
                        new SplitDto(member.memberId(), 20000L)
                )
        );

        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("삼겹살"))
                .andExpect(jsonPath("$.data.amount").value(40000))
                .andExpect(jsonPath("$.data.category").value("FOOD"))
                .andExpect(jsonPath("$.data.settlement.type").value("EQUAL"))
                .andExpect(jsonPath("$.data.splits.length()").value(2));
    }

    @Test
    @DisplayName("핀 생성 - play 비멤버는 403 NOT_PLAY_MEMBER")
    void createPin_notPlayMember() throws Exception {
        Session author = login("g-pin-np-a", "pinnpa@gmail.com", "작성자");
        Session outsider = login("g-pin-np-o", "pinnpo@gmail.com", "외부");
        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(outsider.token(), crew.inviteCode());
        // play는 author만 멤버
        long playId = createPlay(author.token(), crew.crewId(), Set.of());

        CreatePinRequest request = new CreatePinRequest(
                playId, 1000L, PinCategory.FOOD, "test", null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, outsider.memberId()),
                List.of(new SplitDto(outsider.memberId(), 1000L))
        );

        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + outsider.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_PLAY_MEMBER"));
    }

    @Test
    @DisplayName("핀 생성 - paidBy가 play 멤버 아니면 400 INVALID_PIN_PAYER")
    void createPin_invalidPayer() throws Exception {
        Session author = login("g-pin-pay-a", "pinpaya@gmail.com", "A");
        Session crewOnly = login("g-pin-pay-c", "pinpayc@gmail.com", "크루멤버");
        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(crewOnly.token(), crew.inviteCode());
        // play 멤버에 crewOnly 포함하지 않음
        long playId = createPlay(author.token(), crew.crewId(), Set.of());

        CreatePinRequest request = new CreatePinRequest(
                playId, 1000L, PinCategory.FOOD, "test", null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, crewOnly.memberId()),
                List.of(new SplitDto(author.memberId(), 1000L))
        );

        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PIN_PAYER"));
    }

    @Test
    @DisplayName("핀 생성 - splits.userId가 play 멤버 아니면 400 INVALID_PIN_SPLITS")
    void createPin_invalidSplits() throws Exception {
        Session author = login("g-pin-sp-a", "pinspa@gmail.com", "A");
        Session crewOnly = login("g-pin-sp-c", "pinspc@gmail.com", "크루멤버");
        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(crewOnly.token(), crew.inviteCode());
        long playId = createPlay(author.token(), crew.crewId(), Set.of());

        CreatePinRequest request = new CreatePinRequest(
                playId, 1000L, PinCategory.FOOD, "test", null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, author.memberId()),
                List.of(
                        new SplitDto(author.memberId(), 500L),
                        new SplitDto(crewOnly.memberId(), 500L)
                )
        );

        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PIN_SPLITS"));
    }

    @Test
    @DisplayName("핀 생성 - splits 합과 amount 불일치 시 400 INVALID_SPLIT_SUM")
    void createPin_splitSumMismatch() throws Exception {
        Session author = login("g-pin-sum-a", "pinsuma@gmail.com", "A");
        CrewCtx crew = createCrew(author.token(), "크루");
        long playId = createPlay(author.token(), crew.crewId(), Set.of());

        CreatePinRequest request = new CreatePinRequest(
                playId, 10000L, PinCategory.FOOD, "test", null, null, null, null, null,
                new SettlementDto(SettlementType.CUSTOM, author.memberId()),
                List.of(new SplitDto(author.memberId(), 9000L))
        );

        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SPLIT_SUM"));
    }

    @Test
    @DisplayName("플레이의 핀 목록 조회 - crew 멤버만 가능")
    void getPinsByPlay_crewMemberOnly() throws Exception {
        Session author = login("g-pin-l-a", "pinla@gmail.com", "A");
        Session crewMember = login("g-pin-l-c", "pinlc@gmail.com", "크루멤버");
        Session stranger = login("g-pin-l-s", "pinls@gmail.com", "외부");
        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(crewMember.token(), crew.inviteCode());
        long playId = createPlay(author.token(), crew.crewId(), Set.of());

        // 핀 1개 생성
        CreatePinRequest request = new CreatePinRequest(
                playId, 1000L, PinCategory.FOOD, "p", null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, author.memberId()),
                List.of(new SplitDto(author.memberId(), 1000L))
        );
        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // crew 멤버는 조회 가능 (play 멤버가 아니어도)
        mockMvc.perform(get("/api/plays/" + playId + "/pins")
                        .header("Authorization", "Bearer " + crewMember.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // 외부인은 차단
        mockMvc.perform(get("/api/plays/" + playId + "/pins")
                        .header("Authorization", "Bearer " + stranger.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("핀 수정 - 작성자만 가능")
    void updatePin_authorOnly() throws Exception {
        Session author = login("g-pin-u-a", "pinua@gmail.com", "A");
        Session member = login("g-pin-u-m", "pinum@gmail.com", "멤버");
        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(member.token(), crew.inviteCode());
        long playId = createPlay(author.token(), crew.crewId(), Set.of(member.memberId()));

        CreatePinRequest createReq = new CreatePinRequest(
                playId, 2000L, PinCategory.FOOD, "원래", null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, author.memberId()),
                List.of(
                        new SplitDto(author.memberId(), 1000L),
                        new SplitDto(member.memberId(), 1000L)
                )
        );
        long pinId = objectMapper.readTree(mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        UpdatePinRequest updateReq = new UpdatePinRequest(
                null, null, "수정된제목", null, null, null, null, null, null, null
        );

        mockMvc.perform(patch("/api/pins/" + pinId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_PIN_OWNER"));

        mockMvc.perform(patch("/api/pins/" + pinId)
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된제목"));
    }

    @Test
    @DisplayName("핀 삭제 - 작성자만 가능, 삭제 후 404")
    void deletePin_authorOnly() throws Exception {
        Session author = login("g-pin-d-a", "pinda@gmail.com", "A");
        CrewCtx crew = createCrew(author.token(), "크루");
        long playId = createPlay(author.token(), crew.crewId(), Set.of());

        CreatePinRequest createReq = new CreatePinRequest(
                playId, 1000L, PinCategory.FOOD, "delete-target", null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, author.memberId()),
                List.of(new SplitDto(author.memberId(), 1000L))
        );
        long pinId = objectMapper.readTree(mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        mockMvc.perform(delete("/api/pins/" + pinId)
                        .header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/pins/" + pinId)
                        .header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PIN_NOT_FOUND"));
    }
}
