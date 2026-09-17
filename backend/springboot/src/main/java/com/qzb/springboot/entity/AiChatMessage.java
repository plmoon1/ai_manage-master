package com.qzb.springboot.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * AI助手会话消息
 */
@Data
public class AiChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键UUID */
    private String id;
    /** 会话ID */
    private String sid;
    /** 角色 user/assistant */
    private String role;
    /** 消息内容 */
    private String content;
    /** 本轮涉及条目JSON(assistant消息)：前端据此把任务名渲染成跳转链接 */
    private String tasks;
    /** 本轮选项按钮JSON(assistant消息)：历史恢复后胶囊按钮仍可点 */
    private String options;
    /** 创建时间 */
    private String createTime;
}
