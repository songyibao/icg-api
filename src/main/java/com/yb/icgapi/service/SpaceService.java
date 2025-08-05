package com.yb.icgapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.model.dto.picture.PictureQueryRequest;
import com.yb.icgapi.model.dto.space.SpaceAddRequest;
import com.yb.icgapi.model.dto.space.SpaceQueryRequest;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.SpaceVO;
import com.yb.icgapi.model.vo.SpaceVO;

/**
* @author songyibao
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-07-17 15:01:35
*/
public interface SpaceService extends IService<Space> {

    public void validSpace(Space space,boolean add);

    public void fillSpaceBySpaceLevel(Space space);

    public SpaceVO getSpaceVO(Space space);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    public Page<SpaceVO> listSpaceVOByPage(SpaceQueryRequest spaceQueryRequest);

    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    void checkSpaceAuth(User user, Long spaceId);

    void checkSpaceAuth(User user, Space space);

}
