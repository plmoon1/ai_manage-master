package com.qzb.springboot.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.entity.PlanFiles;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.mapper.PlanMapper;
import com.qzb.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * 文件接口
 */
@RestController
@RequestMapping("/files/stop")
public class FileController {

    @Resource
    PlanMapper planMapper;
    // 文件上传存储路径
    private static final String filePath = System.getProperty("user.dir") + "/files/";

    @Value("${server.port:9090}")
    private String port;

    @Value("${ip:localhost}")
    private String ip;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        String flag;
        synchronized (FileController.class) {
            flag = System.currentTimeMillis() + "";
            ThreadUtil.sleep(1L);
        }
        String fileName = file.getOriginalFilename();
        try {
            if (!FileUtil.isDirectory(filePath)) {
                FileUtil.mkdir(filePath);
            }
            // 文件存储形式：时间戳-文件名
            FileUtil.writeBytes(file.getBytes(), filePath + flag + "-" + fileName);  // ***/manager/files/1697438073596-avatar.png
            System.out.println(fileName + "--上传成功");

        } catch (Exception e) {
            System.err.println(fileName + "--文件上传失败");
        }
        String http = "http://" + ip + ":" + port + "/files/";
        return Result.success(http + flag + "-" + fileName);  //  http://localhost:9090/files/1697438073596-avatar.png
    }


    /**
     * 获取文件
     *
     * @param file
     * @param response
     */
//    @GetMapping("/download")   //  1697438073596-avatar.png
//    public void avatarPath(@RequestParam String file, HttpServletResponse response) {
//        System.out.println(filePath);
//        // 按"/"分割字符串
//        String[] parts = file.split("/");
//
//        // 数组最后一个元素即为文件名（需确保URL格式正确）
//        String fileName = parts[parts.length - 1];
//        OutputStream os;
//        try {
//            if (StrUtil.isNotEmpty(file)) {
//                response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
//                response.setContentType("application/octet-stream");
//                byte[] bytes = FileUtil.readBytes(filePath + fileName);
//                os = response.getOutputStream();
//                os.write(bytes);
//                os.flush();
//                os.close();
//            }
//        } catch (Exception e) {
//            System.out.println("文件下载失败");
//        }
//    }
    @GetMapping("/download")
    public void download(@RequestParam String file, HttpServletResponse response) {
        // 修复1：统一响应编码和异常处理
        response.setCharacterEncoding("UTF-8");
        // 完整文件路径（根据你的实际filePath配置调整，确保路径拼接正确）
        String[] parts = file.split("/");
        String fileName = parts[parts.length - 1];
        String fullFilePath = filePath  + fileName;
        File targetFile = new File(fullFilePath);

        // 声明流变量，便于finally关闭
        InputStream is = null;
        OutputStream os = null;

        try {
            // 1. 校验文件存在性
            if (!targetFile.exists() || !targetFile.isFile()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在：" + fullFilePath);
                System.out.println("文件不存在：" + fullFilePath);
                return;
            }

            // 2. 强化下载响应头（补充缺失的关键头）
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            // 改用标准二进制流类型，兼容所有浏览器
            response.setContentType("application/octet-stream");
            // 完善文件名编码（兼容IE/Chrome/Firefox）
            String encodedFileName = URLEncoder.encode(targetFile.getName(), "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(targetFile.length());
            // 修复2：禁用连接复用，避免连接中止问题
            response.setHeader("Connection", "close");

            // 3. 缓冲流分块读写（避免一次性加载文件到内存）
            is = new BufferedInputStream(new FileInputStream(targetFile));
            os = new BufferedOutputStream(response.getOutputStream());
            // 用Spring工具类分块拷贝，效率更高且避免手动处理缓冲区
            FileCopyUtils.copy(is, os);
            os.flush(); // 强制刷出所有数据

        } catch (Exception e) {
            // 修复3：异常时清空响应，返回明确错误信息
            try {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("下载失败：" + e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.err.println("下载失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 修复4：兜底关闭流，释放资源（关键！）
            try {
                if (os != null) os.close();
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 删除文件
     *
     * @param file
     */
    @DeleteMapping("/delete")
    public Result delFile(@RequestParam String file) {
        // 身份验证
//        String planDid = this.planMapper.getPlanDidByFileName(file);
//        checkUserRoleForFileDelete(planDid);
        // 执行业务
        String[] parts = file.split("/");
        String fileName = parts[parts.length - 1];
        String fullFilePath = filePath  + fileName;
        File targetFile = new File(fullFilePath);
        try {
            // 2. 校验文件存在性（不存在直接返回404）
            if (!targetFile.exists() || !targetFile.isFile()) {
                System.out.println("文件不存在：" + fullFilePath);
                return Result.error();
            }
            FileUtil.del(fullFilePath);
            System.out.println("删除文件" + file + "成功");
            return Result.success();
        }catch (Exception e){
            e.printStackTrace();
            return Result.error();
        }
    }

    /**
     * wang-editor编辑器文件上传接口
     */
    @PostMapping("/wang/upload")
    public Map<String, Object> wangEditorUpload(MultipartFile file) {
        String flag = System.currentTimeMillis() + "";
        String fileName = file.getOriginalFilename();
        try {
            // 文件存储形式：时间戳-文件名
            FileUtil.writeBytes(file.getBytes(), filePath + flag + "-" + fileName);
            System.out.println(fileName + "--上传成功");
            Thread.sleep(1L);
        } catch (Exception e) {
            System.err.println(fileName + "--文件上传失败");
        }
        String http = "http://" + ip + ":" + port + "/files/";
        Map<String, Object> resMap = new HashMap<>();
        // wangEditor上传图片成功后， 需要返回的参数
        resMap.put("errno", 0);
        resMap.put("data", CollUtil.newArrayList(Dict.create().set("url", http + flag + "-" + fileName)));
        return resMap;
    }




}
