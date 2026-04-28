package com.meeny.play;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthClient;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.member.SocialProvider;
import com.meeny.domain.play.PlayType;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.crew.dto.CreateCrewRequest;
import com.meeny.presentation.crew.dto.JoinByCodeRequest;
import com.meeny.presentation.play.dto.CreatePlayRequest;
import com.meeny.presentation.play.dto.DateRangeDto;
import com.meeny.presentation.play.dto.UpdatePlayRequest;
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
class PlayControllerTest {

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

        MvcResult result = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
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

    @Test
    @DisplayName("플레이 생성 성공 - 작성자가 멤버에 자동 포함")
    void createPlay_success() throws Exception {
        Session owner = login("g-p-create", "pcreate@gmail.com", "오너");
        CrewCtx crew = createCrew(owner.token(), "여행크루");

        CreatePlayRequest request = new CreatePlayRequest(
                crew.crewId(),
                "제주 여행",
                PlayType.TRAVEL,
                new DateRangeDto(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3)),
                Set.of(),
                List.of("제주 제주시"),
                List.of("힐링"),
                null
        );

        mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제주 여행"))
                .andExpect(jsonPath("$.data.type").value("TRAVEL"))
                .andExpect(jsonPath("$.data.crewId").value(crew.crewId()))
                .andExpect(jsonPath("$.data.memberIds.length()").value(1))
                .andExpect(jsonPath("$.data.regions[0]").value("제주 제주시"))
                .andExpect(jsonPath("$.data.tags[0]").value("힐링"));
    }

    @Test
    @DisplayName("플레이 생성 - crew 비멤버는 403 NOT_CREW_MEMBER")
    void createPlay_notCrewMember() throws Exception {
        Session owner = login("g-p-out-owner", "pout@gmail.com", "오너");
        Session outsider = login("g-p-out-stranger", "pouts@gmail.com", "외부");

        CrewCtx crew = createCrew(owner.token(), "비공개크루");

        CreatePlayRequest request = new CreatePlayRequest(
                crew.crewId(), "외부플레이", PlayType.HANGOUT,
                new DateRangeDto(LocalDate.now(), null),
                Set.of(), null, null, null
        );

        mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + outsider.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("플레이 생성 - members가 crew 멤버 부분집합 아니면 400 INVALID_PLAY_MEMBERS")
    void createPlay_invalidMembers() throws Exception {
        Session owner = login("g-p-im-owner", "pim@gmail.com", "오너");
        Session stranger = login("g-p-im-stranger", "pims@gmail.com", "외부");

        CrewCtx crew = createCrew(owner.token(), "크루");

        CreatePlayRequest request = new CreatePlayRequest(
                crew.crewId(), "잘못된 멤버 플레이", PlayType.HANGOUT,
                new DateRangeDto(LocalDate.now(), null),
                Set.of(stranger.memberId()),
                null, null, null
        );

        mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLAY_MEMBERS"));
    }

    @Test
    @DisplayName("플레이 생성 - end < start면 400 INVALID_DATE_RANGE")
    void createPlay_invalidDateRange() throws Exception {
        Session owner = login("g-p-dr", "pdr@gmail.com", "오너");
        CrewCtx crew = createCrew(owner.token(), "크루");

        CreatePlayRequest request = new CreatePlayRequest(
                crew.crewId(), "잘못된 날짜", PlayType.TRAVEL,
                new DateRangeDto(LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 1)),
                Set.of(), null, null, null
        );

        mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("크루 플레이 목록 조회 - crew 멤버만 가능")
    void getPlaysByCrew_memberOnly() throws Exception {
        Session owner = login("g-p-list-owner", "plist@gmail.com", "오너");
        Session stranger = login("g-p-list-stranger", "plists@gmail.com", "외부");
        CrewCtx crew = createCrew(owner.token(), "리스트크루");

        // 플레이 1개 생성
        CreatePlayRequest request = new CreatePlayRequest(
                crew.crewId(), "p1", PlayType.HANGOUT,
                new DateRangeDto(LocalDate.now(), null),
                Set.of(), null, null, null
        );
        mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 멤버는 조회 가능
        mockMvc.perform(get("/api/crews/" + crew.crewId() + "/plays")
                        .header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // 비멤버는 403
        mockMvc.perform(get("/api/crews/" + crew.crewId() + "/plays")
                        .header("Authorization", "Bearer " + stranger.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_CREW_MEMBER"));
    }

    @Test
    @DisplayName("플레이 수정 - 작성자만 가능")
    void updatePlay_authorOnly() throws Exception {
        Session author = login("g-p-up-author", "pupa@gmail.com", "작성자");
        Session member = login("g-p-up-member", "pupm@gmail.com", "멤버");

        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(member.token(), crew.inviteCode());

        // author가 play 생성, member도 멤버에 포함
        CreatePlayRequest createReq = new CreatePlayRequest(
                crew.crewId(), "원래제목", PlayType.HANGOUT,
                new DateRangeDto(LocalDate.now(), null),
                Set.of(member.memberId()), null, null, null
        );
        long playId = objectMapper.readTree(mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        UpdatePlayRequest updateReq = new UpdatePlayRequest(
                "새제목", null, null, null, null, null, null
        );

        // member는 수정 불가
        mockMvc.perform(patch("/api/plays/" + playId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_PLAY_OWNER"));

        // author는 수정 가능
        mockMvc.perform(patch("/api/plays/" + playId)
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새제목"));
    }

    @Test
    @DisplayName("플레이 삭제 - 작성자만 가능")
    void deletePlay_authorOnly() throws Exception {
        Session author = login("g-p-del-author", "pdela@gmail.com", "작성자");
        Session member = login("g-p-del-member", "pdelm@gmail.com", "멤버");

        CrewCtx crew = createCrew(author.token(), "크루");
        joinCrew(member.token(), crew.inviteCode());

        CreatePlayRequest createReq = new CreatePlayRequest(
                crew.crewId(), "삭제대상", PlayType.HANGOUT,
                new DateRangeDto(LocalDate.now(), null),
                Set.of(), null, null, null
        );
        long playId = objectMapper.readTree(mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + author.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();

        // member 삭제 시도 - 403
        mockMvc.perform(delete("/api/plays/" + playId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());

        // author는 삭제 가능
        mockMvc.perform(delete("/api/plays/" + playId)
                        .header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isNoContent());

        // 삭제 후 조회 404
        mockMvc.perform(get("/api/plays/" + playId)
                        .header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAY_NOT_FOUND"));
    }
}
