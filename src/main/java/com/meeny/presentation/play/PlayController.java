package com.meeny.presentation.play;

import com.meeny.application.play.PlayService;
import com.meeny.application.play.PlaySettlementService;
import com.meeny.application.play.PlayStatsService;
import com.meeny.common.response.ApiResponse;
import com.meeny.common.response.PageResponse;
import com.meeny.domain.activity.play.PlayType;
import com.meeny.presentation.play.dto.CategoryStatsResponse;
import com.meeny.presentation.play.dto.CreatePlayRequest;
import com.meeny.presentation.play.dto.PlayResponse;
import com.meeny.presentation.play.dto.PlaySettlementResponse;
import com.meeny.presentation.play.dto.UpdatePlayRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlayController {

    private final PlayService playService;
    private final PlaySettlementService playSettlementService;
    private final PlayStatsService playStatsService;

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

    // 검색 + 페이지네이션. type/keyword 모두 옵셔널. 기본 정렬은 createdAt desc.
    @GetMapping("/api/crews/{crewId}/plays/search")
    public ResponseEntity<ApiResponse<PageResponse<PlayResponse>>> search(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long crewId,
            @RequestParam(required = false) PlayType type,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                playService.searchPlays(crewId, memberId, type, keyword, pageable)));
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

    // 작성자 강제 마감: 미수신 송금이 남아 있어도 마감 (데드락 해소용). reason 은 감사 로그에 기록.
    @PostMapping("/api/plays/{playId}/settlement/force-close")
    public ResponseEntity<ApiResponse<PlaySettlementResponse>> forceCloseSettlement(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId,
            @RequestBody(required = false) ForceCloseSettlementRequest request) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(ApiResponse.ok(
                playSettlementService.forceClose(playId, memberId, reason),
                "정산이 강제로 마감되었습니다."));
    }

    public record ForceCloseSettlementRequest(String reason) {}

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

    @GetMapping("/api/plays/{playId}/stats")
    public ResponseEntity<ApiResponse<CategoryStatsResponse>> playStats(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId) {
        return ResponseEntity.ok(ApiResponse.ok(playStatsService.statsByPlay(playId, memberId)));
    }

    @GetMapping("/api/crews/{crewId}/stats")
    public ResponseEntity<ApiResponse<CategoryStatsResponse>> crewStats(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long crewId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(playStatsService.statsByCrew(crewId, memberId, from, to)));
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

    @DeleteMapping("/api/plays/{playId}/pins/{pinId}/transfers/{fromMemberId}/{toMemberId}/received")
    public ResponseEntity<ApiResponse<PlaySettlementResponse>> cancelTransferReceived(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId,
            @PathVariable Long pinId,
            @PathVariable Long fromMemberId,
            @PathVariable Long toMemberId) {
        PlaySettlementResponse res = playSettlementService.cancelTransferReceived(playId, pinId, fromMemberId, toMemberId, memberId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
