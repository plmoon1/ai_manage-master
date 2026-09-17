package com.qzb.springboot.controller;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;

import com.qzb.springboot.common.PlanConstants;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.OpenEnum;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.*;
import com.qzb.springboot.exception.PermissionException;
import com.qzb.springboot.mapper.PlanMapper;
import com.qzb.springboot.service.PlanService;
import com.qzb.springboot.utils.AuthCheckUtils;
import com.qzb.springboot.utils.TokenUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plan")
public class PlanController {
    @Resource
    private PlanService planService;
    @Resource
    private PlanMapper planMapper;
    // SpringBoot 自带的JSON工具类
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GetMapping("/getAllPlanByPerson")
    public Result getAllPlanByPerson(@RequestParam String uid){
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if(!user.getId().equals(uid)) {
            return Result.error(ResultCodeEnum.Authority_ERROR);
        }
        List<Plan> plan = this.planService.getAllPlanByPerson(uid);
        return Result.success(plan);
    }

    @GetMapping("/getAllPlanByDepartment")
    public Result getAllPlanByDepartment(@RequestParam String tid, @RequestParam String did){
        String isopen = null;
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if (user.getRid().equals(RoleEnum.DEPARTMENT_ORDINARY.code)){
            isopen = "1";
        }
        List<Plan> plan = this.planService.getAllPlanByDepartment(tid, did,isopen);
        return Result.success(plan);
    }

    @GetMapping("/getAllPlanByTenant")
    public Result getAllPlanByTenant(@RequestParam String tid){
        List<Plan> plan = this.planService.getAllPlanByTenant(tid);
        return Result.success(plan);
    }

    @GetMapping("/screen/getAllPlanByTenant")
    public Result screenGetAllPlanByTenant(@RequestParam String tid){
        List<Plan> plan = this.planService.screenGetAllPlanByTenant(tid);
        return Result.success(plan);
    }


    @GetMapping("/searchPlanByCondition")
    public Result searchPlanByCondition(Plan plan){
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if(!user.getRid().equals(RoleEnum.COLLEGE_LEADER.code)){
            // 可见范围为“私密”
            if (ObjectUtil.isNotNull(plan.getIsopen()) && plan.getIsopen().equals(OpenEnum.SECRETE.code)){
                if(ObjectUtil.isNull(plan.getDid()) ){
                    plan.setDid(user.getDid());
                }
                AuthCheckUtils.checkUserRoleForPlan(plan.getDid());
            }
            // 可见范围没有选择，“公开”不用判断
            else if (ObjectUtil.isNull(plan.getIsopen())) {
                if(ObjectUtil.isNotNull(plan.getDid()) && plan.getDid().equals(user.getDid()) && user.getRid().equals(RoleEnum.DEPARTMENT_LEADER.code)){
                    plan.setIsopen(null);
                }else {
                    plan.setIsopen("1");
                }
            }
        }
        // 领导可以查看所有科室信息
        List<Plan> plans = this.planService.searchPlanByCondition(plan);
        return Result.success(plans);
    }

    @PostMapping("/addPlan")
    public Result addPlan(@RequestBody Plan plan){
        // 身份验证
//        AuthCheckUtils.checkUserRoleForPlan(plan.getDid());
        if(!AuthCheckUtils.checkAuthority("plan:operate:add")){
            throw new PermissionException(ResultCodeEnum.Authority_ERROR);
        }
        // 领导可以创建所有科室任务
        this.planService.addPlan(plan);
        return Result.success();
    }

    @PutMapping("/updatePlan")
    public Result updatePlan(@RequestBody Plan plan){
        // 身份验证
        String planDid = this.planMapper.getPlanDidById(plan.getId());
//        AuthCheckUtils.checkUserRoleForPlan(planDid);
        if(!AuthCheckUtils.checkAuthority("plan:operate:update")){
            throw new PermissionException(ResultCodeEnum.Authority_ERROR);
        }
        User user = TokenUtils.getCurrentUser();
        if(!AuthCheckUtils.checkAuthority("plan:operate:get:college") && !planDid.equals(user.getDid())){
            throw new PermissionException(ResultCodeEnum.Authority_ERROR);
        }
        // 分管领导可以修改所有科室任务
        this.planService.updatePlan(plan);
        return Result.success();
    }

    /**
     * 通用查询接口
     * */
    @GetMapping("/getPlanDetail")
    public Result getPlanDetail(Plan plan){
        List<Plan> plans = this.planService.getPlanDetail(plan);
        return Result.success(plans);
    }

    @GetMapping("/getPlanNumPrepareByPerson")
    public Result getPlanNumPrepareByPerson(@RequestParam String tid,
                                             @RequestParam String did,
                                             @RequestParam String uid){
        Integer num = this.planService.getPlanNumPrepareByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getPlanNumCompleteByPerson")
    public Result getPlanNumCompleteByPerson(@RequestParam String tid,
                                             @RequestParam String did,
                                             @RequestParam String uid){
        Integer num = this.planService.getPlanNumCompleteByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getPlanNumDoingByPerson")
    public Result getPlanNumDoingByPerson(@RequestParam String tid,
                                             @RequestParam String did,
                                             @RequestParam String uid){
        Integer num = this.planService.getPlanNumDoingByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getPlanNumCancelByPerson")
    public Result getPlanNumCancelByPerson(@RequestParam String tid,
                                          @RequestParam String did,
                                          @RequestParam String uid){
        Integer num = this.planService.getPlanNumCancelByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getPlanNumPrepareByDepartment")
    public Result getPlanNumPrepareByDepartment(@RequestParam String tid,
                                                 @RequestParam String did){
        Integer num = this.planService.getPlanNumPrepareByDepartment(tid,did);
        return Result.success(num);
    }

    @GetMapping("/getPlanNumCompleteByDepartment")
    public Result getPlanNumCompleteByDepartment(@RequestParam String tid,
                                             @RequestParam String did){
        Integer num = this.planService.getPlanNumCompleteByDepartment(tid,did);
        return Result.success(num);
    }

    @GetMapping("/getPlanNumDoingByDepartment")
    public Result getPlanNumDoingByDepartment(@RequestParam String tid,
                                          @RequestParam String did){

        Integer num = this.planService.getPlanNumDoingByDepartment(tid,did);
        return Result.success(num);
    }

    @GetMapping("/getPlanNumCancelByDepartment")
    public Result getPlanNumCancelByDepartment(@RequestParam String tid,
                                           @RequestParam String did){
        Integer num = this.planService.getPlanNumCancelByDepartment(tid,did);
        return Result.success(num);
    }

    @PostMapping("/addPlanLog")
    public Result addPlanLog(@RequestBody PlanLog planLog){
        this.planService.addPlanLog(planLog);
        return Result.success();
    }

    @PutMapping("/updatePlanLog")
    public Result updatePlanLog(@RequestBody PlanLog planLog){
        this.planService.updatePlanLog(planLog);
        return Result.success();
    }

    @DeleteMapping("/deletePlanLog")
    public Result deletePlanLog(@RequestParam String  plid){
        this.planService.deletePlanLog(plid);
        return Result.success();
    }

    @GetMapping("/getPlanLogByPid")
    public Result getPlanLogByPid(@RequestParam String pid){
        this.planService.getPlanLogByPid(pid);
        return Result.success();
    }


    @PostMapping("/addPlanFile")
    public Result addPlanFile(@RequestBody PlanFiles planFiles){
        // 身份验证
//        checkUserRoleForFileUpload
        String fileId = this.planService.addPlanFile(planFiles);
        return Result.success(fileId);
    }

    @GetMapping("/getPlanFiles")
    public Result getPlanFiles(@RequestParam String pid){
        List<PlanFiles> planFilesList = this.planService.getPlanFiles(pid);
        return Result.success(planFilesList);
    }
    @DeleteMapping("/deletePlanFiles")
    public Result deletePlanFiles(@RequestParam String pfid){
        // 身份验证
//        String planDid = planMapper.getPlanDidByPfid(pfid);
//        checkUserRoleForPlanFileDelete(planDid);
        this.planService.deletePlanFiles(pfid);
        return Result.success();
    }

    /**
     * 上传Excel并导入数据
     */
    @PostMapping("/import")
    public Result importPlan(@RequestParam("file") MultipartFile file) {
        try {
            // 身份验证
//            checkUserRoleForPlanImport();
            this.planService.importPlanData(file);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(PlanConstants.FILE_INSERT_ERROR_CODE, e.getMessage());
        }
    }

    /**
     * 分页查询
     */
    @GetMapping("/searchPlanByConditionPage")
    public Result searchPlanByConditionPage(Plan plan,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
            // 身份验证 + 数据范围控制（与 AI 工具共用同一套逻辑）
            AuthCheckUtils.applyPlanQueryScope(plan);
            // 执行业务
            PageInfo<Plan> page = planService.getPlanPage(plan, pageNum, pageSize);
            return Result.success(page);
    }

    @GetMapping("/getAllPlanByPersonPage")
    public Result getAllPlanByPersonPage(@RequestParam String uid,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize){
        // 身份验证
//        User user = TokenUtils.getCurrentUser();
//        if(!user.getId().equals(uid)) {
//            return Result.error(ResultCodeEnum.Authority_ERROR);
//        }
        PageInfo<Plan> plan = this.planService.getAllPlanByPersonPage(uid,pageNum,pageSize);
        return Result.success(plan);
    }

    @GetMapping("/getAllPlanByDepartmentPage")
//    @PreAuthorize("hasAuthority('plan:getAllPlanByDepartmentPage')")
    public Result getAllPlanByDepartmentPage(@RequestParam String tid, @RequestParam String did,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize){
        String isopen = null;
        PageInfo<Plan> plans = null;
        // 身份验证
        List<String> permissions = TokenUtils.getPermissions();
        User user = TokenUtils.getCurrentUser();
//        if (user.getRid().equals(RoleEnum.DEPARTMENT_ORDINARY.code)){
//            isopen = "1";
//        }
//        if(user.getRid().equals(RoleEnum.COLLEGE_LEADER.code) || user.getRid().equals(RoleEnum.SUPER_ADMIN.code)){
//            plans = this.planService.getAllPlanByTenantPage(user.getTid(),pageNum,pageSize);
//            return Result.success(plans);
//        }
        if(permissions.contains("plan:operate:get:ordinary")){
            isopen = "1";
        }
        if(permissions.contains("plan:operate:get:college")){
            plans = this.planService.getAllPlanByTenantPage(user.getTid(),pageNum,pageSize);
            return Result.success(plans);
        }
        plans = this.planService.getAllPlanByDepartmentPage(tid, did,isopen,pageNum,pageSize);
        return Result.success(plans);
    }

    //大屏右上角
    @GetMapping("/screen/getAllPlanByTenantAndTime")
    public Result screenGetAllPlanByTenantAndTime(@RequestParam String tid,
                                                  @RequestParam(defaultValue = "10") String time){

        List<Plan> plan = this.planService.screenGetAllPlanByTenantAndTime(tid,time);
        return Result.success(plan);
    }


    /**
     * 导出计划数据（前端传递ids列表+tid）
     */
    @PostMapping("/export")
//    @PreAuthorize("hasAuthority('plan:export')")
    public void exportPlan(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        try {
            // 接收前端参数
            List<String> ids = (List<String>) params.get("ids");
            String tid = (String) params.get("tid");

            // 参数校验
            if (ids == null || ids.isEmpty() || tid == null || tid.trim().isEmpty()) {
                throw new RuntimeException("参数异常：任务ID列表和学院ID不能为空");
            }

            // 执行导出
            planService.exportPlanData(ids, tid, response);
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

}
