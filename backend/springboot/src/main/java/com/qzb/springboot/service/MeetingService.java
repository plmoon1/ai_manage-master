package com.qzb.springboot.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.entity.Meeting;
import com.qzb.springboot.entity.MeetingExcel;
import com.qzb.springboot.mapper.MeetingMapper;
import com.qzb.springboot.mapper.SysdictMapper;
import com.qzb.springboot.utils.MeetingExcelColumnWidthStrategy;
import com.qzb.springboot.utils.PlanExcelColumnWidthStrategy;
import com.qzb.springboot.utils.MeetingExcelListener;
import com.qzb.springboot.utils.TimeUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingService {

    @Resource
    private MeetingMapper meetingMapper;
    @Resource
    private MeetingExcelListener meetingExcelListener;

    @Resource
    private SysdictMapper sysdictMapper;
    public void addMeeting(Meeting meeting) {
        // isdelete给默认值
        meeting.setIsdelete("0");
        // 设置UUID
        meeting.setId(UUID.randomUUID().toString());
        // 设置timeSlot
        if(ObjectUtil.isNotNull(meeting.getStartStartTime()) ){
            meeting.setStartStartTime(TimeUtils.formatTimeSlot(meeting.getStartStartTime()));
        }
        if(ObjectUtil.isNotNull(meeting.getEndEndTime()) && !meeting.getEndEndTime().isEmpty()){
            meeting.setEndEndTime(TimeUtils.formatTimeSlot(meeting.getEndEndTime()));
        }
        meetingMapper.addMeeting(meeting);
    }

    public void deleteMeetingByID(String id) {
        meetingMapper.deleteMeetingByID(id);
    }

    public void updateMeeting(Meeting meeting) {
        if(ObjectUtil.isNotNull(meeting.getStartStartTime())){
            meeting.setStartStartTime(TimeUtils.formatTimeSlot(meeting.getStartStartTime()));
        }
        if(ObjectUtil.isNotNull(meeting.getEndEndTime()) && !meeting.getEndEndTime().isEmpty()){
            meeting.setEndEndTime(TimeUtils.formatTimeSlot(meeting.getEndEndTime()));
        }
        meetingMapper.updateMeeting(meeting);
    }

    public Meeting getMeetingByPersonPage(String id) {
        return meetingMapper.getMeetingById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public PageInfo<Meeting> getMeetingByConditionPage(Meeting meeting,Integer pageNum, Integer pageSize) {
        // 更新已到时间的会议状态为“已结束”
        updateMeetingToFinished(meeting.getTid());
        // 执行正常业务
        PageHelper.startPage(pageNum, pageSize);
        List<Meeting> list = meetingMapper.getMeetingByConditionPage(meeting);
        return PageInfo.of(list);
    }

    @Transactional(rollbackFor = Exception.class)
    public PageInfo<Meeting> getMeetingByTenantPage(String tid, Integer pageNum, Integer pageSize) {
        // 更新已到时间的会议状态为“已结束”
        updateMeetingToFinished(tid);
        PageHelper.startPage(pageNum, pageSize);
        List<Meeting> list = meetingMapper.getMeetingByTenantPage(tid);
        return PageInfo.of(list);
    }

    public List<Meeting> screenGetAllMeetingByTenantAndTime(String tid, String startTime, String endTime) {
        return this.meetingMapper.screenGetAllMeetingByTenantAndTime(tid,startTime,endTime);
    }

    public void importMeetingData(MultipartFile file) {
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
            EasyExcel.read(inputStream, MeetingExcel.class, meetingExcelListener)
                    .sheet() // 读取第一个sheet
                    .headRowNumber(1) // 表头行号（第1行）
                    .doRead();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("读取Excel文件失败：" + e.getMessage());
        }
    }


    /**
     * 导出计划数据为Excel
     */
    public void exportMeetingData(List<String> ids, String tid, HttpServletResponse response) throws Exception {
        // 1. 查询数据
        List<Meeting> meetingList = meetingMapper.selectMeetingByIdsAndTid(ids, tid);
        if (meetingList == null || meetingList.isEmpty()) {
            throw new RuntimeException("未查询到符合条件的数据，导出失败");
        }
        // 2. 转换为Excel导出DTO（与导入表头一致）
        List<MeetingExcel> excelList = new ArrayList<>();
        for (Meeting meeting : meetingList) {
            MeetingExcel dto = new MeetingExcel();
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
        MeetingExcelColumnWidthStrategy columnWidthStrategy = new MeetingExcelColumnWidthStrategy();

        // 7. 响应头配置
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(TimeUtils.getCurrentTime()+"-会议数据导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 8. 写入Excel
        EasyExcel.write(response.getOutputStream(), MeetingExcel.class)
                .registerWriteHandler(styleStrategy)
                .registerWriteHandler(columnWidthStrategy)
                .sheet("会议数据")
                .doWrite(excelList);

    }


    /**
     * 【核心方法】自动更新会议状态为【已结束】
     * 业务规则：
     * 1. 单日会议：start_date ≤ 当天 且 结束时间 < 当前时间 → 已结束
     * 2. 多日会议：end_date ≤ 当天 且 结束时间 < 当前时间 → 已结束
     */
    @Transactional(rollbackFor = Exception.class) // 事务管理，异常回滚
    public void updateMeetingToFinished(String tid) {
        // 1. 获取当天日期、当前时间（工具类封装）
        String today = TimeUtils.getTodayDate();
        String currentTime = TimeUtils.getCurrentTimeHHMM();

        // 查询“已结束”的id
        String finishedStatusId = sysdictMapper.selectByIdAndStatus(tid,"已结束");
        // 查询“已取消”的id
        String cancelledStatusId = sysdictMapper.selectByIdAndStatus(tid,"已取消");
        // 2. 查询所有需要更新为【已结束】的会议
        List<String> ids = meetingMapper.selectNeedUpdateMeetings(today, currentTime,tid,finishedStatusId,cancelledStatusId);
        if (ids.isEmpty()) {
            return; // 无需要更新的会议，直接返回
        }

        // 3. 批量更新会议状态（高效）
        meetingMapper.updateMeetingBatchStatus(ids,finishedStatusId);
    }
}
