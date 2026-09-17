package com.qzb.springboot.controller;

import com.qzb.springboot.entity.SysTemplate;
import com.qzb.springboot.service.SysTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/template")
public class SysTemplateController {
    @Resource
    private SysTemplateService sysTemplateService;

    /**
     * 查询当前租户的所有模板
     * @param tenantId 租户ID
     * @return 模板Map
     */
    @GetMapping("/list")
    public Map<String, Object> getTemplateList(@RequestParam String tenantId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, SysTemplate> templateMap = sysTemplateService.getTemplateMapByTenantId(tenantId);
            result.put("code", 200);
            result.put("msg", "查询成功");
            result.put("data", templateMap);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "查询失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 上传/更新模板
     * @param tenantId 租户ID
     * @param originalName 原始模板名
     * @param file 上传的文件
     * @return 结果
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadTemplate(
            @RequestParam String tenantId,
            @RequestParam String originalName,
            @RequestParam MultipartFile file
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            sysTemplateService.uploadTemplate(tenantId, originalName, file);
            result.put("code", 200);
            result.put("msg", "模板上传成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    /**
     * 下载模板
     * @param tenantId 租户ID
     * @param originalName 原始模板名
     * @param response 响应对象
     */
    @GetMapping("/download")
    public void downloadTemplate(
            @RequestParam String tenantId,
            @RequestParam String originalName,
            HttpServletResponse response
    ) {
        try {
            sysTemplateService.downloadTemplate(tenantId, originalName, response);
        } catch (Exception e) {
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("下载失败，响应写入异常", ex);
            }
        }
    }

    /**
     * 删除模板
     * @param tenantId 租户ID
     * @param originalName 原始模板名
     * @return 结果
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteTemplate(
            @RequestParam String tenantId,
            @RequestParam String originalName
    ) {
        Map<String, Object> result = new HashMap<>();
        try {
            sysTemplateService.deleteTemplate(tenantId, originalName);
            result.put("code", 200);
            result.put("msg", "模板删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", e.getMessage());
        }
        return result;
    }
}