package com.qzb.springboot.mapper;
import com.qzb.springboot.entity.SysTemplate;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface SysTemplateMapper {
    /** 根据租户ID查询所有模板 */
    List<SysTemplate> selectByTenantId(@Param("tenantId") String tenantId);

    /** 根据租户ID+原始名称查询模板 */
    SysTemplate selectByTenantAndOriginal(@Param("tenantId") String tenantId, @Param("originalName") String originalName);

    /** 插入模板 */
    int insert(SysTemplate sysTemplate);

    /** 更新模板（根据租户ID+原始名称） */
    int updateByTenantAndOriginal(SysTemplate sysTemplate);

    /** 删除模板（根据租户ID+原始名称） */
    int deleteByTenantAndOriginal(@Param("tenantId") String tenantId, @Param("originalName") String originalName);
}
