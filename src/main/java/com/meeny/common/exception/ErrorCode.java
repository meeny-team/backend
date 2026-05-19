package com.meeny.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "이미 사용된 리프레시 토큰입니다. 다시 로그인해주세요."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    OAUTH_ERROR(HttpStatus.UNAUTHORIZED, "소셜 로그인 인증에 실패했습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 크루입니다."),
    NOT_CREW_MEMBER(HttpStatus.FORBIDDEN, "크루 멤버만 접근할 수 있습니다."),
    NOT_CREW_OWNER(HttpStatus.FORBIDDEN, "크루 생성자만 수정할 수 있습니다."),
    ALREADY_JOINED_CREW(HttpStatus.BAD_REQUEST, "이미 참여 중인 크루입니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 초대 코드입니다."),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다."),
    PLAY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 플레이입니다."),
    NOT_PLAY_OWNER(HttpStatus.FORBIDDEN, "플레이 작성자만 수정/삭제할 수 있습니다."),
    NOT_PLAY_MEMBER(HttpStatus.FORBIDDEN, "플레이 멤버만 접근할 수 있습니다."),
    INVALID_PLAY_MEMBERS(HttpStatus.BAD_REQUEST, "플레이 멤버는 크루 멤버여야 합니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "종료일은 시작일 이후여야 합니다."),
    PIN_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 핀입니다."),
    NOT_PIN_OWNER(HttpStatus.FORBIDDEN, "핀 작성자만 수정/삭제할 수 있습니다."),
    INVALID_PIN_PAYER(HttpStatus.BAD_REQUEST, "결제자는 플레이 멤버여야 합니다."),
    INVALID_PIN_SPLITS(HttpStatus.BAD_REQUEST, "분담자는 플레이 멤버여야 하며 비어있을 수 없습니다."),
    INVALID_SPLIT_SUM(HttpStatus.BAD_REQUEST, "분담 금액의 합이 총 금액과 일치해야 합니다."),
    OUTSTANDING_SETTLEMENT_BALANCE(HttpStatus.CONFLICT, "미정산 잔액이 남아있어 진행할 수 없습니다. 정산을 먼저 마감해주세요."),
    PLAY_ALREADY_SETTLED(HttpStatus.CONFLICT, "정산이 마감된 플레이는 더 이상 변경할 수 없습니다."),
    PLAY_NOT_SETTLEABLE(HttpStatus.CONFLICT, "모든 송금이 받음 처리되어야 마감할 수 있습니다."),
    TRANSFER_INVALID_SPLIT(HttpStatus.BAD_REQUEST, "해당 핀에 그 송금 관계가 존재하지 않습니다."),
    TRANSFER_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 송금의 당사자만 변경할 수 있습니다."),
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "아직 보냈음으로 표시되지 않은 송금입니다."),
    TRANSFER_ALREADY_RECEIVED(HttpStatus.CONFLICT, "이미 받음 처리된 송금은 변경할 수 없습니다."),
    UPLOAD_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "업로드 스토리지가 구성되지 않았습니다."),
    UPLOAD_INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 형식입니다."),
    UPLOAD_INVALID_PURPOSE(HttpStatus.BAD_REQUEST, "지원하지 않는 업로드 용도입니다."),
    PLACES_SEARCH_FAILED(HttpStatus.BAD_GATEWAY, "장소 검색에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
