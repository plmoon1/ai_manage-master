package com.qzb.springboot.mapper;
import com.qzb.springboot.entity.ScreenConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 大屏参数配置Mapper
 */
public interface ScreenConfigMapper {
    /**
     * 新增配置
     */
    int insert(ScreenConfig screenConfig);

    /**
     * 修改配置
     */
    int updateById(ScreenConfig screenConfig);

    /**
     * 删除配置（物理删除，也可改为逻辑删除）
     */
    int deleteById(String id);

    /**
     * 按ID查询配置
     */
    ScreenConfig selectById(String id);

    /**
     * 按租户ID查询所有配置
     */
    List<ScreenConfig> selectByTid(String tid);

    /**
     * 按租户ID查询启用的配置（大屏读取用）
     */
    ScreenConfig selectEnabledByTid(@Param("tid") String tid);

    Integer checkParamMutilOn(String tid, String configName);

    ScreenConfig getConfigByTidAndConfigName(String tid, String configName);

}
