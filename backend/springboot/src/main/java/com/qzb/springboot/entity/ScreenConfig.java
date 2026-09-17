package com.qzb.springboot.entity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 大屏参数配置实体
 */
@Data
public class ScreenConfig {
    /**
     * 主键ID
     */
    private String id;

    /**
     * 租户ID
     */
    private String tid;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * JSON格式的大屏参数
     */
    private String configJson;

    /**
     * 是否启用（1=启用，0=禁用）
     */
    private Integer isEnabled;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}