package com.yb.icgapi.model.dto.ai;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单图处理消息
 *
 * @author songyibao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SingleProcessMessage extends BaseAIMessage {

    /**
     * 消息数据
     */
    private SingleProcessData data;

    public SingleProcessMessage() {
        super.setType("single_process");
    }

    @Data
    public static class SingleProcessData {
        /**
         * 图片ID
         */
        private Long pictureId;

        /**
         * 是否强制重新处理
         */
        private Boolean forceReprocess = false;
    }
}
