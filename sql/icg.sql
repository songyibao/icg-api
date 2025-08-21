# ************************************************************
# Sequel Ace SQL dump
# 版本号： 20095
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# 主机: 47.105.69.194 (MySQL 5.7.42)
# 数据库: icg
# 生成时间: 2025-08-21 06:12:52 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# 转储表 ai_batch_task
# ------------------------------------------------------------

CREATE TABLE `ai_batch_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL,
  `spaceId` bigint(20) DEFAULT NULL,
  `taskType` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `totalPictures` int(11) DEFAULT NULL,
  `processedPictures` int(11) DEFAULT NULL,
  `failedPictures` int(11) DEFAULT NULL,
  `options` json DEFAULT NULL,
  `errorMessage` text,
  `createTime` datetime NOT NULL,
  `updateTime` datetime NOT NULL,
  `completedTime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# 转储表 ai_detected_face
# ------------------------------------------------------------

CREATE TABLE `ai_detected_face` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pictureId` bigint(20) NOT NULL COMMENT '关联的图片 id',
  `clusterId` bigint(20) DEFAULT NULL COMMENT '【关键】关联到的人物簇 id (ai_person_cluster.id)',
  `area` json NOT NULL COMMENT '人脸区域坐标 (x, y, w, h, left_eye, right_eye)',
  `confidence` double DEFAULT NULL COMMENT '人脸检测的置信度',
  `embedding` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '人脸特征向量 (JSON数组格式, 备份用)',
  PRIMARY KEY (`id`),
  KEY `idx_pictureId` (`pictureId`),
  KEY `idx_clusterId` (`clusterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='【AI】图片中检测到的人脸信息';



# 转储表 ai_ocr_fulltext
# ------------------------------------------------------------

CREATE TABLE `ai_ocr_fulltext` (
  `pictureId` bigint(20) NOT NULL COMMENT '关联的图片 id',
  `content` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`pictureId`),
  FULLTEXT KEY `ft_content` (`content`) /*!50100 WITH PARSER `ngram` */ 
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='【AI】OCR 全文检索专用表';



# 转储表 ai_ocr_result
# ------------------------------------------------------------

CREATE TABLE `ai_ocr_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pictureId` bigint(20) NOT NULL COMMENT '关联的图片 id',
  `text` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '识别到的单行或单块文本',
  `score` double DEFAULT NULL COMMENT '该文本的置信度分数',
  `box` json DEFAULT NULL COMMENT '文本位置框 (x, y, width, height)',
  PRIMARY KEY (`id`),
  KEY `idx_pictureId` (`pictureId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='【AI】OCR 识别结果表 (精确)';



# 转储表 ai_person_cluster
# ------------------------------------------------------------

CREATE TABLE `ai_person_cluster` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `userId` bigint(20) NOT NULL COMMENT '所属用户 id (实现数据隔离)',
  `displayName` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '人物的显示名称 (由用户命名, 如“宝宝”,"妈妈")',
  `coverFaceId` bigint(20) DEFAULT NULL COMMENT '该人物簇的封面人脸ID, 关联ai_detected_face.id',
  `faceCount` int(11) NOT NULL DEFAULT '0' COMMENT '该人物簇下的人脸数量',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='【AI】人物簇 (人脸聚类结果)';



# 转储表 picture
# ------------------------------------------------------------

CREATE TABLE `picture` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片 url',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片名称',
  `introduction` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简介',
  `category` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类',
  `tags` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签（JSON 数组）',
  `picSize` bigint(20) DEFAULT NULL COMMENT '图片体积',
  `picWidth` int(11) DEFAULT NULL COMMENT '图片宽度',
  `picHeight` int(11) DEFAULT NULL COMMENT '图片高度',
  `picScale` double DEFAULT NULL COMMENT '图片宽高比例',
  `picFormat` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片格式',
  `userId` bigint(20) NOT NULL COMMENT '创建用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `reviewStatus` int(11) NOT NULL DEFAULT '0' COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
  `reviewMessage` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核信息',
  `reviewerId` bigint(20) DEFAULT NULL COMMENT '审核人 ID',
  `reviewTime` datetime DEFAULT NULL COMMENT '审核时间',
  `thumbnailUrl` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图地址',
  `picColor` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片主色调',
  `spaceId` bigint(20) DEFAULT NULL COMMENT '空间 id(为空表示图片属于公共空间)',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`) COMMENT '用于模糊搜索图片名称',
  KEY `idx_introduction` (`introduction`) COMMENT '用于模糊搜索图片简介',
  KEY `idx_category` (`category`) COMMENT '提升基于分类的查询性能',
  KEY `idx_tags` (`tags`) COMMENT '提升基于标签的查询性能',
  KEY `idx_userId` (`userId`) COMMENT '提升基于用户 ID 的查询性能',
  KEY `idx_reviewStatus` (`reviewStatus`) COMMENT '提升基于审核状态的查询性能',
  KEY `idx_spaceId` (`spaceId`) COMMENT '提升基于空间 ID 的查询性能'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片';



# 转储表 space
# ------------------------------------------------------------

CREATE TABLE `space` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `spaceName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '空间名称',
  `spaceLevel` int(11) DEFAULT '0' COMMENT '空间等级：0-普通版; 1-专业版; 2-旗舰版',
  `maxSize` bigint(20) DEFAULT '0' COMMENT '空间最大容量（字节）',
  `maxCount` bigint(20) DEFAULT '0' COMMENT '空间最大图片数量',
  `totalSize` bigint(20) DEFAULT '0' COMMENT '空间已使用容量（字节）',
  `totalCount` bigint(20) DEFAULT '0' COMMENT '空间已使用图片数量',
  `userId` bigint(20) NOT NULL COMMENT '创建空间的用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `spaceType` int(11) NOT NULL DEFAULT '0' COMMENT '空间类型：0-私有 1-团队',
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`) COMMENT '提升基于用户 ID 的查询性能',
  KEY `idx_spaceName` (`spaceName`) COMMENT '提升基于空间名称的查询性能',
  KEY `idx_spaceLevel` (`spaceLevel`) COMMENT '提升基于空间等级的查询性能',
  KEY `idx_spaceType` (`spaceType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间';



# 转储表 space_user
# ------------------------------------------------------------

CREATE TABLE `space_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `spaceId` bigint(20) NOT NULL COMMENT '空间 id',
  `userId` bigint(20) NOT NULL COMMENT '用户 id',
  `spaceRole` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT 'viewer' COMMENT '空间角色：viewer/editor/admin',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spaceId_userId` (`spaceId`,`userId`) COMMENT '唯一索引，用户在一个空间中只能有一个角色',
  KEY `idx_spaceId` (`spaceId`) COMMENT '提升按空间查询的性能',
  KEY `idx_userId` (`userId`) COMMENT '提升按用户查询的性能'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间用户关联';



# 转储表 user
# ------------------------------------------------------------

CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userAccount` (`userAccount`),
  KEY `idx_userName` (`userName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
