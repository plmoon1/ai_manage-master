package com.qzb.springboot.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * AI助手会话
 */
@Data
public class AiChatSession implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键UUID */
    private String id;
    /** 用户ID */
    private String uid;
    /** 会话标题(取首问前20字) */
    private String title;
    /** 是否删除 0否 1是 */
    private String isdelete;
    /** 创建时间 */
    private String createTime;
    /** 最近活跃时间 */
    private String updateTime;
}
