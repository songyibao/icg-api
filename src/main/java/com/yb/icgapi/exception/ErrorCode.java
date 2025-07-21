package com.yb.icgapi.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0,"OK"),
    NOT_LOGIN_ERROR(40100,"未登陆"),
    PARAMS_ERROR(40102,"请求参数错误"),
    PARAM_BLANK(40101,"缺少请求参数"),
    PASSWORD_NOT_MATCH(40102,"两次密码不匹配"),
    PASSWORD_TOO_SHORT(40103,"密码过短"),
    PASSWORD_ERROR(40104,"密码错误"),
    USER_EXIST(40105,"账号已存在"),
    NO_AUTHORIZED(40300,"无权限操作"),
    NOT_FOUND(40400,"资源未找到"),
    OPERATION_ERROR(40500,"操作失败"),
    SERVER_ERROR(50000,"内部异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
