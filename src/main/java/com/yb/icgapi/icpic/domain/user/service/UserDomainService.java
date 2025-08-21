package com.yb.icgapi.icpic.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.interfaces.dto.user.UserQueryRequest;
import com.yb.icgapi.icpic.interfaces.vo.user.LoginUserVO;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author songyibao
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-06-02 15:00:36
 */
public interface UserDomainService {


    String getEncryptedPassword(String password);

    long userRegister(String userAccount, String userPassword, String checkPassword);

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    UserVO getUserVOById(Long id);

    /**
     * 退出登陆
     */
    boolean userLogout(HttpServletRequest request);


    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    Boolean removeById(Long id);

    boolean updateById(User user);

    User getById(long id);

    Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper);


    List<User> listByIds(Set<Long> userIdSet);

    List<UserVO> getUserVOList(List<User> userList);

    boolean save(User user);

    long addUser(User user);

    User getUserById(long id);
}
