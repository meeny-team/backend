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
class PinSearchTest {

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
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest("검색크루", null))))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private long createPlay(String token, long crewId) throws Exception {
        CreatePlayRequest req = new CreatePlayRequest(crewId, "여행1",
                PlayType.TRAVEL, new DateRangeDto(LocalDate.now(), null), Set.of(), null, null, null);
        return objectMapper.readTree(mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private void createPin(String token, long playId, long me, String title, PinCategory cat) throws Exception {
        CreatePinRequest req = new CreatePinRequest(playId, 10000L, cat, title, null, null, null,
                new SettlementDto(SettlementType.EQUAL, me),
                List.of(new SplitDto(me, 10000L)));
        mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("핀 search - category 필터 정확 매칭, page/size 응답 포맷 검증")
    void search_byCategory() throws Exception {
        String token = login("g-ps-cat", "pscat@gmail.com", "검색유저");
        long me = myId(token);
        long crewId = createCrew(token);
        long playId = createPlay(token, crewId);
        createPin(token, playId, me, "스시", PinCategory.FOOD);
        createPin(token, playId, me, "지하철", PinCategory.TRANSPORT);
        createPin(token, playId, me, "카페", PinCategory.CAFE);

        mockMvc.perform(get("/api/plays/" + playId + "/pins/search?category=FOOD")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("스시"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @DisplayName("핀 search - keyword 가 title/memo 모두에 LIKE 매칭")
    void search_byKeyword() throws Exception {
        String token = login("g-ps-kw", "pskw@gmail.com", "키워드유저");
        long me = myId(token);
        long crewId = createCrew(token);
        long playId = createPlay(token, crewId);
        createPin(token, playId, me, "스시", PinCategory.FOOD);
        createPin(token, playId, me, "햄버거", PinCategory.FOOD);
        createPin(token, playId, me, "카페라떼", PinCategory.CAFE);

        mockMvc.perform(get("/api/plays/" + playId + "/pins/search?keyword=라떼")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("카페라떼"));
    }

    @Test
    @DisplayName("핀 search - page=0 size=2 페이지네이션")
    void search_pagination() throws Exception {
        String token = login("g-ps-pg", "pspg@gmail.com", "페이지유저");
        long me = myId(token);
        long crewId = createCrew(token);
        long playId = createPlay(token, crewId);
        for (int i = 0; i < 5; i++) {
            createPin(token, playId, me, "pin-" + i, PinCategory.FOOD);
        }
        mockMvc.perform(get("/api/plays/" + playId + "/pins/search?page=0&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test
    @DisplayName("플레이 search - type 필터, keyword 매칭")
    void searchPlay_byTypeAndKeyword() throws Exception {
        String token = login("g-ps-pl", "pspl@gmail.com", "플레이유저");
        long crewId = createCrew(token);
        // play 3개 생성
        CreatePlayRequest p1 = new CreatePlayRequest(crewId, "제주여행",
                PlayType.TRAVEL, new DateRangeDto(LocalDate.now(), null), Set.of(), null, null, null);
        CreatePlayRequest p2 = new CreatePlayRequest(crewId, "동네산책",
                PlayType.HANGOUT, new DateRangeDto(LocalDate.now(), null), Set.of(), null, null, null);
        CreatePlayRequest p3 = new CreatePlayRequest(crewId, "부산여행",
                PlayType.TRAVEL, new DateRangeDto(LocalDate.now(), null), Set.of(), null, null, null);
        for (CreatePlayRequest req : List.of(p1, p2, p3)) {
            mockMvc.perform(post("/api/plays")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }
        // TRAVEL + keyword "여행" → 2 건
        mockMvc.perform(get("/api/crews/" + crewId + "/plays/search?type=TRAVEL&keyword=여행")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }
}
