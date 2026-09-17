package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.Meeting;
import com.qzb.springboot.entity.Plan;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会议安排Mapper
 */
public interface MeetingMapper  {


    void addMeeting(Meeting meeting);

    List<Meeting> getMeetingByConditionPage(Meeting meeting);

    Meeting getMeetingById(String id);

    void updateMeeting(Meeting meeting);

    void deleteMeetingByID(String id);

    List<Meeting> getMeetingByTenantPage(String tid);

    List<Meeting> screenGetAllMeetingByTenantAndTime(String tid, String startTime, String endTime);

    @Insert({
            "<script>",
            "INSERT INTO meeting (id, meeting_type, start_week, end_week, start_date,end_date,start_start_time,end_end_time,status, meeting_name, host, participants, location, tid, uid, isdelete, isshow)",
            "VALUES ",
            "<foreach collection='list' item='meeting' separator=','>",
            "(#{meeting.id}, #{meeting.meetingType}, #{meeting.startWeek}, #{meeting.endWeek},  #{meeting.startDate} ,#{meeting.endDate} ,#{meeting.startStartTime}  ,#{meeting.endEndTime} ,#{meeting.status} ,#{meeting.meetingName}, #{meeting.host}, #{meeting.participants}, #{meeting.location}, #{meeting.tid}, #{meeting.uid}, #{meeting.isdelete}, #{meeting.isshow})",
            "</foreach>",
            "</script>"
    })
    int batchInsertPlan(List<Meeting> planList);

    List<Meeting> selectMeetingByIdsAndTid(List<String> ids, String tid);


    /**
     * 查询【需要更新为已结束状态】的会议
     * @param today 当天日期 yyyy-MM-dd
     * @param currentTime 当前时间 HH:mm
     * @return 会议列表
     */
    List<String> selectNeedUpdateMeetings(
            @Param("today") String today,
            @Param("currentTime") String currentTime,
            String tid,
            String finishedStatusId,
            String cancelledStatusId
    );

    /**
     * 批量更新会议状态
     */
    int updateMeetingBatchStatus(List<String> ids,String finishedStatusId);


}
