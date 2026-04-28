package com.meeny.presentation.crew;

import com.meeny.application.crew.CrewService;
import com.meeny.common.response.ApiResponse;
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
}
