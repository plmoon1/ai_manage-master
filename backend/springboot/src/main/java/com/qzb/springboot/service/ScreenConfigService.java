package com.qzb.springboot.service;

import com.qzb.springboot.entity.ScreenConfig;
import com.qzb.springboot.mapper.ScreenConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

/**
 * 大屏参数配置Service
 */
@Service
public class ScreenConfigService {

    @Resource
    private ScreenConfigMapper screenConfigMapper;

    /**
     * 新增配置
     */
    public boolean addConfig(ScreenConfig screenConfig) {
        // 参数校验
        if (screenConfig.getTid() == null || !StringUtils.hasText(screenConfig.getConfigName())
                || !StringUtils.hasText(screenConfig.getConfigJson())) {
            return false;
        }
        //参数启动校验
        if(checkParamMutilOn(screenConfig.getTid(),screenConfig.getConfigName())){
            return false;
        };
        // 默认启用
        if (screenConfig.getIsEnabled() == null) {
            screenConfig.setIsEnabled(1);
        }
        //添加id
        screenConfig.setId(UUID.randomUUID().toString());
        return screenConfigMapper.insert(screenConfig) > 0;
    }

    /**
     * 修改配置
     */
    public boolean updateConfig(ScreenConfig screenConfig) {
        if (screenConfig.getId() == null) {
            return false;
        }
        //参数启动校验
//        if(checkParamMutilOn(screenConfig.getTid(),screenConfig.getConfigName())){
//            return false;
//        };
        return screenConfigMapper.updateById(screenConfig) > 0;
    }

    /**
     * 删除配置
     */
    public boolean deleteConfig(String id) {
        if (id == null) {
            return false;
        }
        return screenConfigMapper.deleteById(id) > 0;
    }

    /**
     * 按ID查询配置
     */
    public ScreenConfig getConfigById(String id) {
        return screenConfigMapper.selectById(id);
    }

    /**
     * 按租户ID查询所有配置
     */
    public List<ScreenConfig> listConfigByTid(String tid) {
        if (tid == null) {
            return null;
        }
        return screenConfigMapper.selectByTid(tid);
    }

    /**
     * 按租户ID查询启用的配置（大屏读取）
     */
    public ScreenConfig getEnabledConfigByTid(String tid) {
        if (tid == null) {
            return null;
        }
        return screenConfigMapper.selectEnabledByTid(tid);
    }

    public Boolean checkParamMutilOn(String tid,String configName){
        return screenConfigMapper.checkParamMutilOn(tid, configName) == 1;
    }

    public ScreenConfig getConfigByTidAndConfigName(String tid, String configName) {
        return screenConfigMapper.getConfigByTidAndConfigName(tid,configName);
    }
}
