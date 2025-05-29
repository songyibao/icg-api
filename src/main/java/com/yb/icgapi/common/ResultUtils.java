package com.yb.icgapi.common;

import com.yb.icgapi.exception.ErrorCode;

public class ResultUtils {
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(),data,ErrorCode.SUCCESS.getMessage());
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode.getCode(),null,errorCode.getMessage());
    }
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(),null,message);
    }
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code,null,message);
    }
}
