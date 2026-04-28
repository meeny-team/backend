package com.meeny.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    OAUTH_ERROR(HttpStatus.UNAUTHORIZED, "소셜 로그인 인증에 실패했습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 크루입니다."),
    NOT_CREW_MEMBER(HttpStatus.FORBIDDEN, "크루 멤버만 접근할 수 있습니다."),
    NOT_CREW_OWNER(HttpStatus.FORBIDDEN, "크루 생성자만 수정할 수 있습니다."),
    ALREADY_JOINED_CREW(HttpStatus.BAD_REQUEST, "이미 참여 중인 크루입니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 코드입니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
