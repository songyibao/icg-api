ALTER TABLE picture ADD COLUMN ai_status JSON DEFAULT NULL;
-- ----------------------------
-- 4. AI OCR 识别结果表 (AI 功能)
-- ----------------------------
DROP TABLE IF EXISTS `ai_ocr_result`;
CREATE TABLE `ai_ocr_result`
(
    `id`        bigint(20)                               NOT NULL AUTO_INCREMENT,
    `pictureId` bigint(20)                               NOT NULL COMMENT '关联的图片 id',
    `text`      varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '识别到的单行或单块文本',
    `score`     double DEFAULT NULL COMMENT '该文本的置信度分数',
    `box`       json   DEFAULT NULL COMMENT '文本位置框 (x, y, width, height)',
    PRIMARY KEY (`id`),
    KEY `idx_pictureId` (`pictureId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='【AI】OCR 识别结果表 (精确)';

-- ----------------------------
-- 5. AI OCR 全文检索表 (AI 功能)
-- ----------------------------
DROP TABLE IF EXISTS `ai_ocr_fulltext`;
CREATE TABLE `ai_ocr_fulltext`
(
    `pictureId` bigint(20) NOT NULL COMMENT '关联的图片 id',
    `content`   longtext COLLATE utf8mb4_unicode_ci,
    PRIMARY KEY (`pictureId`),
    FULLTEXT KEY `ft_content` (`content`) /*!50100 WITH PARSER `ngram` */
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='【AI】OCR 全文检索专用表';

-- ----------------------------
-- 6. 【新增】AI 人物簇表 (人脸聚类的核心)
-- ----------------------------
DROP TABLE IF EXISTS `ai_person_cluster`;
CREATE TABLE `ai_person_cluster`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT,
    `userId`      bigint(20) NOT NULL COMMENT '所属用户 id (实现数据隔离)',
    `displayName` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '人物的显示名称 (由用户命名, 如“宝宝”,"妈妈")',
    `coverFaceId` bigint(20)                              DEFAULT NULL COMMENT '该人物簇的封面人脸ID, 关联ai_detected_face.id',
    `faceCount`   int(11)    NOT NULL                     DEFAULT '0' COMMENT '该人物簇下的人脸数量',
    `createTime`  datetime   NOT NULL                     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  datetime   NOT NULL                     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='【AI】人物簇 (人脸聚类结果)';


-- ----------------------------
-- 7. 【修改】AI 检测到的人脸表
-- ----------------------------
DROP TABLE IF EXISTS `ai_detected_face`;
CREATE TABLE `ai_detected_face`
(
    `id`              bigint(20)                      NOT NULL AUTO_INCREMENT,
    `pictureId`       bigint(20)                      NOT NULL COMMENT '关联的图片 id',
    `clusterId`       bigint(20) DEFAULT NULL COMMENT '【关键】关联到的人物簇 id (ai_person_cluster.id)',
    `facial_area`     json                            NOT NULL COMMENT '人脸区域坐标 (x, y, w, h, left_eye, right_eye)',
    `face_confidence` double     DEFAULT NULL COMMENT '人脸检测的置信度',
    `embedding`       text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人脸特征向量 (JSON数组格式, 备份用)',
    PRIMARY KEY (`id`),
    KEY `idx_pictureId` (`pictureId`),
    KEY `idx_clusterId` (`clusterId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='【AI】图片中检测到的人脸信息';
