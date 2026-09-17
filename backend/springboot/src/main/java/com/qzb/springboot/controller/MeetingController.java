package com.qzb.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.MeetingConstants;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.entity.Meeting;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.service.MeetingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/meeting")
public class MeetingController {
    @Resource
    private MeetingService meetingService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping("/addMeeting")
    public Result addMeeting(@RequestBody Meeting meeting) {
        meetingService.addMeeting(meeting);
        return Result.success();
    }


    @DeleteMapping("/deleteMeetingByID/{id}")
    public Result deleteMeetingByID(@PathVariable String id) {
        meetingService.deleteMeetingByID(id);
        return Result.success();
    }

    @PutMapping("/updateMeeting")
    public Result updateMeeting(@RequestBody Meeting meeting) {
        meetingService.updateMeeting(meeting);
        return Result.success();
    }

    @GetMapping("/getMeetingByPersonPage/{uid}")
    public Result getMeetingByPersonPage(@PathVariable String uid) {
        Meeting meeting = meetingService.getMeetingByPersonPage(uid);
        return Result.success(meeting);
    }

    @GetMapping("/getMeetingByTenantPage")
    public Result getMeetingByTenantPage(@RequestParam String tid,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Meeting> page = meetingService.getMeetingByTenantPage(tid,pageNum,pageSize);
        return Result.success(page);
    }


    @GetMapping("/getMeetingByConditionPage")
    public Result getMeetingByConditionPage(Meeting meeting,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.debug("进入会议通用接口");
        PageInfo<Meeting> page = meetingService.getMeetingByConditionPage(meeting,pageNum,pageSize);
        return Result.success(page);
    }

    /**
     * 上传Excel并导入数据
     */
    @PostMapping("/import")
    public Result importMeeting(@RequestParam("file") MultipartFile file) {
        try {
            this.meetingService.importMeetingData(file);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(MeetingConstants.FILE_INSERT_ERROR_CODE, e.getMessage());
        }
    }

    /**
     * 导出计划数据（前端传递ids列表+tid）
     */
    @PostMapping("/export")
    public void exportPlan(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        try {
            // 接收前端参数
            List<String> ids = (List<String>) params.get("ids");
            String tid = (String) params.get("tid");

            // 参数校验
            if (ids == null || ids.isEmpty() || tid == null || tid.trim().isEmpty()) {
                throw new RuntimeException("参数异常：会议ID列表和学院ID不能为空");
            }

            // 执行导出
            meetingService.exportMeetingData(ids, tid, response);
        } catch (Exception e) {
            // 异常处理
            response.setContentType("application/json;charset=utf-8");
            try {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 500);
                result.put("msg", "导出失败：" + e.getMessage());
                // 核心修改：用Jackson转JSON，无依赖、不爆红
                response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }



    @GetMapping("/screen/getAllMeetingByTenantAndTime")
    public Result screenGetAllPlanByTenantAndTime(@RequestParam String tid,
                                                  @RequestParam String startTime,
                                                  @RequestParam String endTime){

        List<Meeting> list = this.meetingService.screenGetAllMeetingByTenantAndTime(tid,startTime,endTime);
        return Result.success(list);
    }
}
