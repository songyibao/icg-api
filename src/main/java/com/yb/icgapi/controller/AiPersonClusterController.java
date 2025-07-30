package com.yb.icgapi.controller;


import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.common.ResultUtils;
import com.yb.icgapi.model.entity.AiDetectedFace;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.AiDetectedFaceVO;
import com.yb.icgapi.model.vo.AiPersonClusterVO;
import com.yb.icgapi.service.AiDetectedFaceService;
import com.yb.icgapi.service.AiPersonClusterService;
import com.yb.icgapi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/person_cluster")
public class AiPersonClusterController {

    @Resource
    private UserService userService;
    @Resource
    private AiPersonClusterService aiPersonClusterService;

    @Resource
    private AiDetectedFaceService aiDetectedFaceService;
//    public List<AiPersonClusterVO> getPrivatePersonCluster(User loginUser)


    @GetMapping("/list")
    public BaseResponse<List<AiPersonClusterVO>> listPersionCluster(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        List<AiPersonClusterVO> personCluster = aiPersonClusterService.getPrivatePersonCluster(loginUser);
        return ResultUtils.success(personCluster);
    }

    @GetMapping("/detected_faces")
    public BaseResponse<List<AiDetectedFaceVO>> listClusterDetectedFaces(@RequestParam("id") Long clusterId, HttpServletRequest request ) {
        User loginUser = userService.getLoginUser(request);
        // 获取指定人物簇的检测到的人脸列表
        List<AiDetectedFaceVO> detectedFaces = aiPersonClusterService.getClusterDetectedFaces(loginUser, clusterId);
        return ResultUtils.success(detectedFaces);
    }

    /**
     * 重建当前用户空间的人物索引
     * @param request
     * @return
     */
    @GetMapping("/reconstruct")
    public BaseResponse<Boolean> reconstructPersonCluster(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 重建指定人物簇
        boolean result = aiPersonClusterService.reconstructPersonCluster(loginUser);
        return ResultUtils.success(result);
    }
}
