package com.qzb.springboot.utils;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.qzb.springboot.common.Constants;
import com.qzb.springboot.common.PlanConstants;
import com.qzb.springboot.common.enums.MeetingConstants;
import com.qzb.springboot.entity.*;
import com.qzb.springboot.mapper.MeetingMapper;
import com.qzb.springboot.mapper.SysdictMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/**
 * Excel导入监听器
 * 逐行读取Excel数据并转换为Meeting实体
 */
@Slf4j
@Component
public class MeetingExcelListener extends AnalysisEventListener<MeetingExcel> {

    /** 批量插入阈值（避免内存溢出） */
    private static final int BATCH_SIZE = 100;
    /** 临时存储读取的数据 */
    private List<Meeting> meetingList = new ArrayList<>(BATCH_SIZE);
    private StringBuilder estr = new StringBuilder();

    @Resource
    private MeetingMapper meetingMapper;
    @Resource
    private SysdictMapper sysdictMapper;
    /**
     * 逐行处理Excel数据
     */
    @Override
    public void invoke(MeetingExcel excel, AnalysisContext context) {
        log.info("读取到Excel数据：{}", excel);
        String row = "第" + context.readRowHolder().getRowIndex().toString() + "行数据：";
        // 转换DTO为Meeting实体
        Meeting meeting = new Meeting();
        BeanUtils.copyProperties(excel, meeting);
        // 获取当前用户
        User user = TokenUtils.getCurrentUser();
        if(ObjectUtil.isNull(user) || ObjectUtil.isEmpty(user)){
            estr.append(MeetingConstants.USER_ERROR);
        }
        // 0. 格式化时间
        // 日期格式化+星期格式化-单日会议
        // 起始日期
        if(ObjectUtil.isNotNull(meeting.getStartDate()) && !meeting.getStartDate().isEmpty()){
            try{
                meeting.setStartDate(TimeUtils.formatToYyyyMmDd(meeting.getStartDate()));
            }catch (Exception e){
                estr.append("\n" +row +MeetingConstants.START_DATA_ERROR);
            }
            try{
                meeting.setStartWeek(TimeUtils.getChineseWeek(meeting.getStartDate()));
            }catch (Exception e){
                estr.append("\n" +row + e.getMessage());
            }
        }else{
            estr.append("\n" +row + MeetingConstants.START_DATA_MISSING);
        }

        // 起始时间
        String timePlusTowHour = null;
        String startTime = null;
        if(ObjectUtil.isNotNull(meeting.getStartStartTime()) && !meeting.getStartStartTime().isEmpty() ){
            // 去掉上午、下午，格式化时间为HH:mm格式
            try{
                startTime = TimeUtils.formatTo24Hour(meeting.getStartStartTime());
                timePlusTowHour = TimeUtils.addTwoHours(startTime);

            }catch (Exception e){
                estr.append("\n" +row +MeetingConstants.START_TIME_ERROR);
            }

            try{
                meeting.setStartStartTime(TimeUtils.formatTimeSlot(startTime));
            }catch (Exception e){
                estr.append("\n" +row + e.getMessage());
            }
        }else{
            estr.append("\n" +row +meeting.getStartStartTime()+ MeetingConstants.START_TIME_MISSING);
        }
        //日期格式化+星期格式化-多日会议
        // 结束日期
        if(ObjectUtil.isNotNull(meeting.getEndDate()) && !meeting.getEndDate().isEmpty() &&
        ObjectUtil.isNotNull(meeting.getEndEndTime()) && !meeting.getEndEndTime().isEmpty()){
            try{
                meeting.setEndDate(TimeUtils.formatToYyyyMmDd(meeting.getEndDate()));
            }catch (Exception e){
                estr.append("\n" +row + MeetingConstants.END_DATA_ERROR);
            }
            try{
                meeting.setEndWeek(TimeUtils.getChineseWeek(meeting.getEndDate()));
            }catch (Exception e){
                estr.append("\n" +row + e.getMessage());
            }

            try{
                // 结束时间
                String endTime = TimeUtils.formatTo24Hour(meeting.getEndEndTime());
                meeting.setEndEndTime(TimeUtils.formatTimeSlot(endTime));
                // 多日会议
                meeting.setMeetingType(Constants.MULTI_MEETING_TYPE);
            }catch (Exception e){
                estr.append("\n" +row + e.getMessage());
            }
        }else {
           try{
               meeting.setEndEndTime(TimeUtils.formatTimeSlot(timePlusTowHour));
               //  单日会议
               meeting.setMeetingType(Constants.SINGLE_MEETING_TYPE);
           }catch (Exception e){
               estr.append("\n" +row + e.getMessage());
           }
        }

        // 1. 处理tid
        meeting.setTid(user.getTid());
        // 2. 处理uid（当前登录用户ID）
        meeting.setUid(user.getId());
        // 4. 处理delete
        meeting.setIsdelete("0");
        // 5. 处理id
        String uuid = UUID.randomUUID().toString();
        meeting.setId(uuid);
        // 6. 处理驾驶舱展示
        if (ObjectUtil.isNull(meeting.getIsshow())){
            estr.append("\n" +row + MeetingConstants.ISSHOW_MISSING);
        }else {
            String isshow = meeting.getIsshow().equals("是")? "1" : "0";
            if (ObjectUtil.isNull(isshow)){
                estr.append("\n" +row + MeetingConstants.ISSHOW_ERROR);
            }else {
                meeting.setIsshow(isshow);
            }
        }


        // 8.处理status
        if (ObjectUtil.isNull(meeting.getStatus())){
            estr.append("\n" +row + MeetingConstants.STATUS_MISSING);
        }else{
            String status = sysdictMapper.getIdByValue(user.getTid(),"meeting_status",meeting.getStatus());
            if (ObjectUtil.isNull(status)){
                estr.append("\n" +row + MeetingConstants.STATUS_ERROR);
            }else {
                meeting.setStatus(status);
            }
        }
        // 添加到临时列表
        if(ObjectUtil.isNull(estr) || ObjectUtil.isEmpty(estr)){
            // 添加到临时列表
            meetingList.add(meeting);
        }else{
            meetingList.clear();
        }

        // 达到批量阈值则插入数据库
        if (meetingList.size() >= BATCH_SIZE) {
            saveData();
            // 清空列表
            meetingList.clear();
        }
    }

    /**
     * Excel读取完成后，处理剩余数据
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if(ObjectUtil.isNotEmpty(estr)){
            meetingList.clear();
            String error = estr.toString();
            estr.setLength(0);
            throw new RuntimeException(error);
        }
        // 插入剩余数据
        saveData();
        log.info("Excel数据读取完成，共处理{}条数据", meetingList.size());
        //防止数据重复
        meetingList.clear();
    }

    /**
     * 批量插入数据库
     */
    private void saveData() {
        if (!meetingList.isEmpty()) {
            try {
                if(ObjectUtil.isNotEmpty(estr)){
                    meetingList.clear();
                    String error = estr.toString();
                    estr.setLength(0);
                    throw new RuntimeException(error);
                }
                meetingMapper.batchInsertPlan(meetingList);
                log.info("批量插入{}条计划数据成功", meetingList.size());
            } catch (Exception e) {
                log.error("批量插入计划数据失败", e);
                meetingList.clear();  // 清除缓存数据，避免因为一次失败而导致后面的数据全部插入失败
                throw new RuntimeException("数据导入失败：" + e.getMessage());
            }
        }
    }


}
