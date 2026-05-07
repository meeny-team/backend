package com.meeny.presentation.upload;

import com.meeny.application.upload.UploadService;
import com.meeny.common.response.ApiResponse;
import com.meeny.presentation.upload.dto.PresignedUploadRequest;
import com.meeny.presentation.upload.dto.PresignedUploadResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    // presigned URL 발급: 클라이언트는 이 URL로 직접 S3에 PUT한 뒤 응답의 publicUrl 또는 objectKey를 후속 요청에 사용
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> createPresignedUpload(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PresignedUploadRequest request
    ) {
        UploadService.PresignedUpload upload = uploadService.createPresignedUpload(
                memberId, request.purpose(), request.contentType());
        return ResponseEntity.ok(ApiResponse.ok(PresignedUploadResponse.from(upload)));
    }
}
