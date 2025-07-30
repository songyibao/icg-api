package com.yb.icgapi.model.dto.ai;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 批量重新处理消息
 *
 * @author songyibao
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatchReprocessMessage extends BaseAIMessage {

    /**
     * 消息数据
     */
    private BatchReprocessData data;

    public BatchReprocessMessage() {
        super.setType("batch_reprocess");
    }

    @Data
    public static class BatchReprocessData {
        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 空间ID（可选）
         */
        private Long spaceId;

        /**
         * 处理选项
         */
        private ProcessingOptions options;
    }

    @Data
    public static class ProcessingOptions {
        /**
         * 是否包含OCR处理
         */
        private Boolean includeOCR = true;

        /**
         * 是否包含CLIP处理
         */
        private Boolean includeCLIP = true;

        /**
         * 是否包含人脸识别
         */
        private Boolean includeFaces = true;

        /**
         * 批处理大小
         */
        private Integer batchSize = 10;
    }
}
