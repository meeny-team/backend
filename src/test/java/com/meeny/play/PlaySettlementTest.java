package com.meeny.play;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.member.SocialProvider;
import com.meeny.domain.pin.PinCategory;
import com.meeny.domain.pin.SettlementType;
import com.meeny.domain.play.PlayType;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PlaySettlementTest {

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

    private record Session(String token, long memberId) {}

    private Session login(String providerId, String email, String nickname) throws Exception {
        given(googleOAuthClient.provider()).willReturn(SocialProvider.GOOGLE);
        given(googleOAuthClient.getUserInfo(anyString()))
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
                crewId, "settlement-play", PlayType.HANGOUT,
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

    private void createPin(String token, long playId, long amount, Long paidBy, List<SplitDto> splits) throws Exception {
        CreatePinRequest request = new CreatePinRequest(
                playId, amount, PinCategory.FOOD, "pin", null, null, null,
                new SettlementDto(SettlementType.EQUAL, paidBy),
                splits
        );
        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("핀 0개 - totalAmount 0, 모든 balance 0, transfers 비어있음")
    void settlement_emptyPins() throws Exception {
        Session owner = login("g-st-empty", "stem@gmail.com", "오너");
        CrewCtx crew = createCrew(owner.token(), "크루");
        long playId = createPlay(owner.token(), crew.crewId(), Set.of());

        mockMvc.perform(get("/api/plays/" + playId + "/settlement")
                        .header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(0))
                .andExpect(jsonPath("$.data.memberBalances.length()").value(1))
                .andExpect(jsonPath("$.data.memberBalances[0].balance").value(0))
                .andExpect(jsonPath("$.data.transfers.length()").value(0));
    }

    @Test
    @DisplayName("균등 분담 - A 결제 30000, A/B/C 균등 → B,C가 A에게 10000씩")
    void settlement_singlePayerEqualSplit() throws Exception {
        Session a = login("g-st-eq-a", "stea@gmail.com", "A");
        Session b = login("g-st-eq-b", "steb@gmail.com", "B");
        Session c = login("g-st-eq-c", "stec@gmail.com", "C");
        CrewCtx crew = createCrew(a.token(), "크루");
        joinCrew(b.token(), crew.inviteCode());
        joinCrew(c.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId(), c.memberId()));

        createPin(a.token(), playId, 30000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 10000L),
                new SplitDto(b.memberId(), 10000L),
                new SplitDto(c.memberId(), 10000L)
        ));

        mockMvc.perform(get("/api/plays/" + playId + "/settlement")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(30000))
                .andExpect(jsonPath("$.data.memberBalances.length()").value(3))
                .andExpect(jsonPath("$.data.transfers.length()").value(2))
                .andExpect(jsonPath("$.data.transfers[0].toMemberId").value(a.memberId()))
                .andExpect(jsonPath("$.data.transfers[1].toMemberId").value(a.memberId()))
                .andExpect(jsonPath("$.data.transfers[0].amount").value(10000))
                .andExpect(jsonPath("$.data.transfers[1].amount").value(10000));
    }

    @Test
    @DisplayName("교차 결제 - A,B 각각 결제, 균등 분담 시 transfer 결과")
    void settlement_crossPayments() throws Exception {
        Session a = login("g-st-cr-a", "stcra@gmail.com", "A");
        Session b = login("g-st-cr-b", "stcrb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));

        // A 40000 결제, 균등 (각 20000)
        createPin(a.token(), playId, 40000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 20000L),
                new SplitDto(b.memberId(), 20000L)
        ));
        // B 10000 결제, 균등 (각 5000)
        createPin(b.token(), playId, 10000L, b.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        // A: paid 40000, share 25000, balance +15000
        // B: paid 10000, share 25000, balance -15000
        // → B가 A에게 15000 송금
        mockMvc.perform(get("/api/plays/" + playId + "/settlement")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(50000))
                .andExpect(jsonPath("$.data.transfers.length()").value(1))
                .andExpect(jsonPath("$.data.transfers[0].fromMemberId").value(b.memberId()))
                .andExpect(jsonPath("$.data.transfers[0].toMemberId").value(a.memberId()))
                .andExpect(jsonPath("$.data.transfers[0].amount").value(15000));
    }

    @Test
    @DisplayName("custom 분담 - A 결제 10000, A/B 6000/4000 분담")
    void settlement_customSplit() throws Exception {
        Session a = login("g-st-cs-a", "stcsa@gmail.com", "A");
        Session b = login("g-st-cs-b", "stcsb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));

        createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 6000L),
                new SplitDto(b.memberId(), 4000L)
        ));

        // A: paid 10000, share 6000, balance +4000
        // B: paid 0, share 4000, balance -4000
        mockMvc.perform(get("/api/plays/" + playId + "/settlement")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transfers.length()").value(1))
                .andExpect(jsonPath("$.data.transfers[0].amount").value(4000))
                .andExpect(jsonPath("$.data.transfers[0].fromMemberId").value(b.memberId()))
                .andExpect(jsonPath("$.data.transfers[0].toMemberId").value(a.memberId()));
    }

    @Test
    @DisplayName("crew 비멤버 정산 조회 - 403 NOT_CREW_MEMBER")
    void settlement_notCrewMember() throws Exception {
        Session owner = login("g-st-nc-o", "stnco@gmail.com", "오너");
        Session stranger = login("g-st-nc-s", "stncs@gmail.com", "외부");
        CrewCtx crew = createCrew(owner.token(), "크루");
        long playId = createPlay(owner.token(), crew.crewId(), Set.of());

        mockMvc.perform(get("/api/plays/" + playId + "/settlement")
                        .header("Authorization", "Bearer " + stranger.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("존재하지 않는 플레이 정산 조회 - 404 PLAY_NOT_FOUND")
    void settlement_playNotFound() throws Exception {
        Session owner = login("g-st-nf", "stnf@gmail.com", "오너");
        mockMvc.perform(get("/api/plays/999999/settlement")
                        .header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAY_NOT_FOUND"));
    }

}
