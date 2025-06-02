package com.yb.icgapi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.service.UserService;
import com.yb.icgapi.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
* @author songyibao
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-06-02 15:00:36
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 检查账号和密码是否符合要求
        if (StringUtils.) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        // 检查账号是否已存在
        User existingUser = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        if (existingUser != null) {
            throw new IllegalArgumentException("账号已存在");
        }

        // 创建新用户
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(userPassword); // 密码应加密存储
        newUser.setUserName(userAccount); // 默认昵称为账号
        this.save(newUser);

        return newUser.getId();
    }
}




