package com.yb.icgapi.controller;

import cn.hutool.core.util.ObjectUtil;
import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.common.DeleteRequest;
import com.yb.icgapi.common.ResultUtils;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.manager.auth.annotation.SaSpaceCheckPermission;
import com.yb.icgapi.model.dto.spaceuser.SpaceUserAddRequest;
import com.yb.icgapi.model.dto.spaceuser.SpaceUserEditRequest;
import com.yb.icgapi.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yb.icgapi.model.entity.SpaceUser;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.SpaceRoleEnum;
import com.yb.icgapi.model.vo.SpaceUserVO;
import com.yb.icgapi.service.SpaceUserService;
import com.yb.icgapi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.ThrowIf(oldSpaceUser == null, ErrorCode.NOT_FOUND);
        // 操作数据库
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.ThrowIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.ThrowIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.ThrowIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.ThrowIf(spaceUser == null, ErrorCode.NOT_FOUND);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(spaceUserEditRequest.getSpaceRole().equals(SpaceRoleEnum.OWNER.getValue())){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"暂不支持修改空间创建者");
        }
        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.ThrowIf(oldSpaceUser == null, ErrorCode.NOT_FOUND);
        // 将实体类和 DTO 进行转换
//        SpaceUser spaceUser = new SpaceUser();
//        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        ThrowUtils.ThrowIf(oldSpaceUser.getSpaceRole().equals(SpaceRoleEnum.OWNER.getValue()),
                ErrorCode.OPERATION_ERROR,"暂时不支持修改空间创建者");
        // 只允许修改非创建者角色
        oldSpaceUser.setSpaceRole(spaceUserEditRequest.getSpaceRole());
        spaceUserService.validSpaceUser(oldSpaceUser, false);
        // 操作数据库
        boolean result = spaceUserService.updateById(oldSpaceUser);
        ThrowUtils.ThrowIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }
}
