package com.yb.icgapi.model.dto.space;

import com.yb.icgapi.common.PageRequest;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.vo.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 创建用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 4700194792506869904L;
}
