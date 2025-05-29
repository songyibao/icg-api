package com.yb.icgapi.exception;

public class ThrowUtils {
    public static void ThrowIf(boolean condition, RuntimeException runtimeException) {
        if(condition){
            throw runtimeException;
        }
    }
    public static void ThrowIf(boolean condition,ErrorCode errorCode) {
        ThrowIf(condition, new BusinessException(errorCode));
    }

    public static void ThrowIf(boolean condition,ErrorCode errorCode, String message) {
        ThrowIf(condition, new BusinessException(errorCode, message));
    }
}
