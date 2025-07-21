package com.yb.icgapi.model.enums;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum SpaceLevelEnum {
    COMMON("普通版", 0, 100L, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000L, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000L, 10000L * 1024 * 1024);

    private final String text;
    private final int value;
    private final long maxCount;
    private final long maxSize;

    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    public static SpaceLevelEnum fromValue(int value) {
        for (SpaceLevelEnum level : SpaceLevelEnum.values()) {
            if (level.value == value) {
                return level;
            }
        }
        return null;
    }

}
