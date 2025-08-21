package com.yb.icgapi.model.dto.space;

import com.yb.icgapi.icpic.infrastructure.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

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
     * 空间类型  0-个人空间; 1-团队空间
     */
    private Integer spaceType;

    /**
     * 创建用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 4700194792506869904L;
}
