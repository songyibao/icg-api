package com.yb.icgapi.service;

import com.yb.icgapi.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.entity.UserVO;
import org.springframework.beans.BeanUtils;

/**
* @author songyibao
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-06-02 15:00:36
*/
public interface UserService extends IService<User> {
    public static UserVO toUserVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    public long userRegister(String userAccount, String userPassword, String checkPassword);

}
