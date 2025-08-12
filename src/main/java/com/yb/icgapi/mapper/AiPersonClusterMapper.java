package com.yb.icgapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yb.icgapi.model.dto.AiPersonCluster.AiPersonClusterDTO;
import com.yb.icgapi.model.entity.AiPersonCluster;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author songyibao
* @description 针对表【ai_person_cluster(【AI】人物簇 (人脸聚类结果))】的数据库操作Mapper
* @createDate 2025-07-25 09:04:51
* @Entity com.yb.icgapi.model.entity.AiPersonCluster
*/
public interface AiPersonClusterMapper extends BaseMapper<AiPersonCluster> {
    /**
     * 自定义查询：联表查询人物簇列表，并附带封面图片URL
     *
     * @param userId 用户ID
     * @return 包含封面URL的人物簇DTO列表
     */
    List<AiPersonClusterDTO > listClusterWithCoverUrl(@Param("userId") Long userId,
                                                      @Param("spaceId") Long spaceId);
}




