package com.yb.icgapi.controller;

import com.yb.icgapi.common.BaseResponse;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.entity.UserVO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class UserController {

    public BaseResponse<UserVO> register(User user) {

    }

}
