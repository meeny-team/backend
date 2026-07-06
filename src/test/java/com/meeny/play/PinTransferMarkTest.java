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
class PinTransferMarkTest {

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
                crewId, "transfer-mark-play", PlayType.HANGOUT,
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

    private long createPin(String token, long playId, long amount, Long paidBy, List<SplitDto> splits) throws Exception {
        CreatePinRequest request = new CreatePinRequest(
                playId, amount, PinCategory.FOOD, "pin", null, null, null, null, null,
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

    private String sentUrl(long playId, long pinId, long from, long to) {
        return "/api/plays/" + playId + "/pins/" + pinId + "/transfers/" + from + "/" + to + "/sent";
    }

    private String receivedUrl(long playId, long pinId, long from, long to) {
        return "/api/plays/" + playId + "/pins/" + pinId + "/transfers/" + from + "/" + to + "/received";
    }

    @Test
    @DisplayName("송신자가 sent 마킹 - 응답 pinTransfers 의 sentAt 채워짐")
    void markSent_success() throws Exception {
        Session a = login("g-tm-ms-a", "tmmsa@gmail.com", "A");
        Session b = login("g-tm-ms-b", "tmmsb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "마킹크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinTransfers.length()").value(1))
                .andExpect(jsonPath("$.data.pinTransfers[0].fromMemberId").value(b.memberId()))
                .andExpect(jsonPath("$.data.pinTransfers[0].toMemberId").value(a.memberId()))
                .andExpect(jsonPath("$.data.pinTransfers[0].sentAt").exists())
                .andExpect(jsonPath("$.data.pinTransfers[0].receivedAt").doesNotExist());
    }

    @Test
    @DisplayName("송신자가 아닌 사람이 sent 마킹 시도 - 403 TRANSFER_FORBIDDEN")
    void markSent_forbidden() throws Exception {
        Session a = login("g-tm-fb-a", "tmfba@gmail.com", "A");
        Session b = login("g-tm-fb-b", "tmfbb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "권한크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        // A 가 B 입장의 sent 마킹 시도 (송신자는 B 인데 A 가 누름)
        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRANSFER_FORBIDDEN"));
    }

    @Test
    @DisplayName("핀의 split 에 없는 송금 마킹 - 400 TRANSFER_INVALID_SPLIT")
    void markSent_invalidSplit() throws Exception {
        Session a = login("g-tm-iv-a", "tmiva@gmail.com", "A");
        Session b = login("g-tm-iv-b", "tmivb@gmail.com", "B");
        Session c = login("g-tm-iv-c", "tmivc@gmail.com", "C");
        CrewCtx crew = createCrew(a.token(), "검증크루");
        joinCrew(b.token(), crew.inviteCode());
        joinCrew(c.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId(), c.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        // C 가 자기 → A 송금 마킹 시도. 그런데 C 는 이 pin 의 split 에 없음.
        mockMvc.perform(post(sentUrl(playId, pinId, c.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + c.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSFER_INVALID_SPLIT"));
    }

    @Test
    @DisplayName("sent 마킹 두 번 호출해도 idempotent (상태 동일)")
    void markSent_idempotent() throws Exception {
        Session a = login("g-tm-id-a", "tmida@gmail.com", "A");
        Session b = login("g-tm-id-b", "tmidb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "멱등크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinTransfers[0].sentAt").exists())
                .andExpect(jsonPath("$.data.pinTransfers[0].receivedAt").doesNotExist());
    }

    @Test
    @DisplayName("수신자(paidBy) 가 received 마킹 - 응답 pinTransfers 의 receivedAt 채워짐")
    void markReceived_success() throws Exception {
        Session a = login("g-tm-mr-a", "tmmra@gmail.com", "A");
        Session b = login("g-tm-mr-b", "tmmrb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "수신크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        // B 가 보냈음 → A 가 받았음
        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinTransfers[0].sentAt").exists())
                .andExpect(jsonPath("$.data.pinTransfers[0].receivedAt").exists());
    }

    @Test
    @DisplayName("paidBy 가 아닌 사람이 received 마킹 시도 - 403 TRANSFER_FORBIDDEN")
    void markReceived_forbidden() throws Exception {
        Session a = login("g-tm-rf-a", "tmrfa@gmail.com", "A");
        Session b = login("g-tm-rf-b", "tmrfb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "수신권한크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        // B 가 자기가 받았다고 마킹 시도 (paidBy 는 A)
        mockMvc.perform(post(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRANSFER_FORBIDDEN"));
    }

    @Test
    @DisplayName("sent 마킹 없이 received 마킹 시도 - 404 TRANSFER_NOT_FOUND")
    void markReceived_notSent() throws Exception {
        Session a = login("g-tm-nr-a", "tmnra@gmail.com", "A");
        Session b = login("g-tm-nr-b", "tmnrb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "선결크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
    }

    @Test
    @DisplayName("sent 취소(DELETE) - 다시 pending 상태")
    void cancelSent_success() throws Exception {
        Session a = login("g-tm-cs-a", "tmcsa@gmail.com", "A");
        Session b = login("g-tm-cs-b", "tmcsb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "취소크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(delete(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinTransfers[0].sentAt").doesNotExist())
                .andExpect(jsonPath("$.data.pinTransfers[0].receivedAt").doesNotExist());
    }

    @Test
    @DisplayName("received 이후 sent 취소 시도 - 409 TRANSFER_ALREADY_RECEIVED")
    void cancelSent_afterReceived() throws Exception {
        Session a = login("g-tm-ca-a", "tmcaa@gmail.com", "A");
        Session b = login("g-tm-ca-b", "tmcab@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "완료보호크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk());
        mockMvc.perform(delete(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSFER_ALREADY_RECEIVED"));
    }

    @Test
    @DisplayName("모든 송금이 received 이면 정산 마감 성공")
    void close_succeedsAfterAllReceived() throws Exception {
        Session a = login("g-tm-cl-a", "tmcla@gmail.com", "A");
        Session b = login("g-tm-cl-b", "tmclb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "마감성공크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/plays/" + playId + "/settlement/close")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settledAt").exists());
    }

    @Test
    @DisplayName("일부 송금이 sent 까지만이고 received 안 됐으면 마감 차단 - 409 PLAY_NOT_SETTLEABLE")
    void close_blockedWhenSentButNotReceived() throws Exception {
        Session a = login("g-tm-cb-a", "tmcba@gmail.com", "A");
        Session b = login("g-tm-cb-b", "tmcbb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "마감차단크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        // sent 까지만, received 아직
        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/plays/" + playId + "/settlement/close")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAY_NOT_SETTLEABLE"));
    }

    @Test
    @DisplayName("받음 취소 - paidBy 가 누른 received 가 되돌려져서 receivedAt 사라지고 마감 차단됨")
    void cancelReceived_success() throws Exception {
        Session a = login("g-tm-cr-a", "tmcra@gmail.com", "A");
        Session b = login("g-tm-cr-b", "tmcrb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "수신취소크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk());
        // A 가 받음을 잘못 눌렀음을 되돌림
        mockMvc.perform(delete(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pinTransfers[0].sentAt").exists())
                .andExpect(jsonPath("$.data.pinTransfers[0].receivedAt").doesNotExist());
        // 결과적으로 받음이 안 된 상태이므로 마감 차단
        mockMvc.perform(post("/api/plays/" + playId + "/settlement/close")
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAY_NOT_SETTLEABLE"));
    }

    @Test
    @DisplayName("paidBy 가 아닌 사람이 received 취소 시도 - 403 TRANSFER_FORBIDDEN")
    void cancelReceived_forbidden() throws Exception {
        Session a = login("g-tm-cf-a", "tmcfa@gmail.com", "A");
        Session b = login("g-tm-cf-b", "tmcfb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "수신취소권한크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        mockMvc.perform(post(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isOk());
        // B(송신자) 가 자기가 받았다고 했던 걸 취소 시도 — 권한 없음
        mockMvc.perform(delete(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRANSFER_FORBIDDEN"));
    }

    @Test
    @DisplayName("받음 표시되지 않은 송금의 received 취소 시도 - 409 TRANSFER_NOT_RECEIVED")
    void cancelReceived_notReceived() throws Exception {
        Session a = login("g-tm-nr2-a", "tmnr2a@gmail.com", "A");
        Session b = login("g-tm-nr2-b", "tmnr2b@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "선결수신취소크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());
        // 받음 누른 적 없는데 취소 시도
        mockMvc.perform(delete(receivedUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + a.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_RECEIVED"));
    }

    @Test
    @DisplayName("작성자 강제 마감 - 미수신 송금이 있어도 마감되고 settledAt 채워짐")
    void forceClose_success() throws Exception {
        Session a = login("g-tm-fc-a", "tmfca@gmail.com", "A");
        Session b = login("g-tm-fc-b", "tmfcb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "강제마감크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        long pinId = createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        // sent 만 되어 있고 received 안 됨
        mockMvc.perform(post(sentUrl(playId, pinId, b.memberId(), a.memberId()))
                        .header("Authorization", "Bearer " + b.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/plays/" + playId + "/settlement/force-close")
                        .header("Authorization", "Bearer " + a.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"수신자가 응답 없음\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settledAt").exists());
    }

    @Test
    @DisplayName("작성자 아닌 사람이 강제 마감 시도 - 403 NOT_PLAY_OWNER")
    void forceClose_forbidden() throws Exception {
        Session a = login("g-tm-ff-a", "tmffa@gmail.com", "A");
        Session b = login("g-tm-ff-b", "tmffb@gmail.com", "B");
        CrewCtx crew = createCrew(a.token(), "강제마감권한크루");
        joinCrew(b.token(), crew.inviteCode());
        long playId = createPlay(a.token(), crew.crewId(), Set.of(b.memberId()));
        createPin(a.token(), playId, 10000L, a.memberId(), List.of(
                new SplitDto(a.memberId(), 5000L),
                new SplitDto(b.memberId(), 5000L)
        ));

        mockMvc.perform(post("/api/plays/" + playId + "/settlement/force-close")
                        .header("Authorization", "Bearer " + b.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_PLAY_OWNER"));
    }
}
