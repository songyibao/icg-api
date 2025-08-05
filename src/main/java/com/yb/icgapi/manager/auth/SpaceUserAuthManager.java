package com.yb.icgapi.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yb.icgapi.manager.auth.model.SpaceUserAuthConfig;
import com.yb.icgapi.manager.auth.model.SpaceUserPermission;
import com.yb.icgapi.manager.auth.model.SpaceUserRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static{
        String jsonStr = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(jsonStr, SpaceUserAuthConfig.class);
    }

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
}
