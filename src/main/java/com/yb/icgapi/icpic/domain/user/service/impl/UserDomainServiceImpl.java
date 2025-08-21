package com.yb.icgapi.icpic.domain.user.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.icpic.domain.user.constant.UserConstant;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.domain.user.repository.UserRepository;
import com.yb.icgapi.icpic.domain.user.service.UserDomainService;
import com.yb.icgapi.icpic.domain.user.valueobject.UserRoleEnum;
import com.yb.icgapi.icpic.infrastructure.constant.DatabaseConstant;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.interfaces.dto.user.UserQueryRequest;
import com.yb.icgapi.icpic.interfaces.vo.user.LoginUserVO;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import com.yb.icgapi.manager.auth.StpKit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author songyibao
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-06-02 15:00:36
 */
@Slf4j
@Service
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    UserRepository userRepository;
    @Override
    public String getEncryptedPassword(String password) {
        // 加盐值
        return DigestUtils.md5DigestAsHex((UserConstant.SALT + password).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 检查账号是否已存在
        User existingUser = userRepository.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.ThrowIf(existingUser != null, ErrorCode.USER_EXIST);
        // 创建新用户
        User newUser = new User();
        newUser.setUserAccount(userAccount);
        newUser.setUserPassword(getEncryptedPassword(userPassword));
        newUser.setUserName(userAccount); // 默认昵称为账号
        newUser.setUserRole(UserRoleEnum.USER.getValue());
        boolean res = userRepository.save(newUser);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "数据库错误");

        return newUser.getId();
    }


    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 账号是否存在
        User user = userRepository.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        ThrowUtils.ThrowIf(user == null, ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        // 密码是否正确
        String encryptedPassword = getEncryptedPassword(userPassword);
        ThrowUtils.ThrowIf(!user.getUserPassword().equals(encryptedPassword), ErrorCode.PASSWORD_ERROR, "密码错误");
        // 保存登陆态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录用户登录状态到Sa-Token，便于空间鉴权时使用,注意保证与SpringSession中的过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        // 返回脱敏视图
        return User.toLoginUserVO(user);
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

        currentUser = userRepository.getById(currentUser.getId());
        // 如果用户不存在，抛出异常
        ThrowUtils.ThrowIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户状态异常");
        // 返回用户信息
        return currentUser;
    }

    @Override
    public UserVO getUserVOById(Long id) {
        // 参数校验
        ThrowUtils.ThrowIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "用户id无效");
        // 查询用户
        User user = userRepository.getById(id);
        ThrowUtils.ThrowIf(user == null, ErrorCode.NOT_FOUND, "用户不存在");
        // 转换为VO对象
        // 返回用户视图对象
        return User.toVO(user);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            log.warn("用户未登录，退出登陆操作无效");
            return false;
        }
        // 清除用户登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        log.info("用户退出登录成功");
        return true;
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
                        sortOrder.equals(DatabaseConstant.ASC), sortField);

        return queryWrapper;
    }
    @Override
    public Boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(userPage, queryWrapper);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return  userList.stream()
                .map(User::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean save(User user){
        return userRepository.save(user);
    }

    @Override
    public long addUser(User user) {
        user.setUserPassword(getEncryptedPassword(UserConstant.DEFAULT_PASSWORD));
        boolean res = userRepository.save(user);
        ThrowUtils.ThrowIf(!res,ErrorCode.SERVER_ERROR);
        return user.getId();
    }

    @Override
    public User getUserById(long id) {
        User user = userRepository.getById(id);
        ThrowUtils.ThrowIf(user == null,ErrorCode.NOT_FOUND);
        return user;
    }
}




