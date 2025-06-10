package com.yb.icgapi.controller;

import com.yb.icgapi.annotation.AuthCheck;
import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.common.ResultUtils;
import com.yb.icgapi.constant.UserConstant;
import com.yb.icgapi.model.dto.picture.PictureUploadRequest;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.PictureVO;
import com.yb.icgapi.service.PictureService;
import com.yb.icgapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;
    @Autowired
    private PictureService pictureService;


    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile,pictureUploadRequest,loginUser);
        return ResultUtils.success(pictureVO);
    }

}
