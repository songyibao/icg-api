package com.yb.icgapi.model.enums;
import lombok.Getter;

/**
 * 空间类型枚举
 * 0 - 私有空间
 * 1 - 团队空间
 */
@Getter
public enum SpaceTypeEnum {
    PRIVATE("私有空间", 0),
    SHARE("团队空间", 1);

    private final String text;
    private final int value;

    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static SpaceTypeEnum fromValue(int value) {
        for (SpaceTypeEnum level : SpaceTypeEnum.values()) {
            if (level.value == value) {
                return level;
            }
        }
        return null;
    }

}
