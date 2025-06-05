package com.yb.icgapi.model.enums;

import cn.hutool.core.util.ObjUtil;
import com.yb.icgapi.constant.UserConstant;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    USER("用户",UserConstant.DEFAULT_ROLE),
    ADMIN("管理员", UserConstant.ADMIN_ROLE);

    private String name;
    private String value;
    UserRoleEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据值获取枚举
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if(ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum e : UserRoleEnum.values()) {
            if (e.value.equals(value)) {
                return e;
            }
        }
        return null;
    }
}
