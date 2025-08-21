package com.yb.icgapi.icpic.domain.user.constant;

public interface UserConstant {
    String USER_LOGIN_STATE = "userLoginState";

    // region 权限
    String DEFAULT_ROLE = "USER";
    String ADMIN_ROLE = "ADMIN";
    // endregion

    // region 默认值
    String DEFAULT_PASSWORD = "123456789";
    String SALT = "icg";
    // endregion
}
