package com.yb.icgapi.constant;

public interface PictureConstant {
    // 图片URL限长
    int PICTURE_URL_MAX_LENGTH = 1024;
    // 图片简介限长
    int PICTURE_INTRODUCTION_MAX_LENGTH = 512;
    // 图片大小限制
    long PICTURE_SIZE_MAX = 2 * 1024 * 1024; // 10MB
    // 超过此大小的图片将生成缩略图
    long THUMBNAIL_SIZE_MAX = 100 * 1024; // 100KB
    // 缩略图最大宽度
    int THUMBNAIL_MAX_WIDTH = 128;
    // 缩略图最大高度
    int THUMBNAIL_MAX_HEIGHT = 128;
}
