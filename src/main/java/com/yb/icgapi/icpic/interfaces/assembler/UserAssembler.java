package com.yb.icgapi.icpic.interfaces.assembler;

import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.interfaces.dto.user.UserAddRequest;
import com.yb.icgapi.icpic.interfaces.dto.user.UserUpdateRequest;
import com.yb.icgapi.icpic.interfaces.vo.user.LoginUserVO;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import org.springframework.beans.BeanUtils;

public class UserAssembler {

    public static User toUserEntity(UserAddRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }
}
