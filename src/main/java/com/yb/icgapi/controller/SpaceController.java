package com.yb.icgapi.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.annotation.AuthCheck;
import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.common.BeanCopyUtils;
import com.yb.icgapi.common.DeleteRequest;
import com.yb.icgapi.common.ResultUtils;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.constant.UserConstant;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.manager.auth.annotation.SaSpaceCheckPermission;
import com.yb.icgapi.model.dto.space.*;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.SpaceLevelEnum;
import com.yb.icgapi.model.enums.SpaceTypeEnum;
import com.yb.icgapi.model.vo.SpaceVO;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        // 获取空间等级列表
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        // 返回结果
        return ResultUtils.success(spaceLevelList);
    }

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,
                                       HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceAddRequest == null, ErrorCode.PARAM_BLANK);
        User loginUser = userService.getLoginUser(request);
        Long id = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 删除空间（仅限本人或者管理员，在内部进行逻辑校验，不使用注解）
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_MANAGE)
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() < 0, ErrorCode.PARAMS_ERROR);

        long id = deleteRequest.getId();

        // 改为注解校验
        // 判断是否存在，注解中：如果spaceId不为空，则会自动查询空间是否存在，不存在则抛出异常
//        Space oldSpace = spaceService.getById(id);
//        ThrowUtils.ThrowIf(oldSpace == null, ErrorCode.NOT_FOUND, "空间不存在");
        // 判断是否是本人或者管理员
//        User loginUser = userService.getLoginUser(request);
//        if (!oldSpace.getUserId().equals(loginUser.getId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)) {
//            throw new BusinessException(ErrorCode.NO_AUTHORIZED);
//        }
        // 操作数据库
        boolean res = spaceService.removeById(id);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "删除空间失败");
        return ResultUtils.success(true);
    }


    /**
     * 更新空间信息（仅限owner和管理员）
     *
     * @param spaceUpdateRequest 空间更新请求
     * @return 更新结果
     */
    @PostMapping("/update")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_MANAGE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceUpdateRequest == null, ErrorCode.PARAM_BLANK);
        Long updateId = spaceUpdateRequest.getId();
        ThrowUtils.ThrowIf(updateId == null || updateId <= 0,
                ErrorCode.PARAMS_ERROR);

        Space toUpdateSpace = spaceService.getById(updateId);
        // 防止前端未传的null字段覆盖原始数据
        BeanUtils.copyProperties(spaceUpdateRequest, toUpdateSpace,
                BeanCopyUtils.getNullPropertyNames(spaceUpdateRequest));
        // 自动填充空间级别相关信息
        spaceService.fillSpaceBySpaceLevel(toUpdateSpace);
        // 数据校验
        spaceService.validSpace(toUpdateSpace, false);
        // 判断旧空间是否存在,改为注解校验
//        Space oldSpace = spaceService.getById(toUpdateSpace.getId());
//        ThrowUtils.ThrowIf(oldSpace == null, ErrorCode.NOT_FOUND, "空间不存在");
        // 更新空间信息
        boolean res = spaceService.updateById(toUpdateSpace);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "更新空间失败");
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_MANAGE)
    public BaseResponse<Space> getSpaceById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.ThrowIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.ThrowIf(space == null, ErrorCode.NOT_FOUND, "空间不存在");
        return ResultUtils.success(space);
    }

    @GetMapping("/get/vo")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_MANAGE)
    public BaseResponse<SpaceVO> getSpaceVOById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.ThrowIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Space space = spaceService.lambdaQuery()
                .eq(Space::getId, id)
                .one();
        ThrowUtils.ThrowIf(space == null, ErrorCode.NOT_FOUND, "用户未创建过空间");
        // 如果传来的id与根据登录用户的id查询到的空间id不一致，则抛出异常
        // 改为注解校验
//        ThrowUtils.ThrowIf(!id.equals(space.getId()), ErrorCode.PARAMS_ERROR, "空间id参数错误");
        return ResultUtils.success(spaceService.getSpaceVO(space));
    }

    /**
     * 获取登录用户的私有空间VO对象
     *
     * @param request
     * @return
     */
    @GetMapping("/get/vo/unique")
    public BaseResponse<SpaceVO> getSpaceVOByLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Space space = spaceService.lambdaQuery()
                .eq(Space::getUserId, loginUser.getId())
                .eq(Space::getSpaceType, SpaceTypeEnum.PRIVATE.getValue())
                .one();
        ThrowUtils.ThrowIf(space == null, ErrorCode.NOT_FOUND, "用户未创建过空间");
        // 返回空间VO对象
        return ResultUtils.success(spaceService.getSpaceVO(space));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        long currentPage = spaceQueryRequest.getCurrentPage();
        long pageSize = spaceQueryRequest.getPageSize();

        Page<Space> page = spaceService.page(
                new Page<>(currentPage, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest)
        );
        return ResultUtils.success(page);
    }


    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_MANAGE)
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceEditRequest == null, ErrorCode.PARAM_BLANK);
        ThrowUtils.ThrowIf(spaceEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 填充空间等级相关信息
        spaceService.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        spaceService.validSpace(space, false);
        User loginUser = userService.getLoginUser(request);
        // 判断旧空间是否存在
        Space oldSpace = spaceService.getById(space.getId());
        ThrowUtils.ThrowIf(oldSpace == null, ErrorCode.NOT_FOUND, "空间不存在");
        // 仅本人和管理员可编辑，改为注解校验
//        if (!oldSpace.getUserId().equals(loginUser.getId()) && !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE)) {
//            throw new BusinessException(ErrorCode.NO_AUTHORIZED);
//        }
        // 更新空间信息
        boolean res = spaceService.updateById(space);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "编辑空间失败");
        return ResultUtils.success(true);
    }

}
