package com.qzb.springboot.service;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.qzb.springboot.entity.SysTemplate;
import com.qzb.springboot.mapper.SysTemplateMapper;
import com.qzb.springboot.service.SysTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class SysTemplateService {
    @Resource
    private SysTemplateMapper sysTemplateMapper;

    /** 从配置文件获取文件上传根路径 */
    @Value("${template.path}")
    private String UPLOAD_ROOT_PATH;

    /** 允许的文件类型：仅Excel(xlsx) */
    private static final String ALLOWED_FILE_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /** 固定的两个模板名（前后端保持一致） */
    public static final String TASK_TEMPLATE = "任务数据导入模板.xlsx";
    public static final String USER_TEMPLATE = "用户数据导入模板.xlsx";
    public static final String WORKS_TEMPLATE = "工作数据导入模板.xlsx";
    public static final String MEETING_TEMPLATE = "会议数据导入模板.xlsx";


    public Map<String, SysTemplate> getTemplateMapByTenantId(String tenantId) {
        List<SysTemplate> templateList = sysTemplateMapper.selectByTenantId(tenantId);
        Map<String, SysTemplate> templateMap = new HashMap<>();
        for (SysTemplate template : templateList) {
            templateMap.put(template.getOriginalName(), template);
        }
        return templateMap;
    }


    @Transactional(rollbackFor = Exception.class)
    public void uploadTemplate(String tenantId, String originalName, MultipartFile file) {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        if (!ALLOWED_FILE_TYPE.equals(file.getContentType())) {
            throw new RuntimeException("仅支持上传xlsx格式的Excel文件");
        }
        // 校验是否为指定的模板名
        if (!originalName.equals(TASK_TEMPLATE)
                && !originalName.equals(USER_TEMPLATE)
                && !originalName.equals(WORKS_TEMPLATE)
                && !originalName.equals(MEETING_TEMPLATE)) {
            throw new RuntimeException("仅允许上传指定的模板文件：" + TASK_TEMPLATE + "、" + USER_TEMPLATE + "、" + WORKS_TEMPLATE + "、" + MEETING_TEMPLATE);
        }

        try {
            // 2. 拼接存储文件名（租户ID-原始名）
            String storageName = tenantId + "-" + originalName;
            // 3. 创建存储目录（不存在则自动创建）
            File uploadDir = new File(UPLOAD_ROOT_PATH);
            if (!uploadDir.exists()) {
                FileUtil.mkdir(uploadDir);
            }
            // 4. 存储文件的绝对路径
            String filePath = UPLOAD_ROOT_PATH + storageName;
            File newFile = new File(filePath);

            // 5. 查询当前模板是否存在
            SysTemplate existTemplate = sysTemplateMapper.selectByTenantAndOriginal(tenantId, originalName);
            if (existTemplate != null) {
                // 存在则删除旧文件
                File oldFile = new File(existTemplate.getFilePath());
                if (oldFile.exists()) {
                    FileUtil.del(oldFile);
                    log.info("删除旧模板文件：{}", oldFile.getAbsolutePath());
                }
                // 更新数据库记录
                SysTemplate updateTemplate = new SysTemplate();
                updateTemplate.setTenantId(tenantId);
                updateTemplate.setOriginalName(originalName);
                updateTemplate.setStorageName(storageName);
                updateTemplate.setFilePath(filePath);
                sysTemplateMapper.updateByTenantAndOriginal(updateTemplate);
                log.info("更新模板记录：租户{}，模板{}", tenantId, originalName);
            } else {
                // 不存在则插入新记录
                SysTemplate newTemplate = new SysTemplate();
                newTemplate.setTenantId(tenantId);
                newTemplate.setOriginalName(originalName);
                newTemplate.setStorageName(storageName);
                newTemplate.setFilePath(filePath);
                sysTemplateMapper.insert(newTemplate);
                log.info("新增模板记录：租户{}，模板{}", tenantId, originalName);
            }

            // 6. 保存新文件到本地
            file.transferTo(newFile);
            log.info("模板文件上传成功：{}", newFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("模板上传失败", e);
            throw new RuntimeException("模板上传失败：" + e.getMessage());
        }
    }


    public void downloadTemplate(String tenantId, String originalName, HttpServletResponse response) {
        // 1. 查询模板信息
        SysTemplate template = sysTemplateMapper.selectByTenantAndOriginal(tenantId, originalName);
        if (template == null) {
            throw new RuntimeException("该模板未上传，无法下载");
        }

        File file = new File(template.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("模板文件已丢失，无法下载");
        }

        try {
            // 2. 设置响应头（下载文件）
            response.setContentType(ALLOWED_FILE_TYPE);
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(originalName, "UTF-8"));
            response.setHeader("Cache-Control", "no-cache");

            // 3. 写入文件流到响应
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = response.getOutputStream();
            IoUtil.copy(fis, os);
            IoUtil.close(fis);
            IoUtil.close(os);
            log.info("模板下载成功：租户{}，模板{}", tenantId, originalName);
        } catch (Exception e) {
            log.error("模板下载失败", e);
            throw new RuntimeException("模板下载失败：" + e.getMessage());
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String tenantId, String originalName) {
        // 1. 查询模板信息
        SysTemplate template = sysTemplateMapper.selectByTenantAndOriginal(tenantId, originalName);
        if (template == null) {
            throw new RuntimeException("该模板未上传，无需删除");
        }

        try {
            // 2. 删除本地文件
            File file = new File(template.getFilePath());
            if (file.exists()) {
                FileUtil.del(file);
                log.info("删除模板文件：{}", file.getAbsolutePath());
            }
            // 3. 删除数据库记录
            sysTemplateMapper.deleteByTenantAndOriginal(tenantId, originalName);
            log.info("删除模板记录：租户{}，模板{}", tenantId, originalName);
        } catch (Exception e) {
            log.error("模板删除失败", e);
            throw new RuntimeException("模板删除失败：" + e.getMessage());
        }
    }
}
