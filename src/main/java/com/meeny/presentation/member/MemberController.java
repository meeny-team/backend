package com.meeny.presentation.member;

import com.meeny.application.member.MemberService;
import com.meeny.presentation.member.dto.MemberProfileResponse;
import com.meeny.presentation.member.dto.UpdateProfileRequest;
import com.meeny.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getProfile(
            @AuthenticationPrincipal Long memberId) {
        MemberProfileResponse profile = memberService.getProfile(memberId);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> updateProfile(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateProfileRequest request) {
        MemberProfileResponse profile = memberService.updateProfile(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok(profile, "프로필이 수정되었습니다."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId) {
        memberService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }
}
