package com.yb.icgapi.controller;


import com.yb.icgapi.service.AiPersonClusterService;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/ai/face")
public class AiDetectedFaceController {

    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private AiPersonClusterService aiPersonClusterService;


}
