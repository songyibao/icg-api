package com.yb.icgapi.icpic.interfaces.assembler;

import cn.hutool.json.JSONUtil;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.icpic.interfaces.dto.picture.PictureUpdateRequest;
import org.springframework.beans.BeanUtils;

public class PictureAssembler {
    public static Picture toPictureEntity(PictureUpdateRequest pictureUpdateRequest) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将tags转string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        return picture;
    }
}
