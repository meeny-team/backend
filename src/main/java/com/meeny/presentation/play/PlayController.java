package com.meeny.presentation.play;

import com.meeny.application.play.PlayService;
import com.meeny.application.play.PlaySettlementService;
import com.meeny.common.response.ApiResponse;
import com.meeny.presentation.play.dto.CreatePlayRequest;
import com.meeny.presentation.play.dto.PlayResponse;
import com.meeny.presentation.play.dto.PlaySettlementResponse;
import com.meeny.presentation.play.dto.UpdatePlayRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlayController {

    private final PlayService playService;
    private final PlaySettlementService playSettlementService;

    @PostMapping("/api/plays")
    public ResponseEntity<ApiResponse<PlayResponse>> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreatePlayRequest request) {
        PlayResponse play = playService.create(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(play, "플레이가 생성되었습니다."));
    }

    @GetMapping("/api/crews/{crewId}/plays")
    public ResponseEntity<ApiResponse<List<PlayResponse>>> getByCrew(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long crewId) {
        return ResponseEntity.ok(ApiResponse.ok(playService.getPlaysByCrew(crewId, memberId)));
    }

    @GetMapping("/api/plays/{playId}")
    public ResponseEntity<ApiResponse<PlayResponse>> getDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId) {
        return ResponseEntity.ok(ApiResponse.ok(playService.getPlay(playId, memberId)));
    }

    @PatchMapping("/api/plays/{playId}")
    public ResponseEntity<ApiResponse<PlayResponse>> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId,
            @Valid @RequestBody UpdatePlayRequest request) {
        PlayResponse play = playService.update(playId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(play, "플레이가 수정되었습니다."));
    }

    @DeleteMapping("/api/plays/{playId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId) {
        playService.delete(playId, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/plays/{playId}/settlement")
    public ResponseEntity<ApiResponse<PlaySettlementResponse>> getSettlement(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId) {
        return ResponseEntity.ok(ApiResponse.ok(playSettlementService.calculate(playId, memberId)));
    }

    @PostMapping("/api/plays/{playId}/settlement/close")
    public ResponseEntity<ApiResponse<PlaySettlementResponse>> closeSettlement(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId) {
        return ResponseEntity.ok(ApiResponse.ok(playSettlementService.close(playId, memberId), "정산이 마감되었습니다."));
    }

    // 핀 단위 송금 마킹: from = 송신자, to = 결제자(paidBy)
    @PostMapping("/api/plays/{playId}/pins/{pinId}/transfers/{fromMemberId}/{toMemberId}/sent")
    public ResponseEntity<ApiResponse<PlaySettlementResponse>> markTransferSent(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId,
            @PathVariable Long pinId,
            @PathVariable Long fromMemberId,
            @PathVariable Long toMemberId) {
        PlaySettlementResponse res = playSettlementService.markTransferSent(playId, pinId, fromMemberId, toMemberId, memberId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @DeleteMapping("/api/plays/{playId}/pins/{pinId}/transfers/{fromMemberId}/{toMemberId}/sent")
    public ResponseEntity<ApiResponse<PlaySettlementResponse>> cancelTransferSent(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId,
            @PathVariable Long pinId,
            @PathVariable Long fromMemberId,
            @PathVariable Long toMemberId) {
        PlaySettlementResponse res = playSettlementService.cancelTransferSent(playId, pinId, fromMemberId, toMemberId, memberId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/api/plays/{playId}/pins/{pinId}/transfers/{fromMemberId}/{toMemberId}/received")
    public ResponseEntity<ApiResponse<PlaySettlementResponse>> markTransferReceived(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId,
            @PathVariable Long pinId,
            @PathVariable Long fromMemberId,
            @PathVariable Long toMemberId) {
        PlaySettlementResponse res = playSettlementService.markTransferReceived(playId, pinId, fromMemberId, toMemberId, memberId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
