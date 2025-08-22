package com.yb.icgapi.icpic.application.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import com.yb.icgapi.icpic.domain.user.constant.UserConstant;
import com.yb.icgapi.icpic.domain.user.service.UserDomainService;
import com.yb.icgapi.icpic.infrastructure.common.DeleteRequest;
import com.yb.icgapi.icpic.infrastructure.exception.BusinessException;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.interfaces.dto.user.UserLoginRequest;
import com.yb.icgapi.icpic.interfaces.dto.user.UserQueryRequest;
import com.yb.icgapi.icpic.interfaces.dto.user.UserRegisterRequest;
import com.yb.icgapi.icpic.interfaces.vo.user.LoginUserVO;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import com.yb.icgapi.model.dto.space.SpaceAddRequest;
import com.yb.icgapi.model.enums.SpaceLevelEnum;
import com.yb.icgapi.model.enums.SpaceTypeEnum;
import com.yb.icgapi.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author songyibao
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-06-02 15:00:36
 */
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    @Lazy
    SpaceService spaceService;
    @Resource
    private UserDomainService userDomainService;

    @Transactional
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        ThrowUtils.ThrowIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 校验
        User.validUserRegister(userAccount, userPassword, checkPassword);
        long userId = userDomainService.userRegister(userAccount, userPassword, checkPassword);
        User newUser = userDomainService.getUserById(userId);
        ThrowUtils.ThrowIf(newUser == null,ErrorCode.OPERATION_ERROR,"注册失败");
        SpaceAddRequest spaceAddRequest = new SpaceAddRequest();
        spaceAddRequest.setSpaceName("我的空间");
        spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        spaceService.addSpace(spaceAddRequest,newUser);
        return userId;
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 校验
        User.validUserLogin(userAccount, userPassword);
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    /**
     * 获取当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    /**
     * 用户注销
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        ThrowUtils.ThrowIf(request == null, ErrorCode.PARAMS_ERROR);
        return userDomainService.userLogout(request);
    }



    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }


    @Override
    public User getUserById(long id) {
        return userDomainService.getUserById(id);
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVOById(id);
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public boolean updateUser(User user) {
        return userDomainService.updateById(user);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.ThrowIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrentPage();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }


    @Override
    public long addUser(User user) {
        return userDomainService.addUser(user);
    }
}





