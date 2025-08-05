package com.yb.icgapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.model.dto.space.SpaceAddRequest;
import com.yb.icgapi.model.dto.spaceuser.SpaceUserAddRequest;
import com.yb.icgapi.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.SpaceUserVO;
import com.yb.icgapi.model.vo.SpaceVO;

import java.util.List;

/**
* @author songyibao
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-08-05 13:56:39
*/
public interface SpaceUserService extends IService<SpaceUser> {
    public void validSpaceUser(SpaceUser spaceUser, boolean add);


    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);


}
