package com.yb.icgapi.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.annotation.AuthCheck;
import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.common.DeleteRequest;
import com.yb.icgapi.common.ResultUtils;
import com.yb.icgapi.constant.UserConstant;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.model.dto.picture.*;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.PictureReviewStatusEnum;
import com.yb.icgapi.model.vo.PictureTagCategory;
import com.yb.icgapi.model.vo.PictureVO;
import com.yb.icgapi.service.PictureService;
import com.yb.icgapi.service.SpaceService;
import com.yb.icgapi.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SpaceService spaceService;


    @PostMapping(value = "/upload")
    public BaseResponse<PictureVO> uploadPicture(
            // 关键改动：去掉 @RequestBody 注解
            PictureUploadRequest pictureUploadRequest,
            @RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        // 业务逻辑不变
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                    HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 校验图片
        ThrowUtils.ThrowIf(pictureUploadRequest == null, ErrorCode.PARAM_BLANK);
        String fileUrl = pictureUploadRequest.getFileUrl();
        ThrowUtils.ThrowIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAM_BLANK, "图片地址不能为空");
        // 上传图片
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片（仅限本人或者管理员，在内部进行逻辑校验，不使用注解）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(deleteRequest == null || deleteRequest.getId() == null ||
                deleteRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
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

        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将tags转string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断旧图片是否存在
        Picture oldPicture = pictureService.getById(picture.getId());
        ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 更新图片信息
        boolean res = pictureService.updateById(picture);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "更新图片失败");
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.ThrowIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.ThrowIf(picture == null, ErrorCode.NOT_FOUND, "图片不存在");
        return ResultUtils.success(picture);
    }

    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(@RequestParam("id") Long id, HttpServletRequest request) {
        ThrowUtils.ThrowIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.ThrowIf(picture == null, ErrorCode.NOT_FOUND, "图片不存在");
        User loginUser = userService.getLoginUser(request);
        pictureService.checkPictureAuth(loginUser, picture);
        return ResultUtils.success(pictureService.getPictureVO(picture));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long currentPage = pictureQueryRequest.getCurrentPage();
        long pageSize = pictureQueryRequest.getPageSize();

        Page<Picture> page = pictureService.page(
                new Page<>(currentPage, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest)
        );
        return ResultUtils.success(page);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long currentPage = pictureQueryRequest.getCurrentPage();
        long pageSize = pictureQueryRequest.getPageSize();
        // 空间权限校验

        Long spaceId = pictureQueryRequest.getSpaceId();
        // 普通用户默认只能查看已经过审的数据(仅针对公共空间)
        if (spaceId == null) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        } else {
            // 私有空间，校验权限
            User loginUser = userService.getLoginUser(request);
            spaceService.checkSpaceAuth(loginUser, spaceId);
        }
        // 普通用户也不会获得审核信息字段
        pictureQueryRequest.setReviewerId(null);
        pictureQueryRequest.setReviewMessage(null);

        Page<Picture> page = pictureService.page(
                new Page<>(currentPage, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest)
        );
        return ResultUtils.success(pictureService.getPictureVOPage(page));
    }

    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.ThrowIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<PictureVO> pictureVOPage = pictureService.listPictureVOByPage(pictureQueryRequest);
        return ResultUtils.success(pictureVOPage);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.ThrowIf(pictureEditRequest == null, ErrorCode.PARAM_BLANK);
        ThrowUtils.ThrowIf(pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.ThrowIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.ThrowIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer uploaded = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploaded);
    }
}
