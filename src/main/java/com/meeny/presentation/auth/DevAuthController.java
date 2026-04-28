package com.meeny.presentation.auth;

import com.meeny.application.auth.AuthService;
import com.meeny.common.response.ApiResponse;
import com.meeny.presentation.auth.dto.DevLoginRequest;
import com.meeny.presentation.auth.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/dev")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.dev-login.enabled", havingValue = "true")
public class DevAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> devLogin(@Valid @RequestBody DevLoginRequest request) {
        TokenResponse response = authService.devLogin(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "[DEV] 로그인되었습니다."));
    }
}
