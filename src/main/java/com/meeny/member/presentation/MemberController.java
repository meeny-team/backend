package com.meeny.member.presentation;

import com.meeny.global.response.ApiResponse;
import com.meeny.member.application.MemberService;
import com.meeny.member.application.dto.MemberProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
