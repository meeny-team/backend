package com.meeny.auth.presentation;

import com.meeny.auth.application.AuthService;
import com.meeny.auth.application.dto.LogoutRequest;
import com.meeny.auth.application.dto.RefreshRequest;
import com.meeny.auth.application.dto.SocialLoginRequest;
import com.meeny.auth.application.dto.TokenResponse;
import com.meeny.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/social")
    public ResponseEntity<ApiResponse<TokenResponse>> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        TokenResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "로그인에 성공했습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response, "토큰이 갱신되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
