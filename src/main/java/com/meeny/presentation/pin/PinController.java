package com.meeny.presentation.pin;

import com.meeny.application.pin.PinService;
import com.meeny.common.response.ApiResponse;
import com.meeny.presentation.pin.dto.CreatePinRequest;
import com.meeny.presentation.pin.dto.PinResponse;
import com.meeny.presentation.pin.dto.UpdatePinRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PinController {

    private final PinService pinService;

    @PostMapping("/api/pins")
    public ResponseEntity<ApiResponse<PinResponse>> create(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreatePinRequest request) {
        PinResponse pin = pinService.create(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(pin, "핀이 생성되었습니다."));
    }

    @GetMapping("/api/plays/{playId}/pins")
    public ResponseEntity<ApiResponse<List<PinResponse>>> getByPlay(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long playId) {
        return ResponseEntity.ok(ApiResponse.ok(pinService.getPinsByPlay(playId, memberId)));
    }

    @GetMapping("/api/pins/{pinId}")
    public ResponseEntity<ApiResponse<PinResponse>> getDetail(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long pinId) {
        return ResponseEntity.ok(ApiResponse.ok(pinService.getPin(pinId, memberId)));
    }

    @PatchMapping("/api/pins/{pinId}")
    public ResponseEntity<ApiResponse<PinResponse>> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long pinId,
            @Valid @RequestBody UpdatePinRequest request) {
        PinResponse pin = pinService.update(pinId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(pin, "핀이 수정되었습니다."));
    }

    @DeleteMapping("/api/pins/{pinId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long pinId) {
        pinService.delete(pinId, memberId);
        return ResponseEntity.noContent().build();
    }
}
