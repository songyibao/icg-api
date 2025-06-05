package com.yb.icgapi.common;

import lombok.Data;

@Data
public class DeleteRequest {
    private static final long serialVersionUID = 1L;

    /**
     * 删除的ID
     */
    private Long id;
}
