package com.meeny.presentation.crew;

import com.meeny.application.activity.ActivityQueryService;
import com.meeny.application.crew.CrewService;
import com.meeny.common.response.ApiResponse;
import com.meeny.presentation.activity.dto.ActivityResponse;
import com.meeny.presentation.crew.dto.CreateCrewRequest;
import com.meeny.presentation.crew.dto.CrewResponse;
import com.meeny.presentation.crew.dto.JoinByCodeRequest;
import com.meeny.presentation.crew.dto.UpdateCrewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;
    private final ActivityQueryService activityQueryService;

    private static final int ACTIVITY_DEFAULT_LIMIT = 50;
    private static final int ACTIVITY_MAX_LIMIT = 200;

    @PostMapping
    public ResponseEntity<ApiResponse<CrewResponse>> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateCrewRequest request) {
        CrewResponse crew = crewService.create(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(crew, "크루가 생성되었습니다."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CrewResponse>>> getMyCrews(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok(crewService.getMyCrews(memberId)));
    }

    @GetMapping("/{crewId}")
    public ResponseEntity<ApiResponse<CrewResponse>> getDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long crewId) {
        return ResponseEntity.ok(ApiResponse.ok(crewService.getCrewDetail(crewId, memberId)));
    }

    @PatchMapping("/{crewId}")
    public ResponseEntity<ApiResponse<CrewResponse>> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long crewId,
            @Valid @RequestBody UpdateCrewRequest request) {
        CrewResponse crew = crewService.update(crewId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(crew, "크루가 수정되었습니다."));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<CrewResponse>> joinByCode(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody JoinByCodeRequest request) {
        CrewResponse crew = crewService.joinByCode(memberId, request.inviteCode());
        return ResponseEntity.ok(ApiResponse.ok(crew, "크루에 참여했습니다."));
    }

    @DeleteMapping("/{crewId}/me")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long crewId) {
        crewService.leave(crewId, memberId);
        return ResponseEntity.noContent().build();
    }

    // 크루 활동 피드 — 멤버만, 최신순 N 건 (default 50, max 200)
    @GetMapping("/{crewId}/activities")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivities(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long crewId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        int effective = limit == null ? ACTIVITY_DEFAULT_LIMIT : Math.max(1, Math.min(limit, ACTIVITY_MAX_LIMIT));
        return ResponseEntity.ok(ApiResponse.ok(activityQueryService.fetchByCrew(crewId, memberId, effective)));
    }
}
