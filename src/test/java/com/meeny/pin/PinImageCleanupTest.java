package com.meeny.pin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meeny.domain.auth.OAuthUserInfo;
import com.meeny.domain.identity.SocialProvider;
import com.meeny.domain.activity.pin.PinCategory;
import com.meeny.domain.activity.pin.SettlementType;
import com.meeny.domain.activity.play.PlayType;
import com.meeny.infrastructure.aws.S3Storage;
import com.meeny.infrastructure.aws.S3UrlSigner;
import com.meeny.infrastructure.oauth.OAuthClientRegistry;
import com.meeny.presentation.auth.dto.SocialLoginRequest;
import com.meeny.presentation.crew.dto.CreateCrewRequest;
import com.meeny.presentation.pin.dto.CreatePinRequest;
import com.meeny.presentation.pin.dto.SettlementDto;
import com.meeny.presentation.pin.dto.SplitDto;
import com.meeny.presentation.pin.dto.UpdatePinRequest;
import com.meeny.presentation.play.dto.CreatePlayRequest;
import com.meeny.presentation.play.dto.DateRangeDto;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "aws.s3.bucket=test-bucket",
        "aws.s3.region=ap-northeast-2"
})
@AutoConfigureMockMvc
class PinImageCleanupTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OAuthClientRegistry oauthClientRegistry;
    @MockitoBean
    private S3Storage s3Storage;
    // CI 환경엔 AWS credential 이 없어 실제 S3Presigner 호출이 실패한다.
    // PinResponse.from 이 sign 을 호출하는 경로만 빠르게 우회 (입력을 그대로 반환).
    @MockitoBean
    private S3UrlSigner imageSigner;

    private String login(String providerId, String email, String nickname) throws Exception {
        given(imageSigner.sign(anyString())).willAnswer(inv -> inv.getArgument(0));
        given(oauthClientRegistry.getUserInfo(any(SocialProvider.class), anyString()))
                .willReturn(new OAuthUserInfo(providerId, email, nickname));
        MvcResult result = mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest(SocialProvider.GOOGLE, "token", null))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
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
                        .content(objectMapper.writeValueAsString(new CreateCrewRequest("이미지크루", null))))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private long createPlay(String token, long crewId) throws Exception {
        CreatePlayRequest request = new CreatePlayRequest(crewId, "이미지플레이",
                PlayType.HANGOUT, new DateRangeDto(LocalDate.now(), null), Set.of(), null, null, null);
        return objectMapper.readTree(mockMvc.perform(post("/api/plays")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private long createPinWithImages(String token, long playId, long memberId, List<String> images) throws Exception {
        CreatePinRequest request = new CreatePinRequest(playId, 10000L,
                PinCategory.FOOD, "이미지핀", null, null, images,
                new SettlementDto(SettlementType.EQUAL, memberId),
                List.of(new SplitDto(memberId, 10000L)));
        return objectMapper.readTree(mockMvc.perform(post("/api/pins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .at("/data/id").asLong();
    }

    private static String url(String key) {
        return "https://test-bucket.s3.ap-northeast-2.amazonaws.com/" + key;
    }

    @Test
    @DisplayName("핀 update 시 빠진 이미지의 S3 객체 키가 cleanup 으로 전달됨 (signed URL 도 raw 와 매칭)")
    void update_orphanImagesDeleted() throws Exception {
        given(s3Storage.keyOf(anyString())).willAnswer(inv -> {
            String url = inv.getArgument(0);
            String prefix = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/";
            if (url == null || !url.startsWith(prefix)) return null;
            String rest = url.substring(prefix.length());
            int q = rest.indexOf('?');
            return q < 0 ? rest : rest.substring(0, q);
        });

        String token = login("g-pi-up", "piup@gmail.com", "이미지유저");
        long memberId = myId(token);
        long crewId = createCrew(token);
        long playId = createPlay(token, crewId);
        long pinId = createPinWithImages(token, playId, memberId, List.of(url("a.jpg"), url("b.jpg"), url("c.jpg")));

        // b.jpg 만 빠뜨림. 클라이언트는 signed URL 형태로 보낼 수 있음 — query string 이 붙어 있어도 동일 키로 인식돼야 함.
        UpdatePinRequest update = new UpdatePinRequest(
                null, null, null, null, null,
                List.of(url("a.jpg") + "?X-Amz-Signature=abc", url("c.jpg")),
                null, null);
        mockMvc.perform(patch("/api/pins/" + pinId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Storage, atLeastOnce()).deleteQuietly(keyCaptor.capture());
        Set<String> deletedKeys = new HashSet<>(keyCaptor.getAllValues());
        assertThat(deletedKeys).contains("b.jpg");
        assertThat(deletedKeys).doesNotContain("a.jpg", "c.jpg");
    }

    @Test
    @DisplayName("핀 delete 시 모든 이미지 키가 cleanup 으로 전달됨")
    void delete_allImagesCleanedUp() throws Exception {
        given(s3Storage.keyOf(anyString())).willAnswer(inv -> {
            String url = inv.getArgument(0);
            String prefix = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/";
            return url != null && url.startsWith(prefix) ? url.substring(prefix.length()) : null;
        });

        String token = login("g-pi-del", "pidel@gmail.com", "이미지유저2");
        long memberId = myId(token);
        long crewId = createCrew(token);
        long playId = createPlay(token, crewId);
        long pinId = createPinWithImages(token, playId, memberId,
                List.of(url("x.jpg"), url("y.jpg")));

        mockMvc.perform(delete("/api/pins/" + pinId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Storage, atLeastOnce()).deleteQuietly(keyCaptor.capture());
        assertThat(new HashSet<>(keyCaptor.getAllValues())).containsExactlyInAnyOrder("x.jpg", "y.jpg");
    }
}
