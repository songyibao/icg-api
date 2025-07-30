package com.yb.icgapi.controller;


import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.common.ResultUtils;
import com.yb.icgapi.model.entity.AiDetectedFace;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.AiDetectedFaceVO;
import com.yb.icgapi.model.vo.AiPersonClusterVO;
import com.yb.icgapi.service.AiPersonClusterService;
import com.yb.icgapi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ai/face")
public class AiDetectedFaceController {

    @Resource
    private UserService userService;
    @Resource
    private AiPersonClusterService aiPersonClusterService;


}
