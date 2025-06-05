package com.yb.icgapi.service.impl;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.constant.UserConstant;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.model.dto.user.UserQueryRequest;
import com.yb.icgapi.model.vo.LoginUserVO;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.UserRoleEnum;
import com.yb.icgapi.model.vo.UserVO;
import com.yb.icgapi.service.UserService;
import com.yb.icgapi.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author songyibao
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-06-02 15:00:36
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Override
    public String getEncryptedPassword(String password) {
        // 加盐值
        final String salt = "icggci";
        return DigestUtils.md5DigestAsHex((salt + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 参数是否为空
        ThrowUtils.ThrowIf(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAM_BLANK);
        //密码不少于8位
        ThrowUtils.ThrowIf(userPassword.length() < 8, ErrorCode.PASSWORD_TOO_SHORT);
        // 密码是否一致
        ThrowUtils.ThrowIf(!userPassword.equals(checkPassword), ErrorCode.PASSWORD_NOT_MATCH);
        // 检查账号是否已存在
        User existingUser = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.ThrowIf(existingUser != null, ErrorCode.USER_EXIST);
        // 创建新用户
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(getEncryptedPassword(userPassword));
        newUser.setUserName(userAccount); // 默认昵称为账号
        newUser.setUserRole(UserRoleEnum.USER.getValue());
        boolean res = this.save(newUser);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "数据库错误");

        return newUser.getId();
    }

    @Override
    public LoginUserVO toLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.校验
        ThrowUtils.ThrowIf(StringUtils.isAnyBlank(userAccount, userPassword), ErrorCode.PARAM_BLANK);
        // 账号密码长度
        ThrowUtils.ThrowIf(userAccount.length() < 4 || userPassword.length() < 8, ErrorCode.PARAMS_ERROR);
        // 账号是否存在
        User user = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.ThrowIf(user == null, ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        // 密码是否正确
        String encryptedPassword = getEncryptedPassword(userPassword);
        ThrowUtils.ThrowIf(!user.getUserPassword().equals(encryptedPassword), ErrorCode.PASSWORD_ERROR, "密码错误");
        // 保存登陆态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 返回脱敏视图
        return this.toLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 获取用户信息
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = null;
        if (userObj instanceof User) {
            currentUser = (User) userObj;
        }
        // 如果用户未登录，抛出异常
        ThrowUtils.ThrowIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);

        currentUser = this.getById(currentUser.getId());
        // 如果用户不存在，抛出异常
        ThrowUtils.ThrowIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户状态异常");
        // 返回用户信息
        return currentUser;
    }

    @Override
    public void userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            log.warn("用户未登录，退出登陆操作无效");
            return;
        }
        // 清除用户登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        log.info("用户退出登录成功");
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return Collections.emptyList();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            return null;
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        int currentPage = userQueryRequest.getCurrentPage();
        int pageSize = userQueryRequest.getPageSize();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 添加查询条件
        queryWrapper.eq(id != null, "id", id)
                .like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount)
                .like(StringUtils.isNotBlank(userName), "userName", userName)
                .like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile)
                .eq(StringUtils.isNotBlank(userRole), "userRole", userRole)
                .orderBy(StringUtils.isNotBlank(sortField) && StringUtils.isNotBlank(sortOrder),
                        sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }
}




