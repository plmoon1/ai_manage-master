package com.qzb.springboot.controller;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qzb.springboot.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileNginxController {

    private static final Logger log = LoggerFactory.getLogger(FileNginxController.class);

    // 从配置文件注入文件存储绝对路径
    @Value("${file.upload.path}")
    private String uploadAbsolutePath;

    // 从配置文件注入前端访问前缀
    @Value("${file.upload.access-path}")
    private String fileAccessPrefix;

    /**
     * 文件上传（适配新路径）
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        // 校验文件是否为空
        if (file.isEmpty()) {
            log.error("上传失败：文件为空");
            return Result.error();
        }

        String originalFileName = file.getOriginalFilename();
        // 生成唯一文件名（UUID+时间戳，避免重复）
        String uniqueFileName = UUID.randomUUID() + "-" + System.currentTimeMillis() + "-" + originalFileName;
        String fullSavePath = uploadAbsolutePath + uniqueFileName;

        try {
            // 确保存储目录存在（自动创建多级目录）
            if (!FileUtil.isDirectory(uploadAbsolutePath)) {
                FileUtil.mkdir(uploadAbsolutePath);
            }
            // 写入文件到指定路径
            FileUtil.writeBytes(file.getBytes(), fullSavePath);
            log.info("文件上传成功：{}", originalFileName);

            // 返回前端访问路径（如：/resource/task_manage/xxx-xxx.xlsx）
            String accessUrl = fileAccessPrefix + uniqueFileName;
            return Result.success(accessUrl);

        } catch (Exception e) {
            log.error("文件上传失败：{}，原因：{}", originalFileName, e.getMessage(), e);
            return Result.error();
        }
    }

    /**
     * 文件下载（适配新路径）
     */
    @GetMapping("/download")
    public void download(@RequestParam String file, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        // 处理文件名（兼容 /resource/task_manage/xxx.xlsx 或 xxx.xlsx 入参）
        String fileName = file.contains("/") ? file.substring(file.lastIndexOf("/") + 1) : file;
        String fullFilePath = uploadAbsolutePath + fileName;
        File targetFile = new File(fullFilePath);

        InputStream is = null;
        OutputStream os = null;

        try {
            // 校验文件是否存在
            if (!targetFile.exists() || !targetFile.isFile()) {
                log.error("下载失败：文件不存在，路径：{}", fullFilePath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在：" + fileName);
                return;
            }

            // 完善下载响应头（避免中文乱码）
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            response.setContentType("application/octet-stream");
            // 文件名编码（兼容所有浏览器）
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(targetFile.length());

            // 分块读写（避免大文件占用内存）
            is = new BufferedInputStream(new FileInputStream(targetFile));
            os = new BufferedOutputStream(response.getOutputStream());
            FileCopyUtils.copy(is, os);
            os.flush();
            log.info("文件下载成功：{}", fileName);

        } catch (Exception e) {
            log.error("文件下载失败：{}，原因：{}", fileName, e.getMessage(), e);
            try {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("下载失败：" + e.getMessage());
            } catch (IOException ex) {
                log.error("响应错误信息写入失败", ex);
            }
        } finally {
            // 关闭流，释放资源
            try {
                if (os != null) os.close();
                if (is != null) is.close();
            } catch (IOException e) {
                log.error("流关闭失败", e);
            }
        }
    }

    /**
     * 文件删除（适配新路径）
     */
    @DeleteMapping("/delete")
    public Result delFile(@RequestParam String file) {
        // 处理文件名（兼容 /resource/task_manage/xxx.xlsx 或 xxx.xlsx 入参）
        String fileName = file.contains("/") ? file.substring(file.lastIndexOf("/") + 1) : file;
        String fullFilePath = uploadAbsolutePath + fileName;
        File targetFile = new File(fullFilePath);

        try {
            // 校验文件是否存在
            if (!targetFile.exists() || !targetFile.isFile()) {
                log.error("删除失败：文件不存在，路径：{}", fullFilePath);
                return Result.error();
            }

            // 删除文件
            boolean deleteSuccess = FileUtil.del(fullFilePath);
            if (deleteSuccess) {
                log.info("文件删除成功：{}", fileName);
                return Result.success("删除成功");
            } else {
                log.error("删除失败：文件无法删除，路径：{}", fullFilePath);
                return Result.error();
            }

        } catch (Exception e) {
            log.error("删除失败：{}，原因：{}", fileName, e.getMessage(), e);
            return Result.error();
        }
    }


    /**
     * 获取文件
     *
     * @param flag
     * @param response
     */
    @GetMapping("/images/{flag}")   //  1697438073596-avatar.png
    public void avatarPath(@PathVariable String flag, HttpServletResponse response) {
        OutputStream os;
        try {
            if (StrUtil.isNotEmpty(flag)) {
                response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(flag, "UTF-8"));
                response.setContentType("application/octet-stream");
                byte[] bytes = FileUtil.readBytes(uploadAbsolutePath + flag);
                os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
                log.info("获取资源文件：" + flag);
            }
        } catch (Exception e) {
            System.out.println("文件下载失败");
        }
    }

}