package com.yb.icgapi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.model.entity.AiDetectedFace;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.service.AiDetectedFaceService;
import com.yb.icgapi.mapper.AiDetectedFaceMapper;
import com.yb.icgapi.service.PictureService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author songyibao
* @description 针对表【ai_detected_face(【AI】图片中检测到的人脸信息)】的数据库操作Service实现
* @createDate 2025-07-25 08:58:06
*/
@Service
public class AiDetectedFaceServiceImpl extends ServiceImpl<AiDetectedFaceMapper, AiDetectedFace>
    implements AiDetectedFaceService{



}




