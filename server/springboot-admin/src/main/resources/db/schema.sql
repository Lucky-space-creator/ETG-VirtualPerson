-- ============================================================
-- VirtualWife 管理员后台系统 - 数据库初始化脚本
-- 数据库：virtualwife_admin
-- 表数量：13张
-- 编码：utf8mb4 / utf8mb4_unicode_ci
-- 导入命令：mysql -u root -p --default-character-set=utf8mb4 < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `virtualwife_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `virtualwife_admin`;

-- ============================================================
-- 1. sys_user
-- ============================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色 ADMIN/USER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================================
-- 2. chat_record
-- ============================================================
DROP TABLE IF EXISTS `chat_record`;
CREATE TABLE `chat_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '关联用户ID',
    `session_id` VARCHAR(50) NOT NULL COMMENT '会话ID',
    `avatar_name` VARCHAR(100) DEFAULT NULL COMMENT '数字人角色',
    `message_type` VARCHAR(10) NOT NULL COMMENT '消息类型 user/ai',
    `content` TEXT COMMENT '消息内容',
    `emotion` VARCHAR(20) DEFAULT NULL COMMENT '情感标签',
    `token_count` INT DEFAULT 0 COMMENT 'Token消耗',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天记录表';

-- ============================================================
-- 3. knowledge_base
-- ============================================================
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `embedding_model` VARCHAR(50) DEFAULT NULL COMMENT '嵌入模型名称',
    `vector_db_type` VARCHAR(20) DEFAULT NULL COMMENT '向量数据库类型 Milvus',
    `item_count` INT DEFAULT 0 COMMENT '条目数量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_name` (`kb_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ============================================================
-- 4. knowledge_item
-- ============================================================
DROP TABLE IF EXISTS `knowledge_item`;
CREATE TABLE `knowledge_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '条目标题',
    `content` TEXT COMMENT '条目内容',
    `vector_id` VARCHAR(100) DEFAULT NULL COMMENT 'Milvus向量ID',
    `vector_status` TINYINT DEFAULT 0 COMMENT '向量化状态 0=未处理 1=已处理',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识条目表';

-- ============================================================
-- 5. avatar_config
-- ============================================================
DROP TABLE IF EXISTS `avatar_config`;
CREATE TABLE `avatar_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `avatar_name` VARCHAR(100) NOT NULL COMMENT '形象名称',
    `vrm_model_url` VARCHAR(255) DEFAULT NULL COMMENT 'VRM模型URL',
    `thumbnail_url` VARCHAR(255) DEFAULT NULL COMMENT '缩略图URL',
    `persona` TEXT COMMENT '角色设定 System Prompt',
    `personality` TEXT COMMENT '性格描述',
    `voice_type` VARCHAR(50) DEFAULT NULL COMMENT 'TTS语音类型',
    `emotion_config` JSON DEFAULT NULL COMMENT '情感配置',
    `is_system` TINYINT DEFAULT 0 COMMENT '是否系统内置 0=否 1=是',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_avatar_name` (`avatar_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字人形象表';

-- ============================================================
-- 6. llm_config
-- ============================================================
DROP TABLE IF EXISTS `llm_config`;
CREATE TABLE `llm_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `provider` VARCHAR(30) NOT NULL COMMENT 'OpenAI/Ollama/ZhipuAI/Qwen-VL-Chat/GLM-4V',
    `api_url` VARCHAR(255) DEFAULT NULL COMMENT 'API地址',
    `api_key` VARCHAR(255) DEFAULT NULL COMMENT 'API Key AES加密存储',
    `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
    `temperature` DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度',
    `max_tokens` INT DEFAULT 2048 COMMENT '最大Token',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认配置 0=否 1=是',
    `connect_status` TINYINT DEFAULT 0 COMMENT '连通状态 0=未知 1=正常 2=失败',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='大模型配置表';

-- ============================================================
-- 7. statistics_daily
-- ============================================================
DROP TABLE IF EXISTS `statistics_daily`;
CREATE TABLE `statistics_daily` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `total_users` INT DEFAULT 0 COMMENT '总用户数',
    `new_users` INT DEFAULT 0 COMMENT '新增用户数',
    `active_users` INT DEFAULT 0 COMMENT '活跃用户数',
    `total_messages` INT DEFAULT 0 COMMENT '总消息数',
    `ai_messages` INT DEFAULT 0 COMMENT 'AI消息数',
    `user_messages` INT DEFAULT 0 COMMENT '用户消息数',
    `avg_session_count` DECIMAL(5,2) DEFAULT 0.00 COMMENT '平均会话数',
    `total_tokens` BIGINT DEFAULT 0 COMMENT 'Token消耗',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统计日报表';

-- ============================================================
-- 8. rag_document
-- ============================================================
DROP TABLE IF EXISTS `rag_document`;
CREATE TABLE `rag_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_id` BIGINT DEFAULT NULL COMMENT '所属知识库ID',
    `doc_name` VARCHAR(200) NOT NULL COMMENT '文档名称',
    `doc_type` VARCHAR(20) NOT NULL COMMENT '文档类型 PDF/DOCX/TXT/MD/HTML',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小 字节',
    `sha256` VARCHAR(64) DEFAULT NULL COMMENT 'SHA-256去重哈希',
    `page_count` INT DEFAULT 0 COMMENT '总页数 PDF专用',
    `chunk_count` INT DEFAULT 0 COMMENT '切割块数',
    `chunk_strategy` VARCHAR(30) DEFAULT NULL COMMENT '切割策略',
    `chunk_size` INT DEFAULT 512 COMMENT '切割块大小(字符)',
    `chunk_overlap` INT DEFAULT 50 COMMENT '切割重叠(字符)',
    `process_status` TINYINT DEFAULT 0 COMMENT '处理状态 0=待处理 1=解析中 2=切割中 3=向量化中 4=已完成 -1=失败',
    `progress_percent` INT DEFAULT 0 COMMENT '处理进度百分比 0-100',
    `parsed_text` MEDIUMTEXT COMMENT '解析后的纯文本全文',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_process_status` (`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG文档表';

-- ============================================================
-- 9. rag_chunk
-- ============================================================
DROP TABLE IF EXISTS `rag_chunk`;
CREATE TABLE `rag_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `doc_id` BIGINT NOT NULL COMMENT '所属文档ID',
    `kb_id` BIGINT DEFAULT NULL COMMENT '所属知识库ID',
    `chunk_index` INT NOT NULL COMMENT '块序号',
    `content` TEXT COMMENT '块原始文本内容',
    `enhanced_content` TEXT COMMENT '上下文注入后文本 含标题+章节路径+页码',
    `summary` VARCHAR(512) DEFAULT NULL COMMENT 'AI摘要 不超过512字',
    `entities` JSON DEFAULT NULL COMMENT '实体列表 NER抽取',
    `relations` JSON DEFAULT NULL COMMENT '关系列表 知识图谱',
    `section_path` VARCHAR(500) DEFAULT NULL COMMENT '章节路径',
    `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '内容哈希',
    `token_count` INT DEFAULT 0 COMMENT 'Token数',
    `vector_id` VARCHAR(100) DEFAULT NULL COMMENT 'Milvus向量ID',
    `vector_status` TINYINT DEFAULT 0 COMMENT '向量化状态',
    `page_num` INT DEFAULT NULL COMMENT '源文档页码',
    `metadata_json` JSON DEFAULT NULL COMMENT '元数据 标题等',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_doc_id` (`doc_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_vector_status` (`vector_status`),
    FULLTEXT KEY `ft_content` (`content`, `enhanced_content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG文档块表';

-- ============================================================
-- 10. rag_evaluation
-- ============================================================
DROP TABLE IF EXISTS `rag_evaluation`;
CREATE TABLE `rag_evaluation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_id` BIGINT DEFAULT NULL COMMENT '所属知识库ID',
    `eval_type` VARCHAR(20) NOT NULL COMMENT '评测类型 AUTO/MANUAL',
    `recall_at_5` DECIMAL(5,4) DEFAULT NULL COMMENT 'Recall@5',
    `recall_at_10` DECIMAL(5,4) DEFAULT NULL COMMENT 'Recall@10',
    `mrr` DECIMAL(5,4) DEFAULT NULL COMMENT '平均倒数排名',
    `ndcg_at_5` DECIMAL(5,4) DEFAULT NULL COMMENT 'NDCG@5',
    `hit_at_5` DECIMAL(5,4) DEFAULT NULL COMMENT 'Hit@5',
    `avg_faithfulness` DECIMAL(5,4) DEFAULT NULL COMMENT '忠实度',
    `avg_relevance` DECIMAL(5,4) DEFAULT NULL COMMENT '相关性',
    `config_snapshot` JSON DEFAULT NULL COMMENT '配置快照',
    `detail_json` JSON DEFAULT NULL COMMENT '逐题详细评分',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG评测表';

-- ============================================================
-- 11. rag_qa_pair
-- ============================================================
DROP TABLE IF EXISTS `rag_qa_pair`;
CREATE TABLE `rag_qa_pair` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_id` BIGINT DEFAULT NULL COMMENT '所属知识库ID',
    `question` VARCHAR(500) NOT NULL COMMENT '评测问题',
    `expected_doc_ids` JSON DEFAULT NULL COMMENT '期望命中文档',
    `expected_chunk_ids` JSON DEFAULT NULL COMMENT '期望命中块',
    `difficulty` TINYINT DEFAULT 1 COMMENT '难度等级',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '问题分类',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评测问答对表';

-- ============================================================
-- 12. route
-- ============================================================
DROP TABLE IF EXISTS `route`;
CREATE TABLE `route` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_id` BIGINT DEFAULT NULL COMMENT '所属知识库ID',
    `route_name` VARCHAR(200) NOT NULL COMMENT '路线名称',
    `interest_tags` JSON DEFAULT NULL COMMENT '兴趣标签 历史文化/自然风光/美食/拍照打卡',
    `time_budget` INT DEFAULT 120 COMMENT '推荐时间预算 分钟',
    `energy_level` TINYINT DEFAULT 1 COMMENT '体力等级 1=轻松 2=适中 3=挑战',
    `description` TEXT COMMENT '路线描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游览路线表';

-- ============================================================
-- 14. report_cache
-- ============================================================
CREATE TABLE IF NOT EXISTS `report_cache` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `report_type` VARCHAR(50) NOT NULL COMMENT '报告类型',
    `content` TEXT COMMENT '报告内容JSON',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_type` (`report_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告缓存表';

-- ============================================================
-- 13. spot
-- ============================================================
DROP TABLE IF EXISTS `spot`;
CREATE TABLE `spot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `route_id` BIGINT NOT NULL COMMENT '所属路线ID',
    `spot_name` VARCHAR(200) NOT NULL COMMENT '景点名称',
    `latitude` DECIMAL(10,7) DEFAULT NULL COMMENT 'GPS纬度',
    `longitude` DECIMAL(10,7) DEFAULT NULL COMMENT 'GPS经度',
    `geo_radius` INT DEFAULT 100 COMMENT '电子围栏半径 米',
    `narrate_text` TEXT COMMENT '讲解词内容',
    `spot_order` INT DEFAULT 0 COMMENT '路线中顺序',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_route_id` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景点表';

-- ============================================================
-- 15. scenic_spot 景区表
-- ============================================================
CREATE TABLE IF NOT EXISTS `scenic_spot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spot_name` VARCHAR(100) NOT NULL COMMENT '景区名称',
    `spot_code` VARCHAR(50) NOT NULL COMMENT '景区编码',
    `description` TEXT COMMENT '景区描述',
    `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_spot_code` (`spot_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景区表';

-- 默认景区
INSERT IGNORE INTO `scenic_spot` (`spot_name`, `spot_code`, `description`) VALUES
('灵山胜境', 'lingshan', '无锡灵山大佛景区');

-- 添加景区关联字段
ALTER TABLE `avatar_config` ADD COLUMN IF NOT EXISTS `scenic_spot_id` BIGINT DEFAULT NULL COMMENT '所属景区ID' AFTER `id`;
ALTER TABLE `route` ADD COLUMN IF NOT EXISTS `scenic_spot_id` BIGINT DEFAULT NULL COMMENT '所属景区ID' AFTER `id`;
ALTER TABLE `knowledge_base` ADD COLUMN IF NOT EXISTS `scenic_spot_id` BIGINT DEFAULT NULL COMMENT '所属景区ID' AFTER `id`;
ALTER TABLE `chat_record` ADD COLUMN IF NOT EXISTS `scenic_spot_id` BIGINT DEFAULT NULL COMMENT '所属景区ID' AFTER `user_id`;
ALTER TABLE `statistics_daily` ADD COLUMN IF NOT EXISTS `scenic_spot_id` BIGINT DEFAULT NULL COMMENT '所属景区ID' AFTER `stat_date`;
ALTER TABLE `tourist_consumption` ADD COLUMN IF NOT EXISTS `scenic_spot_id` BIGINT DEFAULT NULL COMMENT '所属景区ID' AFTER `id`;

-- 默认数据关联到灵山胜境
UPDATE `avatar_config` SET `scenic_spot_id` = 1 WHERE `scenic_spot_id` IS NULL;
UPDATE `route` SET `scenic_spot_id` = 1 WHERE `scenic_spot_id` IS NULL;
UPDATE `knowledge_base` SET `scenic_spot_id` = 1 WHERE `scenic_spot_id` IS NULL;

-- 添加索引
ALTER TABLE `avatar_config` ADD INDEX IF NOT EXISTS `idx_scenic_spot_id` (`scenic_spot_id`);
ALTER TABLE `route` ADD INDEX IF NOT EXISTS `idx_scenic_spot_id` (`scenic_spot_id`);
ALTER TABLE `knowledge_base` ADD INDEX IF NOT EXISTS `idx_scenic_spot_id` (`scenic_spot_id`);
ALTER TABLE `chat_record` ADD INDEX IF NOT EXISTS `idx_scenic_spot_id` (`scenic_spot_id`);
ALTER TABLE `statistics_daily` ADD INDEX IF NOT EXISTS `idx_scenic_spot_id` (`scenic_spot_id`);
