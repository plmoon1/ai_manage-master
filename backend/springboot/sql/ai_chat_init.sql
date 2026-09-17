-- AI助手历史对话表（后端持久化方案）
-- 幂等脚本，可重复执行

CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键UUID',
  `uid` VARCHAR(64) NOT NULL COMMENT '用户ID(plan.uid同源)',
  `title` VARCHAR(100) DEFAULT '' COMMENT '会话标题(取首问前20字)',
  `isdelete` CHAR(1) DEFAULT '0' COMMENT '是否删除 0否 1是(软删)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近活跃时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid_update` (`uid`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手会话';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` VARCHAR(64) NOT NULL COMMENT '主键UUID',
  `sid` VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色 user/assistant',
  `content` TEXT COMMENT '消息内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `seq` BIGINT NOT NULL AUTO_INCREMENT COMMENT '插入序号(保证同轮问答顺序)',
  PRIMARY KEY (`id`),
  KEY `idx_sid_ctime` (`sid`, `create_time`),
  KEY `idx_seq` (`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助手会话消息';
