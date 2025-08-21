package com.yb.icgapi.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.model.dto.spaceuser.SpaceUserAddRequest;
import com.yb.icgapi.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.SpaceUser;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.model.enums.SpaceRoleEnum;
import com.yb.icgapi.model.vo.SpaceUserVO;
import com.yb.icgapi.model.vo.SpaceVO;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.service.SpaceUserService;
import com.yb.icgapi.icpic.infrastructure.mapper.SpaceUserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author songyibao
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-08-05 13:56:39
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        ThrowUtils.ThrowIf(ObjectUtil.isNull(spaceId) || ObjectUtil.isNull(userId), ErrorCode.PARAM_BLANK, "空间ID或用户ID不能为空");
        // 如果是新增操作，校验空间用户是否已存在
        if (add) {
            Space space = spaceService.getById(spaceId);
            User user = userApplicationService.getUserById(userId);
            ThrowUtils.ThrowIf(ObjUtil.isNull(space) || ObjUtil.isNull(user), ErrorCode.NOT_FOUND, "空间或用户不存在");
            SpaceUser existingSpaceUser = this.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            ThrowUtils.ThrowIf(ObjectUtil.isNotNull(existingSpaceUser), ErrorCode.ALREADY_EXISTS, "用户已是该空间的成员");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        ThrowUtils.ThrowIf(spaceRole == null || SpaceRoleEnum.getEnumByValue(spaceRole) == null,
                ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 填充用户信息 空间信息
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        ThrowUtils.ThrowIf(userId == null || spaceId == null, ErrorCode.PARAM_BLANK);
        // 用户信息
        User user = userApplicationService.getUserById(userId);
        UserVO userVO = User.toVO(user);
        spaceUserVO.setUser(userVO);
        // 空间信息
        Space space = spaceService.getById(spaceId);
        // 这里面包含了创建该空间的用户信息
        spaceUserVO.setSpace(spaceService.getSpaceVO(space));
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());
        // 批量查询用户信息
        Set<Long> userIdsSet = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        Map<Long,User> userMap = userApplicationService.listByIds(userIdsSet)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        // 批量查询空间信息
        Set<Long> spaceIdsSet = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        Map<Long, Space> spaceMap = spaceService.listByIds(spaceIdsSet)
                .stream()
                .collect(Collectors.toMap(Space::getId, space -> space));
        Set<Long> spaceCreatorIdsSet = spaceMap.values().stream()
                .map(Space::getUserId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        Map<Long,User> spaceCreatorMap = userApplicationService.listByIds(spaceCreatorIdsSet)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        // 填充用户信息和空间信息
        for (SpaceUserVO spaceUserVO : spaceUserVOList) {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            if (userId != null && userMap.containsKey(userId)) {
                User user = userMap.get(userId);
                spaceUserVO.setUser(User.toVO(user));
            }
            if (spaceId != null && spaceMap.containsKey(spaceId)) {
                Space space = spaceMap.get(spaceId);
                SpaceVO spaceVO = SpaceVO.objToVo(space);
                // 填充空间创建者信息
                Long spaceCreatorId = space.getUserId();
                if (spaceCreatorId != null && spaceCreatorMap.containsKey(spaceCreatorId)) {
                    User spaceCreator = spaceCreatorMap.get(spaceCreatorId);
                    spaceVO.setUser(User.toVO(spaceCreator));
                }
                spaceUserVO.setSpace(spaceVO);
            }
        }
        return spaceUserVOList;
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {


        if (spaceUserQueryRequest == null) {
            return new QueryWrapper<>();
        }
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        // 添加查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 创建新的空间用户关联
        SpaceUser newSpaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, newSpaceUser);
        if(ObjUtil.isEmpty(newSpaceUser.getSpaceRole())){
            newSpaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
        }
        validSpaceUser(newSpaceUser, true);

        boolean isSaved = this.save(newSpaceUser);
        ThrowUtils.ThrowIf(!isSaved, ErrorCode.OPERATION_ERROR);
        return newSpaceUser.getId();

    }
}




