package com.meeny.play;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.domain.activity.pin.PinCategory;
import com.meeny.domain.activity.pin.SettlementType;
import com.meeny.domain.activity.play.PlayType;
import com.meeny.infrastructure.oauth.OAuthClientRegistry;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.crew.dto.CreateCrewRequest;
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
class PlayStatsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private OAuthClientRegistry oauthClientRegistry;

    private String login(String providerId, String email, String nickname) throws Exception {
        given(oauthClientRegistry.getUserInfo(any(SocialProvider.class), anyString()))
                .willReturn(new OAuthUserInfo(providerId, email, nickname));
        MvcResult r = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    private long myId(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private long createCrew(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/crews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest("통계크루", null))))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private long createPlay(String token, long crewId) throws Exception {
        CreatePlayRequest req = new CreatePlayRequest(crewId, "통계플레이",
                PlayType.HANGOUT, new DateRangeDto(LocalDate.now(), null), Set.of(), null, null, null);
        return objectMapper.readTree(mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private void createPin(String token, long playId, long me, long amount, PinCategory cat) throws Exception {
        CreatePinRequest req = new CreatePinRequest(playId, amount, cat, cat.name(), null, null, null, null, null,
                new SettlementDto(SettlementType.EQUAL, me),
                List.of(new SplitDto(me, amount)));
        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("플레이 stats - 카테고리별 합계/개수/비율, 합계 큰 순")
    void playStats_aggregation() throws Exception {
        String token = login("g-ps-stats", "psstats@gmail.com", "통계유저");
        long me = myId(token);
        long crewId = createCrew(token);
        long playId = createPlay(token, crewId);
        createPin(token, playId, me, 8000L, PinCategory.FOOD);
        createPin(token, playId, me, 2000L, PinCategory.FOOD);
        createPin(token, playId, me, 5000L, PinCategory.TRANSPORT);
        createPin(token, playId, me, 5000L, PinCategory.CAFE);

        mockMvc.perform(get("/api/plays/" + playId + "/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(20000))
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.byCategory.length()").value(3))
                .andExpect(jsonPath("$.data.byCategory[0].category").value("FOOD"))
                .andExpect(jsonPath("$.data.byCategory[0].totalAmount").value(10000))
                .andExpect(jsonPath("$.data.byCategory[0].count").value(2))
                .andExpect(jsonPath("$.data.byCategory[0].percentage").value(50.0));
    }

    @Test
    @DisplayName("크루 stats - 여러 플레이의 핀을 합산")
    void crewStats_aggregation() throws Exception {
        String token = login("g-cs-stats", "csstats@gmail.com", "크루통계유저");
        long me = myId(token);
        long crewId = createCrew(token);
        long play1 = createPlay(token, crewId);
        long play2 = createPlay(token, crewId);
        createPin(token, play1, me, 10000L, PinCategory.FOOD);
        createPin(token, play2, me, 5000L, PinCategory.FOOD);
        createPin(token, play2, me, 3000L, PinCategory.STAY);

        mockMvc.perform(get("/api/crews/" + crewId + "/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(18000))
                .andExpect(jsonPath("$.data.byCategory[0].category").value("FOOD"))
                .andExpect(jsonPath("$.data.byCategory[0].totalAmount").value(15000));
    }

    @Test
    @DisplayName("비-멤버 stats 조회 - 403 NOT_CREW_MEMBER")
    void stats_notMember() throws Exception {
        String tokenOwner = login("g-stat-o", "stato@gmail.com", "오너");
        String tokenStranger = login("g-stat-s", "stats@gmail.com", "외부");
        long crewId = createCrew(tokenOwner);

        mockMvc.perform(get("/api/crews/" + crewId + "/stats")
                        .header("Authorization", "Bearer " + tokenStranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }
}
