package com.qzb.springboot.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.PlanConstants;
import com.qzb.springboot.common.enums.MeetingStatus;
import com.qzb.springboot.entity.*;
import com.qzb.springboot.exception.MeetingException;
import com.qzb.springboot.mapper.PlanMapper;
import com.qzb.springboot.mapper.UserMapper;
import com.qzb.springboot.mapper.SysdictMapper;
import com.qzb.springboot.utils.PlanExcelColumnWidthStrategy;
import com.qzb.springboot.utils.TimeUtils;
import com.qzb.springboot.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;

import com.qzb.springboot.utils.PlanExcelListener;
@Slf4j
@Service
public class PlanService {
    @Resource
    private PlanMapper planMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private PlanExcelListener planExcelListener;
    @Resource
    private SysdictMapper sysdictMapper;
    @Resource
    private SysdictService sysdictService;
    public void addPlan(Plan plan){
//        // 1. 定义日期格式（注意：MM是月份，mm是分钟，必须大写MM）
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        // 2. 生成当前日期的 yyyy-MM-dd 格式字符串
//        LocalDate currentDate = LocalDate.now(); // 获取当前本地日期（不含时分秒）
//        String createTime = currentDate.format(formatter);
        String currentTime = TimeUtils.getCurrentTime();
        plan.setCreateTime(currentTime);
        plan.setUpdateTime(currentTime);
        plan.setIsdelete("0");
        this.planMapper.addPlan(plan);
    }

    public void updatePlan(Plan plan){
        // 修改更新时间
        plan.setUpdateTime(TimeUtils.getCurrentTime());
        this.planMapper.updatePlan(plan);
    }

    public List<Plan> getAllPlanByPerson(String uid){
        List<Plan> list = planMapper.getAllPlanByPerson(uid);
        list = getPlanWithFullType(list);
        return list;
    }
    public List<Plan> getAllPlanByDepartment(String tid,String did,String isopen){
        List<Plan> list =planMapper.getAllPlanByDepartment(tid,did,isopen);
        list = getPlanWithFullType(list);
        return list;
    }

    public List<Plan> getAllPlanByTenant(String tid){
        List<Plan> list = planMapper.getAllPlanByTenant(tid);
        list = getPlanWithFullType(list);
        return list;
    }
    public Integer getPlanNumPrepareByPerson(String tid, String did, String uid) {
       return planMapper.getPlanNumPrepareByPerson(tid,did,uid);
    }

    public Integer getPlanNumCompleteByPerson(String tid,String did,String uid){
        return planMapper.getPlanNumCompleteByPerson(tid,did,uid);
    }
    public Integer getPlanNumDoingByPerson(String tid,String did,String uid){
        return planMapper.getPlanNumDoingByPerson(tid,did,uid);
    }
    public Integer getPlanNumCancelByPerson(String tid,String did,String uid){
        return planMapper.getPlanNumCancelByPerson(tid,did,uid);
    }

    public Integer getPlanNumPrepareByDepartment(String tid,String did){
        return planMapper.getPlanNumPrepareByDepartment(tid,did);
    }

    public Integer getPlanNumCompleteByDepartment(String tid,String did){
        return planMapper.getPlanNumCompleteByDepartment(tid,did);
    }
    public Integer getPlanNumDoingByDepartment(String tid,String did){
        return planMapper.getPlanNumDoingByDepartment(tid,did);
    }
    public Integer getPlanNumCancelByDepartment(String tid,String did){
        return planMapper.getPlanNumCancelByDepartment(tid,did);
    }

    public List<Plan> getPlanDetail(Plan plan){
        List<Plan> list = planMapper.getPlanDetail(plan);
        list = getPlanWithFullType(list);
        return list;
    }


    public void addPlanLog(PlanLog planLog){
        String uuid = UUID.randomUUID().toString();
        planLog.setId(uuid);
//        // 1. 定义日期格式（注意：MM是月份，mm是分钟，必须大写MM）
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        // 2. 生成当前日期的 yyyy-MM-dd 格式字符串
//        LocalDate currentDate = LocalDate.now(); // 获取当前本地日期（不含时分秒）
//        String createTime = currentDate.format(formatter);
        planLog.setCreateTime(TimeUtils.getCurrentTime());
        this.planMapper.addPlanLog(planLog);
    }

    public void updatePlanLog(PlanLog planLog){
        this.planMapper.updatePlanLog(planLog);
    }

    public void deletePlanLog(String plid){
        this.planMapper.deletePlanLog(plid);
    }

    public void getPlanLogByPid(String pid){
        this.planMapper.getPlanLogByPid(pid);
    }

    public List<Plan> searchPlanByCondition(Plan plan){
        List<Plan> list = this.planMapper.searchPlanByCondition(plan);
        list = getPlanWithFullType(list);
        return list;
    }

    public String addPlanFile(PlanFiles planFiles) {
        String uuid = UUID.randomUUID().toString();
        planFiles.setId(uuid);
//        // 1. 定义日期格式（注意：MM是月份，mm是分钟，必须大写MM）
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        // 2. 生成当前日期的 yyyy-MM-dd 格式字符串
//        LocalDate currentDate = LocalDate.now(); // 获取当前本地日期（不含时分秒）
//        String createTime = currentDate.format(formatter);
        planFiles.setCreateTime(TimeUtils.getCurrentTime());
        this.planMapper.addPlanFile(planFiles);
        return uuid;
    }

    public List<PlanFiles> getPlanFiles(String pid) {
        return this.planMapper.getPlanFiles(pid);
    }

    public void deletePlanFiles(String pfid) {
        this.planMapper.deletePlanFiles(pfid);
    }

    /**
     * 导入Excel数据到plan表
     */
    public void importPlanData(MultipartFile file) throws IOException {
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
                throw new RuntimeException(PlanConstants.USER_ERROR);
            }
            SysdictService.DictPathIndex typePathIndex = sysdictService.buildTypePathIndex(importUser.getTid());
            planExcelListener.setTypePathIndex(typePathIndex);
            EasyExcel.read(inputStream, PlanExcel.class, planExcelListener)
                    .sheet() // 读取第一个sheet
                    .headRowNumber(1) // 表头行号（第1行）
                    .doRead();
        } catch (Exception e) {
            log.debug("模板导入异常："+e.getMessage());
//            throw new MeetingException(MeetingStatus.MEETING_TIME_ERROR);
            throw e;
        } finally {
            // 导入结束清空索引，防止残留到下次导入
            planExcelListener.clearTypePathIndex();
        }
    }

    public PageInfo<Plan> getPlanPage(Plan plan, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Plan> list = planMapper.searchPlanByCondition(plan);
        list = getPlanWithFullType(list);
        return PageInfo.of(list);
    }

    public PageInfo<Plan> getAllPlanByPersonPage(String uid, Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Plan> list =planMapper.getAllPlanByPerson(uid);
        //显示type完整路径
        list = getPlanWithFullType(list);
        return PageInfo.of(list);
    }
    public PageInfo<Plan> getAllPlanByDepartmentPage(String tid,String did,String isopen, Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Plan> list =planMapper.getAllPlanByDepartment(tid,did,isopen);
        //显示type完整路径
        list = getPlanWithFullType(list);
        return PageInfo.of(list);
    }


    public PageInfo<Plan> getAllPlanByTenantPage(String tid, Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Plan> list =planMapper.getAllPlanByTenant(tid);
        list = getPlanWithFullType(list);
        return PageInfo.of(list);
    }

    public List<Plan> screenGetAllPlanByTenant(String tid) {
        List<Plan> list = this.planMapper.screenGetAllPlanByTenant(tid);
        list = getPlanWithFullType(list);
        return list;
    }

    public List<Plan> screenGetAllPlanByTenantAndTime(String tid,String time) {
        //显示type完整路径
        List<Plan> list = this.planMapper.screenGetAllPlanByTenantAndTime(tid,time);
        list = getPlanWithFullType(list);
        return list;
    }


    /**
     * 查询plan列表，拼接type的完整路径
     */
    public List<Plan> getPlanWithFullType(List<Plan> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return new ArrayList<>();
        }

        // 2. 遍历每个plan，拼接typeValue
        for (Plan plan : planList) {
            String typeId = plan.getType();
            // 递归查询type的完整路径
            String typePath = buildDictPath(typeId);
            plan.setTypeValue(typePath);
        }
        return planList;
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


    /**
     * 导出计划数据为Excel
     */
    public void exportPlanData(List<String> ids, String tid, HttpServletResponse response) throws Exception {
        // 1. 查询数据
        List<Plan> planList = planMapper.selectPlanByIdsAndTid(ids, tid);
        if (planList == null || planList.isEmpty()) {
            throw new RuntimeException("未查询到符合条件的数据，导出失败");
        }
        planList = getPlanWithFullType(planList);
        //将uuid转成中文
        for(Plan plan : planList){
            plan.setType(plan.getTypeValue());
            plan.setImp(plan.getImpValue());
            plan.setStatus(plan.getStatusValue());
            plan.setAttribute(plan.getAttributeValue());
            plan.setDid(plan.getDepartment());
        }
        // 2. 转换为Excel导出DTO（与导入表头一致）
        List<PlanExcel> excelList = new ArrayList<>();
        for (Plan plan : planList) {
            PlanExcel dto = new PlanExcel();
            BeanUtils.copyProperties(plan, dto);
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
        PlanExcelColumnWidthStrategy columnWidthStrategy = new PlanExcelColumnWidthStrategy();

        // 7. 响应头配置
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(TimeUtils.getCurrentTime()+"-任务数据导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 8. 写入Excel
        EasyExcel.write(response.getOutputStream(), PlanExcel.class)
                .registerWriteHandler(styleStrategy)
                .registerWriteHandler(columnWidthStrategy)
                .sheet("计划任务数据")
                .doWrite(excelList);

    }


    /**
     * 真实有任务的负责人清单(按任务数降序)：AI"按负责人筛选"时直接给出可点选名单。
     * plan.head历史数据混存用户ID/姓名：ID转姓名，姓名原样保留，去重。
     */
    public List<String> getTopHeadNames(String tid, int limit) {
        List<Map<String, Object>> rows = planMapper.countByHead(tid, limit + 10);
        List<String> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> row : rows) {
            if (names.size() >= limit) break;
            Object head = row.get("head");
            if (head == null || String.valueOf(head).trim().isEmpty()) continue;
            String name = String.valueOf(head).trim();
            // head存的是用户ID时解析成姓名
            // 跳过脏数据：多人组合值(张三、李四)与含数字的非姓名
            if (name.matches(".*[0-9a-zA-Z].*") || name.matches(".*[、,，/ ].*")) {
                continue;
            }
            if (name.matches("[0-9a-fA-F-]{8,}")) {
                try {
                    User u = userMapper.getUserInfo(name);
                    if (u != null && ObjectUtil.isNotEmpty(u.getName())) {
                        name = u.getName();
                    }
                } catch (Exception e) { /* 解析失败保留原值 */ }
            }
            if (!seen.contains(name)) {
                seen.add(name);
                names.add(name);
            }
        }
        return names;
    }
}
