package com.meeny.places;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.domain.places.Place;
import com.meeny.domain.places.PlaceSearchClient;
import com.meeny.domain.places.PlaceSearchClient.Coordinate;
import com.meeny.infrastructure.oauth.OAuthClientRegistry;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlacesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OAuthClientRegistry oauthClientRegistry;

    // 외부 카카오 호출은 통합 테스트에서도 가짜로 두어 결정성 보장
    @MockitoBean
    private PlaceSearchClient placeSearchClient;

    private String login() throws Exception {
        given(oauthClientRegistry.getUserInfo(any(SocialProvider.class), anyString()))
                .willReturn(new OAuthUserInfo("provider-id", "user@meeny.com", "유저"));

        MvcResult result = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/accessToken").asText();
    }

    @Test
    @DisplayName("미인증 호출은 401")
    void searchWithoutAuthIsRejected() throws Exception {
        mockMvc.perform(get("/api/places/search").queryParam("query", "강남역"))
                .andExpect(status().isUnauthorized());

        then(placeSearchClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("좌표 없이 키워드만 → coordinate 는 null 로 위임되고 결과를 PlaceResponse 로 직렬화")
    void searchWithoutCoordinate() throws Exception {
        String token = login();
        given(placeSearchClient.searchByKeyword(anyString(), anyInt(), any()))
                .willReturn(List.of(new Place(
                        "1", "스타벅스 강남R점", "음식점 > 카페 > 커피전문점 > 스타벅스",
                        "서울 강남구 역삼동", "서울 강남구 강남대로 390",
                        "02-000-0000", 37.498, 127.027)));

        mockMvc.perform(get("/api/places/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("query", "강남역")
                        .queryParam("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].name").value("스타벅스 강남R점"))
                .andExpect(jsonPath("$.data[0].latitude").value(37.498));

        ArgumentCaptor<Coordinate> coordCaptor = ArgumentCaptor.forClass(Coordinate.class);
        then(placeSearchClient).should(times(1))
                .searchByKeyword(any(), anyInt(), coordCaptor.capture());
        // 좌표 파라미터가 모두 빠지면 coordinate 자체를 null 로 위임 — 전국 검색을 의도.
        org.junit.jupiter.api.Assertions.assertNull(coordCaptor.getValue());
    }

    @Test
    @DisplayName("위경도 + radius 가 모두 주어지면 Coordinate 를 그대로 위임")
    void searchWithCoordinate() throws Exception {
        String token = login();
        given(placeSearchClient.searchByKeyword(anyString(), anyInt(), any()))
                .willReturn(List.of());

        mockMvc.perform(get("/api/places/search")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("query", "카페")
                        .queryParam("latitude", "37.5")
                        .queryParam("longitude", "127.0")
                        .queryParam("radius", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<Coordinate> coordCaptor = ArgumentCaptor.forClass(Coordinate.class);
        then(placeSearchClient).should()
                .searchByKeyword(any(), anyInt(), coordCaptor.capture());
        Coordinate captured = coordCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(37.5, captured.latitude());
        org.junit.jupiter.api.Assertions.assertEquals(127.0, captured.longitude());
        org.junit.jupiter.api.Assertions.assertEquals(500, captured.radiusMeters());
    }
}
