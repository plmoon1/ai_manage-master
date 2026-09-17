package com.qzb.springboot.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysTemplate {
    /** 主键ID */
    private Long id;
    /** 租户ID */
    private String tenantId;
    /** 模板原始名称 */
    private String originalName;
    /** 存储文件名（租户ID-原始名称） */
    private String storageName;
    /** 文件存储绝对路径 */
    private String filePath;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
