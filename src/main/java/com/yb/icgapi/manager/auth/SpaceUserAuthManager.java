package com.yb.icgapi.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.manager.auth.model.SpaceUserAuthConfig;
import com.yb.icgapi.manager.auth.model.SpaceUserRole;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.SpaceUser;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.SpaceRoleEnum;
import com.yb.icgapi.model.enums.SpaceTypeEnum;
import com.yb.icgapi.service.SpaceUserService;
import com.yb.icgapi.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class SpaceUserAuthManager {

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static{
        String jsonStr = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(jsonStr, SpaceUserAuthConfig.class);
    }

    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;

    /**
     * 根据角色获取权限列表
     * @param spaceUserRole 角色标识
     * @return 权限列表
     * 如果角色不存在或角色标识为空，则返回空列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole){
         if(StrUtil.isBlank(spaceUserRole)){
            return new ArrayList<>();
         }
        SpaceUserRole findRole = SPACE_USER_AUTH_CONFIG.getRoles()
                .stream()
                .filter(r -> r.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);
        if(findRole == null){
            return new ArrayList<>();
        }else{
            return findRole.getPermissions();
        }
    }

    public List<String> getPermissionList(Space space, User loginUser){
        if(loginUser == null){
            return new ArrayList<>();
        }
        // 超级管理员（空间owner）权限
        List<String> OWNER_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.OWNER.getValue());
        // 管理员权限(相当于一般的全部权限)
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 如果是公共图库
        if(space == null){
            if(userService.isAdmin(loginUser)){
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        // 判断空间类别
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.fromValue(spaceType);
        switch(Objects.requireNonNull(spaceTypeEnum)){
            case PRIVATE:
                // 私有空间
                if(space.getUserId().equals(loginUser.getId())){
                    // owner权限
                    return OWNER_PERMISSIONS;
                } else if( userService.isAdmin(loginUser)) {
                    // 管理员权限，这里设定系统管理员可以管理别人的私有空间
//                    return ADMIN_PERMISSIONS;
                    return OWNER_PERMISSIONS;
                }else{
                    // 不是自己的空间，返回空权限
                    return new ArrayList<>();
                }
            case SHARE:
                // 团队空间，根据角色返回权限
                Long spaceId = space.getId();
                // 查询空间用户 角色
                SpaceUser spaceUser = spaceUserService.getOne(new LambdaQueryWrapper<SpaceUser>()
                        .eq(SpaceUser::getSpaceId, spaceId)
                        .eq(SpaceUser::getUserId, loginUser.getId()));
                if(spaceUser == null){
                    // 不属于该空间，返回空权限
                    return new ArrayList<>();
                } else {
                    // 返回对应角色的权限列表
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
            default:
                break;
        }
        return new ArrayList<>();
    }
}
