package com.meeny.presentation.member;

import com.meeny.application.member.MemberService;
import com.meeny.presentation.member.dto.MemberProfileResponse;
import com.meeny.common.response.ApiResponse;
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
