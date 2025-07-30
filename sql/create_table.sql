-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;
-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewerId    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime    DATETIME      NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
    -- 添加缩略图地址列
    ADD COLUMN thumbnailUrl VARCHAR(512) NULL COMMENT '缩略图地址';

create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(256)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间等级：0-普通版; 1-专业版; 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间最大容量（字节）',
    maxCount   bigint   default 0                 null comment '空间最大图片数量',
    totalSize  bigint   default 0                 null comment '空间已使用容量（字节）',
    totalCount bigint   default 0                 null comment '空间已使用图片数量',
    userId     bigint                             not null comment '创建空间的用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    INDEX idx_userId (userId),        -- 提升基于用户 ID 的查询性能
    INDEX idx_spaceName (spaceName),  -- 提升基于空间名称的查询性能
    INDEX idx_spaceLevel (spaceLevel) -- 提升基于空间等级的查询性能

) comment '空间' collate = utf8mb4_unicode_ci;

ALTER TABLE picture
    add column spaceId bigint null comment '空间 id(为空表示图片属于公共空间)';

-- 创建索引以提升基于 spaceId 的查询性能
CREATE INDEX idx_spaceId ON picture (spaceId);



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
    `area`     json                            NOT NULL COMMENT '人脸区域坐标 (x, y, w, h, left_eye, right_eye)',
    `confidence` double     DEFAULT NULL COMMENT '人脸检测的置信度',
    `embedding`       text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人脸特征向量 (JSON数组格式, 备份用)',
    PRIMARY KEY (`id`),
    KEY `idx_pictureId` (`pictureId`),
    KEY `idx_clusterId` (`clusterId`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='【AI】图片中检测到的人脸信息';
