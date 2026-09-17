package com.qzb.springboot.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.WorkConstants;
import com.qzb.springboot.entity.*;
import com.qzb.springboot.mapper.SysdictMapper;
import com.qzb.springboot.mapper.WorksMapper;
import com.qzb.springboot.utils.WorksExcelColumnWidthStrategy;
import com.qzb.springboot.utils.WorksExcelListener;
import com.qzb.springboot.utils.TimeUtils;
import com.qzb.springboot.utils.TokenUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class WorksService {
    @Resource
    private WorksMapper worksMapper;
    @Resource
    private WorksExcelListener worksExcelListener;
    @Resource
    private SysdictMapper sysdictMapper;
    @Resource
    private SysdictService sysdictService;

    public void addWorks(Works works){
//        // 1. 定义日期格式（注意：MM是月份，mm是分钟，必须大写MM）
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        // 2. 生成当前日期的 yyyy-MM-dd 格式字符串
//        LocalDate currentDate = LocalDate.now(); // 获取当前本地日期（不含时分秒）
//        String createTime = currentDate.format(formatter);
        String currentTime = TimeUtils.getCurrentTime();
        works.setCreateTime(currentTime);
        works.setUpdateTime(currentTime);
        works.setIsdelete("0");
        this.worksMapper.addWorks(works);
    }

    public void updateWorks(Works works){
        // 修改更新时间
        works.setUpdateTime(TimeUtils.getCurrentTime());
        this.worksMapper.updateWorks(works);
    }

    public List<Works> getAllWorksByPerson(String uid){
        List<Works> list = worksMapper.getAllWorksByPerson(uid);
        list = getWorksWithFullType(list);
        return list;
    }
    public List<Works> getAllWorksByDepartment(String tid,String did,String isopen){
        List<Works> list = worksMapper.getAllWorksByDepartment(tid,did,isopen);
        list = getWorksWithFullType(list);
        return list;
    }

    public List<Works> getAllWorksByTenant(String tid){
        List<Works> list = worksMapper.getAllWorksByTenant(tid);
        list = getWorksWithFullType(list);
        return list;
    }
    public Integer getWorksNumPrepareByPerson(String tid, String did, String uid) {
        return worksMapper.getWorksNumPrepareByPerson(tid,did,uid);
    }

    public Integer getWorksNumCompleteByPerson(String tid,String did,String uid){
        return worksMapper.getWorksNumCompleteByPerson(tid,did,uid);
    }
    public Integer getWorksNumDoingByPerson(String tid,String did,String uid){
        return worksMapper.getWorksNumDoingByPerson(tid,did,uid);
    }
    public Integer getWorksNumCancelByPerson(String tid,String did,String uid){
        return worksMapper.getWorksNumCancelByPerson(tid,did,uid);
    }

    public Integer getWorksNumPrepareByDepartment(String tid,String did){
        return worksMapper.getWorksNumPrepareByDepartment(tid,did);
    }

    public Integer getWorksNumCompleteByDepartment(String tid,String did){
        return worksMapper.getWorksNumCompleteByDepartment(tid,did);
    }
    public Integer getWorksNumDoingByDepartment(String tid,String did){
        return worksMapper.getWorksNumDoingByDepartment(tid,did);
    }
    public Integer getWorksNumCancelByDepartment(String tid,String did){
        return worksMapper.getWorksNumCancelByDepartment(tid,did);
    }

    public List<Works> getWorksDetail(Works works){
        List<Works> list = worksMapper.getWorksDetail(works);
        list = getWorksWithFullType(list);
        return list;
    }


//    public void addWorksLog(WorksLog worksLog){
//        String uuid = UUID.randomUUID().toString();
//        worksLog.setId(uuid);
////        // 1. 定义日期格式（注意：MM是月份，mm是分钟，必须大写MM）
////        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
////        // 2. 生成当前日期的 yyyy-MM-dd 格式字符串
////        LocalDate currentDate = LocalDate.now(); // 获取当前本地日期（不含时分秒）
////        String createTime = currentDate.format(formatter);
//        worksLog.setCreateTime(TimeUtils.getCurrentTime());
//        this.worksMapper.addWorksLog(worksLog);
//    }

//    public void updateWorksLog(WorksLog worksLog){
//        this.worksMapper.updateWorksLog(worksLog);
//    }

    public void deleteWorksLog(String plid){
        this.worksMapper.deleteWorksLog(plid);
    }

    public void getWorksLogByPid(String pid){
        this.worksMapper.getWorksLogByPid(pid);
    }

    public List<Works>  searchWorksByCondition(Works works){
        List<Works> list = this.worksMapper.searchWorksByCondition(works);
        list = getWorksWithFullType(list);
        return list;
    }

    public String addWorksFile(WorksFiles worksFiles) {
        String uuid = UUID.randomUUID().toString();
        worksFiles.setId(uuid);
//        // 1. 定义日期格式（注意：MM是月份，mm是分钟，必须大写MM）
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        // 2. 生成当前日期的 yyyy-MM-dd 格式字符串
//        LocalDate currentDate = LocalDate.now(); // 获取当前本地日期（不含时分秒）
//        String createTime = currentDate.format(formatter);
        worksFiles.setCreateTime(TimeUtils.getCurrentTime());
        this.worksMapper.addWorksFile(worksFiles);
        return uuid;
    }

    public List<WorksFiles> getWorksFiles(String pid) {
        return this.worksMapper.getWorksFiles(pid);
    }

    public void deleteWorksFiles(String pfid) {
        this.worksMapper.deleteWorksFiles(pfid);
    }

    /**
     * 导入Excel数据到works表
     */
    public void importWorksData(MultipartFile file) {
        // 校验文件
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传的Excel文件不能为空");
        }
        // 校验文件格式
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new RuntimeException("仅支持.xlsx/.xls格式的Excel文件");
        }

        // 读取Excel文件
        try (InputStream inputStream = file.getInputStream()) {
            // 2026-08-17 任务类型支持同名叶子（不同父节点），导入按完整路径消歧
            // 索引每次导入构建一次，逐行复用（监听器为单例，索引按次传入避免并发污染）
            User importUser = TokenUtils.getCurrentUser();
            if (importUser == null) {
                throw new RuntimeException(WorkConstants.USER_ERROR);
            }
            SysdictService.DictPathIndex typePathIndex = sysdictService.buildTypePathIndex(importUser.getTid());
            worksExcelListener.setTypePathIndex(typePathIndex);
            EasyExcel.read(inputStream, WorksExcel.class, worksExcelListener)
                    .sheet() // 读取第一个sheet
                    .headRowNumber(1) // 表头行号（第1行）
                    .doRead();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("读取Excel文件失败：" + e.getMessage());
        } finally {
            // 导入结束清空索引，防止残留到下次导入
            worksExcelListener.clearTypePathIndex();
        }
    }

    public PageInfo<Works> getWorksPage(Works works, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Works> list = worksMapper.searchWorksByCondition(works);
        list = getWorksWithFullType(list);
        return PageInfo.of(list);
    }

    public PageInfo<Works> getAllWorksByPersonPage(String uid, Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Works> list =worksMapper.getAllWorksByPerson(uid);
        list = getWorksWithFullType(list);
        return PageInfo.of(list);
    }
    public PageInfo<Works> getAllWorksByDepartmentPage(String tid,String did,String isopen, Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Works> list =worksMapper.getAllWorksByDepartment(tid,did,isopen);
        list = getWorksWithFullType(list);
        return PageInfo.of(list);
    }


    public PageInfo<Works> getAllWorksByTenantPage(String tid, Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Works> list =worksMapper.getAllWorksByTenant(tid);
        list = getWorksWithFullType(list);
        return PageInfo.of(list);
    }

    public List<Works> screenGetAllWorksByTenant(String tid) {
        List<Works> list = this.worksMapper.screenGetAllWorksByTenant(tid);
        list = getWorksWithFullType(list);
        return list;
    }



    /**
     * 导出计划数据为Excel
     */
    public void exportWorksData(List<String> ids, String tid, HttpServletResponse response) throws Exception {
        // 1. 查询数据
        List<Works> workList = worksMapper.selectWorksByIdsAndTid(ids, tid);
        if (workList == null || workList.isEmpty()) {
            throw new RuntimeException("未查询到符合条件的数据，导出失败");
        }
        workList = getWorksWithFullType(workList);
        for (Works work: workList) {
            work.setType(work.getTypeValue());
        }
        // 2. 转换为Excel导出DTO（与导入表头一致）
        List<WorksExcel> excelList = new ArrayList<>();
        for (Works meeting : workList) {
            WorksExcel dto = new WorksExcel();
            BeanUtils.copyProperties(meeting, dto);
            excelList.add(dto);
        }

        // 3. 表头样式：白色背景 + 加粗 + 无边框（核心修改）
        WriteCellStyle headCellStyle = new WriteCellStyle();
//        headCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
//        headCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        headCellStyle.setFillPatternType(FillPatternType.NO_FILL);

        // 关键：去掉表头所有边框
        headCellStyle.setBorderTop(BorderStyle.NONE);
        headCellStyle.setBorderBottom(BorderStyle.NONE);
        headCellStyle.setBorderLeft(BorderStyle.NONE);
        headCellStyle.setBorderRight(BorderStyle.NONE);
        // 表头字体：加粗、12号字
        WriteFont headFont = new WriteFont();
        headFont.setFontHeightInPoints((short)16);
        headFont.setBold(true);
        headCellStyle.setWriteFont(headFont);

        // 4. 内容样式（可选：如果需要内容也无边框，可同步设置）
        WriteCellStyle contentCellStyle = new WriteCellStyle();
        // 若需要内容也去掉边框，取消下面注释
        // contentCellStyle.setBorderTop(BorderStyle.NONE);
        // contentCellStyle.setBorderBottom(BorderStyle.NONE);
        // contentCellStyle.setBorderLeft(BorderStyle.NONE);
        // contentCellStyle.setBorderRight(BorderStyle.NONE);
        WriteFont contentFont = new WriteFont();
        contentFont.setFontHeightInPoints((short)15);
        contentCellStyle.setWriteFont(contentFont);

        // 5. 注册样式策略
        HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(headCellStyle, contentCellStyle);
        // 6. 注册自定义列宽策略（每列独立宽度）
        WorksExcelColumnWidthStrategy columnWidthStrategy = new WorksExcelColumnWidthStrategy();

        // 7. 响应头配置
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(TimeUtils.getCurrentTime()+"-工作数据导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 8. 写入Excel
        EasyExcel.write(response.getOutputStream(), WorksExcel.class)
                .registerWriteHandler(styleStrategy)
                .registerWriteHandler(columnWidthStrategy)
                .sheet("工作数据")
                .doWrite(excelList);

    }


    /**
     * 查询works列表，拼接type的完整路径
     */
    public List<Works> getWorksWithFullType(List<Works> worksList) {
        if (CollectionUtils.isEmpty(worksList)) {
            return new ArrayList<>();
        }

        // 2. 遍历每个works，拼接typeValue
        for (Works works : worksList) {
            String typeId = works.getType();
            // 递归查询type的完整路径
            String typePath = buildDictPath(typeId);
            works.setTypeValue(typePath);
        }
        return worksList;
    }

    /**
     * 递归拼接字典节点的完整路径
     */
    private String buildDictPath(String dictId) {
        List<String> valueList = new ArrayList<>();
        // 递归查询所有父节点，收集value
        recursiveGetDictValue(dictId, valueList);
        // 反转列表（递归是从子到根，反转后根在前），拼接成路径
        Collections.reverse(valueList);
        return String.join("/", valueList);
    }

    /**
     * 递归查询字典节点的value，直到根节点
     */
    private void recursiveGetDictValue(String dictId, List<String> valueList) {
        SysDict dict = sysdictMapper.selectById(dictId);
        if (dict == null) {
            return;
        }
        // 添加当前节点的value
        valueList.add(dict.getValue());
        // 终止条件：根节点（fid=id）
        if (dict.getFid().equals(dict.getId())) {
            return;
        }
        // 递归查询父节点
        recursiveGetDictValue(dict.getFid(), valueList);
    }
}
