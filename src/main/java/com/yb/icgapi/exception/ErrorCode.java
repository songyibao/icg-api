package com.yb.icgapi.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0,"OK"),
    NOT_LOGIN_ERROR(40000,"未登陆"),
    PARAMS_ERROR(40100,"请求参数错误"),
    SERVER_ERROR(50000,"内部异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
