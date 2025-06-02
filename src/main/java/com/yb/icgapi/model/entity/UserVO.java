package com.yb.icgapi.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;

import java.io.Serializable;

public class UserVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
