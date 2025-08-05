package com.yb.icgapi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.dto.analyze.*;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.analyze.*;

import java.util.List;

/**
* @author songyibao
* @description 针对空间进行分析
*/
public interface SpaceAnalyzeService extends IService<Space> {

    public SpaceUsageAnalyzeResponse analyzeSpaceUsage(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    List<SpaceCategoryAnalyzeResponse> analyzeSpaceCategory(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    List<SpaceTagAnalyzeResponse> analyzeSpaceTag(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                  User loginUser);

    List<SpacePictureSizeAnalyzeResponse> analyzeSpacePictureSize(SpacePictureSizeAnalyzeRequest spacePictureSizeAnalyzeRequest, User loginUser);

    List<SpaceUserAnalyzeResponse> analyzeSpaceUserUpload(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    List<Space> analyzeSpaceUsageRank(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                      User loginUser);
}
