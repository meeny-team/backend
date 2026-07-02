package com.meeny.presentation.member.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 20, message = "닉네임은 20자 이하여야 합니다.")
        String nickname,

        String profileImage,

        @Size(max = 100, message = "소개는 100자 이하여야 합니다.")
        String bio,

        @Size(max = 20, message = "은행 코드는 20자 이하여야 합니다.")
        String bankCode,

        // 숫자와 하이픈만 허용 (서버 저장 시 하이픈 제거). 빈 문자열은 등록 해제.
        @Pattern(regexp = "^$|^[0-9-]{1,30}$", message = "계좌번호는 숫자와 하이픈만 입력 가능합니다.")
        String accountNumber,

        @Size(max = 50, message = "예금주명은 50자 이하여야 합니다.")
        String accountHolderName
) {}
