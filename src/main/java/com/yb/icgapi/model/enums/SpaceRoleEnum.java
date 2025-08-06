package com.yb.icgapi.model.enums;

import cn.hutool.core.util.ObjUtil;
import com.yb.icgapi.constant.SpaceUserConstant;
import com.yb.icgapi.constant.UserConstant;
import com.yb.icgapi.model.entity.SpaceUser;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum SpaceRoleEnum {
    VIEWER("浏览者", SpaceUserConstant.VIEWER),
    EDITOR("编辑者", SpaceUserConstant.EDITOR),
    ADMIN("管理员", SpaceUserConstant.ADMIN),
    OWNER("所有者", SpaceUserConstant.OWNER);

    private final String name;
    private final String value;
    SpaceRoleEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if(ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceRoleEnum e : SpaceRoleEnum.values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }

    public static List<String> getAllTexts() {
        List<String> texts = new ArrayList<>();
        for (SpaceRoleEnum e : SpaceRoleEnum.values()) {
            texts.add(e.getName());
        }
        return texts;
    }

    public static List<String> getAllValues() {
        List<String> values = new ArrayList<>();
        for (SpaceRoleEnum e : SpaceRoleEnum.values()) {
            values.add(e.getValue());
        }
        return values;
    }
}
