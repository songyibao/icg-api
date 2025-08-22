package com.yb.icgapi.icpic.interfaces.controller;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.icpic.application.service.PictureApplicationService;
import com.yb.icgapi.icpic.infrastructure.annotation.AuthCheck;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.GetOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.yb.icgapi.icpic.infrastructure.common.BaseResponse;
import com.yb.icgapi.icpic.infrastructure.common.DeleteRequest;
import com.yb.icgapi.icpic.infrastructure.common.ResultUtils;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.icpic.domain.user.constant.UserConstant;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.interfaces.assembler.PictureAssembler;
import com.yb.icgapi.manager.auth.SpaceUserAuthManager;
import com.yb.icgapi.manager.auth.annotation.SaSpaceCheckPermission;
import com.yb.icgapi.icpic.interfaces.dto.picture.*;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.model.vo.PictureTagCategory;
import com.yb.icgapi.icpic.interfaces.vo.picture.PictureVO;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private PictureApplicationService pictureApplicationService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    @PostMapping(value = "/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            // 关键改动：去掉 @RequestBody 注解
            PictureUploadRequest pictureUploadRequest,
            @RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request) {

        User loginUser = userApplicationService.getLoginUser(request);
        // 业务逻辑不变
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                    HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        // 校验图片
        ThrowUtils.ThrowIf(pictureUploadRequest == null, ErrorCode.PARAM_BLANK);
        String fileUrl = pictureUploadRequest.getFileUrl();
        ThrowUtils.ThrowIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAM_BLANK, "图片地址不能为空");
        // 上传图片
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片（仅限本人或者管理员，在内部进行逻辑校验，不使用注解）
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(deleteRequest == null || deleteRequest.getId() == null ||
                deleteRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 更新图片信息（仅限管理员）
     *
     * @param pictureUpdateRequest 图片更新请求
     * @return 更新结果
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(pictureUpdateRequest == null, ErrorCode.PARAM_BLANK);
        ThrowUtils.ThrowIf(pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        boolean res = pictureApplicationService.updatePicture(picture, userApplicationService.getLoginUser(request));
        return ResultUtils.success(res);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.ThrowIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureApplicationService.getPictureById(id));
    }

    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.ThrowIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        PictureVO pictureVO = pictureApplicationService.getPictureVOById(id,request);

        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        return ResultUtils.success(pictureApplicationService.listPictureByPage(pictureQueryRequest));
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        return ResultUtils.success(pictureApplicationService.listPictureVOByPage(pictureQueryRequest));
    }


    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(pictureEditRequest == null, ErrorCode.PARAM_BLANK);
        ThrowUtils.ThrowIf(pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        return ResultUtils.success(pictureApplicationService.listPIctureTagCategory());
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.ThrowIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.ThrowIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Integer uploaded = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploaded);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.ThrowIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        List<ImageSearchResult> resultList =
                pictureApplicationService.searchPictureByPicture(searchPictureByPictureRequest);
        return ResultUtils.success(resultList);
    }

    /**
     * 颜色搜图
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String color = searchPictureByColorRequest.getPicColor();
        Long sapceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.ThrowIf(StrUtil.isBlank(color), ErrorCode.PARAM_BLANK, "颜色不能为空");
        ThrowUtils.ThrowIf(sapceId == null || sapceId <= 0, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
        List<PictureVO> pictureVOList = pictureApplicationService.searchPictureByColor(sapceId, color,
                loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 提交AI扩图任务接口
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        ThrowUtils.ThrowIf(createPictureOutPaintingTaskRequest == null, ErrorCode.PARAM_BLANK);
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        ThrowUtils.ThrowIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        CreateOutPaintingTaskResponse taskResponse =
                pictureApplicationService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(taskResponse);
    }
    /**
     * 查询AI扩图任务状态接口
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getOutPaintingTask(
            @RequestParam("taskId") String taskId) {
        ThrowUtils.ThrowIf(StrUtil.isBlank(taskId), ErrorCode.PARAM_BLANK, "任务ID不能为空");
        GetOutPaintingTaskResponse taskResponse = pictureApplicationService.getPictureOutPaintingTask(taskId);
        return ResultUtils.success(taskResponse);
    }
}
