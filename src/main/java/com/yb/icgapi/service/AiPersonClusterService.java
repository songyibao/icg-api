package com.yb.icgapi.service;

import com.yb.icgapi.model.entity.AiPersonCluster;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.AiDetectedFaceVO;
import com.yb.icgapi.model.vo.AiPersonClusterVO;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

/**
* @author songyibao
* @description 针对表【ai_person_cluster(【AI】人物簇 (人脸聚类结果))】的数据库操作Service
* @createDate 2025-07-25 09:04:51
*/
public interface AiPersonClusterService extends IService<AiPersonCluster> {

    // 人物簇相关的方法
    /**
     * 获取当前用户私有空间的所有人物簇信息
     */
    List<AiPersonClusterVO> getPrivatePersonCluster(User loginUser);

    List<AiDetectedFaceVO> getClusterDetectedFaces(User loginUser, Long clusterId);

    boolean reconstructPersonCluster(User loginUser);

    @Async
    void asyncReconstructPersonCluster(User loginUser);
}
