package com.yb.icgapi.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 空间等级
     */
    private int value;

    /**
     * 空间等级名称
     */
    private String text;

    /**
     * 空间图片数量最大值（单位：张）
     */
    private long maxCount;

    /**
     * 空间最大容量（单位：字节）
     */
    private long maxSize;
}
