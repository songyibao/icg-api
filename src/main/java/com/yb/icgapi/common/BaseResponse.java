package com.yb.icgapi.common;

import com.yb.icgapi.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {
    private final int code;
    private final T data;
    private final String msg;
    public BaseResponse(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
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
