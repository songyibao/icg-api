package com.yb.icgapi.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

}
