package com.yb.icgapi.icpic.interfaces.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.icpic.infrastructure.annotation.AuthCheck;
import com.yb.icgapi.icpic.infrastructure.common.BaseResponse;
import com.yb.icgapi.icpic.infrastructure.common.DeleteRequest;
import com.yb.icgapi.icpic.infrastructure.common.ResultUtils;
import com.yb.icgapi.icpic.domain.user.constant.UserConstant;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.interfaces.assembler.UserAssembler;
import com.yb.icgapi.icpic.interfaces.dto.user.*;
import com.yb.icgapi.icpic.interfaces.vo.user.LoginUserVO;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@Api(tags = "用户相关接口")
public class UserController {
    @Resource
    private UserApplicationService userApplicationService;

    @ApiOperation(value = "用户注册", notes = "用户注册接口")
    @ApiImplicitParam(name = "userRegisterRequest", value = "用户注册请求体", required = true, dataType = "UserRegisterRequest")
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.ThrowIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long userId = userApplicationService.userRegister(userRegisterRequest);
        return ResultUtils.success(userId);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userApplicationService.userLogin(userLoginRequest,request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        return ResultUtils.success(User.toLoginUserVO(loginUser));
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        userApplicationService.userLogout(request);
        return ResultUtils.success(true);
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.ThrowIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = UserAssembler.toUserEntity(userAddRequest);
        return ResultUtils.success(userApplicationService.addUser(user));
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(@RequestParam("id") Long id) {
        ThrowUtils.ThrowIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.getUserById(id));
    }

    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<UserVO> getUserVOById(@RequestParam("id") Long id) {
        ThrowUtils.ThrowIf(id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.getUserVOById(id));
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.ThrowIf(deleteRequest == null, ErrorCode.PARAM_BLANK);
        return ResultUtils.success(userApplicationService.deleteUser(deleteRequest));
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.ThrowIf(userUpdateRequest == null || userUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = UserAssembler.toUserEntity(userUpdateRequest);
        return ResultUtils.success(userApplicationService.updateUser(user));
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.ThrowIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.listUserVOByPage(userQueryRequest));
    }

}
