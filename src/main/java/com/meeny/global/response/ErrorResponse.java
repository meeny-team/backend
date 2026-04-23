package com.meeny.global.response;

import com.meeny.global.exception.ErrorCode;

public record ErrorResponse(boolean success, String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.name(), message);
    }
}
