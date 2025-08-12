package com.yb.icgapi.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.mapper.AiPersonClusterMapper;
import com.yb.icgapi.model.dto.AiPersonCluster.AiPersonClusterDTO;
import com.yb.icgapi.model.dto.ai.BatchReprocessMessage;
import com.yb.icgapi.model.entity.*;
import com.yb.icgapi.model.enums.SpaceTypeEnum;
import com.yb.icgapi.model.vo.AiDetectedFaceVO;
import com.yb.icgapi.model.vo.AiPersonClusterVO;
import com.yb.icgapi.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author songyibao
* @description 针对表【ai_person_cluster(【AI】人物簇 (人脸聚类结果))】的数据库操作Service实现
* @createDate 2025-07-25 09:04:51
*/
@Service
public class AiPersonClusterServiceImpl extends ServiceImpl<AiPersonClusterMapper, AiPersonCluster>
    implements AiPersonClusterService{

    @Resource
    private AiDetectedFaceService aiDetectedFaceService;
    @Resource
    private PictureService pictureService;
    @Resource
    private AiPersonClusterMapper aiPersonClusterMapper;
    @Resource
    private AIMessageService aiMessageService;
    @Autowired
    private SpaceService spaceService;

    @Override
    public List<AiPersonClusterVO> getPrivatePersonCluster(User loginUser) {
        // 这里可以添加获取当前用户私有空间的所有人物簇信息的逻辑
        ThrowUtils.ThrowIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = loginUser.getId();
        Long spaceId = spaceService.listObjs(
                new LambdaQueryWrapper<Space>()
                        .select(Space::getId)
                        .eq(Space::getUserId,userId)
                        .eq(Space::getSpaceType, SpaceTypeEnum.PRIVATE.getValue()),
                obj -> (Long) obj
        ).get(0);
        List<AiPersonClusterDTO> clusterDTOs =
                aiPersonClusterMapper.listClusterWithCoverUrl(userId,spaceId);
        // 2. 将DTO列表转换为最终的VO列表返回给前端
        // 这一步逻辑非常清晰，就是简单的对象属性复制
        // 直接转换即可，objToVo会处理好一切
        // 使用方法引用，更加简洁
        return clusterDTOs.stream()
                .map(AiPersonClusterVO::objToVo) // 使用方法引用，更加简洁
                .collect(Collectors.toList());
    }

    @Override
    public List<AiDetectedFaceVO> getClusterDetectedFaces(User loginUser, Long clusterId) {
        // 首先判断 clusterId 是否属于当前用户
        AiPersonCluster cluster = this.lambdaQuery()
                .eq(AiPersonCluster::getId, clusterId)
                .eq(AiPersonCluster::getUserId, loginUser.getId())
                .one();
        ThrowUtils.ThrowIf(cluster == null, ErrorCode.NOT_FOUND, "指定的人物簇不存在或不属于当前用户");
        // 获取当前人物簇的所有检测到的人脸
        List<AiDetectedFace> detectedFaces = aiDetectedFaceService.lambdaQuery()
                .eq(AiDetectedFace::getClusterId, clusterId)
                .list();
        ThrowUtils.ThrowIf(CollUtil.isEmpty(detectedFaces), ErrorCode.NOT_FOUND, "当前人物簇没有检测到人脸");
        // 获取所有检测到的人脸对应的图片ID
        Set<Long> pictureIds = detectedFaces.stream()
                .map(AiDetectedFace::getPictureId)
                .filter(Objects::nonNull) // 过滤掉null值
                .collect(Collectors.toSet());
        // 查询所有图片的URL
        Map<Long, String> pictureIdToUrlMap = pictureService.lambdaQuery()
                .in(Picture::getId, pictureIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Picture::getId, Picture::getUrl));
        Map<Long, String> pictureIdToThumbnailUrlMap = pictureService.lambdaQuery()
                .in(Picture::getId, pictureIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Picture::getId, Picture::getThumbnailUrl));
        // 将检测到的人脸转换为VO
        List<AiDetectedFaceVO> detectedFaceVOs = detectedFaces.stream()
                .map(detectedFace -> {
                    AiDetectedFaceVO faceVO = AiDetectedFaceVO.objToVo(detectedFace);
                    // 设置图片URL
                    Long pictureId = detectedFace.getPictureId();
                    if (pictureId != null) {
                        faceVO.setPictureUrl(pictureIdToUrlMap.get(pictureId));
                        faceVO.setPictureThumbnailUrl(pictureIdToThumbnailUrlMap.get(pictureId));
                    }
                    return faceVO;
                })
                .collect(Collectors.toList());
        // 返回检测到的人脸VO列表
        return detectedFaceVOs;
    }

    @Async
    public void asyncReconstructPersonCluster(User loginUser) {
        // 1. 获取当前用户的所有人物簇的id
        List<Long> clusterIds = this.listObjs(
                new LambdaQueryWrapper<AiPersonCluster>()
                        .eq(AiPersonCluster::getUserId, loginUser.getId())
                        .select(AiPersonCluster::getId),
                obj-> (Long) obj
        );
        if(CollUtil.isNotEmpty(clusterIds)){
            // 2. 根据人物簇id查询所有检测到的人脸
            List<AiDetectedFace> detectedFaces = aiDetectedFaceService.lambdaQuery()
                    .in(AiDetectedFace::getClusterId, clusterIds)
                    .list();
            // 3. 删除检测到的人脸
            if(CollUtil.isNotEmpty(detectedFaces)){
                aiDetectedFaceService.remove(
                        new LambdaQueryWrapper<AiDetectedFace>()
                                .in(AiDetectedFace::getClusterId, clusterIds)
                );
            }
            // 4. 删除人物簇
            boolean deleteClusterResult = this.remove(
                    new LambdaQueryWrapper<AiPersonCluster>()
                            .eq(AiPersonCluster::getUserId, loginUser.getId())
            );
        }

        // 5. 使用新的批量重新处理消息格式发送消息
        // 创建处理选项，只启用人脸识别
        BatchReprocessMessage.ProcessingOptions options = new BatchReprocessMessage.ProcessingOptions();
        options.setIncludeOCR(true);
        options.setIncludeCLIP(true);
        options.setIncludeFaces(true);
        options.setBatchSize(5); // 设置较大的批处理大小以提高效率
        Long spaceId = spaceService.lambdaQuery()
                .eq(Space::getUserId, loginUser.getId())
                .select(Space::getId)
                .oneOpt()
                .map(Space::getId)
                .orElse(null);
        // 发送批量重新处理消息，处理用户所有空间的图片
        aiMessageService.sendBatchReprocessMessage(loginUser.getId(), spaceId, options);
    }

    @Override
    public boolean reconstructPersonCluster(User loginUser) {
        // 异步执行重建人物簇
        asyncReconstructPersonCluster(loginUser);
        return true;
    }
}
