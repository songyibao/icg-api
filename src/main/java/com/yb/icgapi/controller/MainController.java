package com.yb.icgapi.controller;

import com.baomidou.mybatisplus.core.toolkit.MybatisUtils;
import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("Hello icg");
    }
}
