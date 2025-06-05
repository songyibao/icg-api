package com.yb.icgapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yb.icgapi.model.dto.user.UserQueryRequest;
import com.yb.icgapi.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.vo.LoginUserVO;
import com.yb.icgapi.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author songyibao
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-06-02 15:00:36
*/
public interface UserService extends IService<User> {

    public String getEncryptedPassword(String password);
    LoginUserVO toLoginUserVO(User user);

    public long userRegister(String userAccount, String userPassword, String checkPassword);
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    /**
     * 退出登陆
     */
    void userLogout(HttpServletRequest request);

    UserVO getUserVO(User user);
    List<UserVO> getUserVOList(List<User> userList);

    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
