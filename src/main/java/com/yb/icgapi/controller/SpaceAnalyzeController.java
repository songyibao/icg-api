package com.yb.icgapi.controller;


import com.yb.icgapi.icpic.infrastructure.common.BaseResponse;
import com.yb.icgapi.icpic.infrastructure.common.ResultUtils;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.model.dto.analyze.*;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.model.vo.analyze.*;
import com.yb.icgapi.service.SpaceAnalyzeService;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    private UserApplicationService userApplicationService;

    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> analyzeSpaceUsage(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAM_BLANK);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 执行空间使用分析
        SpaceUsageAnalyzeResponse response = spaceAnalyzeService.analyzeSpaceUsage(spaceUsageAnalyzeRequest, loginUser);
        // 返回结果
        return ResultUtils.success(response);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> analyzeSpaceCategory(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                 HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAM_BLANK);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 执行空间使用分析
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponseList =
                spaceAnalyzeService.analyzeSpaceCategory(spaceCategoryAnalyzeRequest,
                        loginUser);
        // 返回结果
        return ResultUtils.success(spaceCategoryAnalyzeResponseList);
    }

    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> resultList = spaceAnalyzeService.analyzeSpaceTag(spaceTagAnalyzeRequest,
                loginUser);
        return ResultUtils.success(resultList);
    }
    @PostMapping("/size")
    public BaseResponse<List<SpacePictureSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpacePictureSizeAnalyzeRequest spacePictureSizeAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(spacePictureSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpacePictureSizeAnalyzeResponse> resultList =
                spaceAnalyzeService.analyzeSpacePictureSize(spacePictureSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }

    @PostMapping("/user_upload")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> resultList = spaceAnalyzeService.analyzeSpaceUserUpload(spaceUserAnalyzeRequest
                , loginUser);
        return ResultUtils.success(resultList);
    }


    @PostMapping("/space_usage_rank")
    public BaseResponse<List<Space>> analyzeSpaceUsageRank(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.ThrowIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAM_BLANK);
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 执行空间使用分析
        List<Space> spaceList = spaceAnalyzeService.analyzeSpaceUsageRank(spaceRankAnalyzeRequest,
                loginUser);
        // 返回结果
        return ResultUtils.success(spaceList);
    }

}
