package com.yb.icgapi.icpic.infrastructure.common;

import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {
    private final int code;
    private final T data;
    private final String message;
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage());
    }
    public BaseResponse(ErrorCode errorCode, String msg) {
        this(errorCode.getCode(),null,msg);
    }
}
