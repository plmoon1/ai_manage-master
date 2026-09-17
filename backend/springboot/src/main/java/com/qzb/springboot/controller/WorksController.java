package com.qzb.springboot.controller;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.qzb.springboot.common.Result;
import com.qzb.springboot.common.enums.MeetingConstants;
import com.qzb.springboot.common.enums.OpenEnum;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.Works;
import com.qzb.springboot.entity.WorksFiles;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.exception.AuthorityException;
import com.qzb.springboot.exception.PermissionException;
import com.qzb.springboot.mapper.WorksMapper;
import com.qzb.springboot.service.WorksService;
import com.qzb.springboot.utils.AuthCheckUtils;
import com.qzb.springboot.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/works")
public class WorksController {
    @Resource
    private WorksService worksService;
    @Resource
    private WorksMapper worksMapper;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GetMapping("/getAllWorksByPerson")
    public Result getAllWorksByPerson(@RequestParam String uid){
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if(!user.getId().equals(uid)) {
            return Result.error(ResultCodeEnum.Authority_ERROR);
        }
        List<Works> works = this.worksService.getAllWorksByPerson(uid);
        return Result.success(works);
    }

    @GetMapping("/getAllWorksByDepartment")
    public Result getAllWorksByDepartment(@RequestParam String tid, @RequestParam String did){
        String isopen = null;
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if (user.getRid().equals(RoleEnum.DEPARTMENT_ORDINARY.code)){
            isopen = "1";
        }
        List<Works> works = this.worksService.getAllWorksByDepartment(tid, did,isopen);
        return Result.success(works);
    }

    @GetMapping("/getAllWorksByTenant")
    public Result getAllWorksByTenant(@RequestParam String tid){
        List<Works> works = this.worksService.getAllWorksByTenant(tid);
        return Result.success(works);
    }

    //大屏右下角
    @GetMapping("/screen/getAllWorksByTenant")
    public Result screenGetAllWorksByTenant(@RequestParam String tid){
        List<Works> works = this.worksService.screenGetAllWorksByTenant(tid);
        return Result.success(works);
    }


    @GetMapping("/searchWorksByCondition")
    public Result searchWorksByCondition(Works works){
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if(!user.getRid().equals(RoleEnum.COLLEGE_LEADER.code)){
            // 可见范围为“私密”
            if (ObjectUtil.isNotNull(works.getIsopen()) && works.getIsopen().equals(OpenEnum.SECRETE.code)){
                if(ObjectUtil.isNull(works.getDid()) ){
                    works.setDid(user.getDid());
                }
                AuthCheckUtils.checkUserRoleForPlan(works.getDid());
            }
            // 可见范围没有选择，“公开”不用判断
            else if (ObjectUtil.isNull(works.getIsopen())) {
                if(ObjectUtil.isNotNull(works.getDid()) && works.getDid().equals(user.getDid()) && user.getRid().equals(RoleEnum.DEPARTMENT_LEADER.code)){
                    works.setIsopen(null);
                }else {
                    works.setIsopen("1");
                }
            }
        }
        // 领导可以查看所有科室信息
        List<Works> workss = this.worksService.searchWorksByCondition(works);
        return Result.success(workss);
    }

    @PostMapping("/addWorks")
    public Result addWorks(@RequestBody Works works){
        // 身份验证
//        AuthCheckUtils.checkUserRoleForPlan(works.getDid());
        if(!AuthCheckUtils.checkAuthority("works:operate:add")){
            throw new PermissionException(ResultCodeEnum.Authority_ERROR);
        }
        // 领导可以创建所有科室任务
        this.worksService.addWorks(works);
        return Result.success();
    }

    @PutMapping("/updateWorks")
    public Result updateWorks(@RequestBody Works works){
        // 身份验证
        String worksDid = this.worksMapper.getWorksDidById(works.getId());
//        AuthCheckUtils.checkUserRoleForPlan(worksDid);
        // 分管领导可以修改所有科室任务
        if(!AuthCheckUtils.checkAuthority("works:operate:update")){
            throw new PermissionException(ResultCodeEnum.Authority_ERROR);
        }
        User user = TokenUtils.getCurrentUser();
        if(!AuthCheckUtils.checkAuthority("works:operate:get:college") && !worksDid.equals(user.getDid())){
            throw new PermissionException(ResultCodeEnum.Authority_ERROR);
        }
        this.worksService.updateWorks(works);
        return Result.success();
    }

    /**
     * 通用查询接口
     * */
    @GetMapping("/getWorksDetail")
    public Result getWorksDetail(Works works){
        List<Works> workss = this.worksService.getWorksDetail(works);
        return Result.success(workss);
    }

    @GetMapping("/getWorksNumPrepareByPerson")
    public Result getWorksNumPrepareByPerson(@RequestParam String tid,
                                            @RequestParam String did,
                                            @RequestParam String uid){
        Integer num = this.worksService.getWorksNumPrepareByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getWorksNumCompleteByPerson")
    public Result getWorksNumCompleteByPerson(@RequestParam String tid,
                                             @RequestParam String did,
                                             @RequestParam String uid){
        Integer num = this.worksService.getWorksNumCompleteByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getWorksNumDoingByPerson")
    public Result getWorksNumDoingByPerson(@RequestParam String tid,
                                          @RequestParam String did,
                                          @RequestParam String uid){
        Integer num = this.worksService.getWorksNumDoingByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getWorksNumCancelByPerson")
    public Result getWorksNumCancelByPerson(@RequestParam String tid,
                                           @RequestParam String did,
                                           @RequestParam String uid){
        Integer num = this.worksService.getWorksNumCancelByPerson(tid,did,uid);
        return Result.success(num);
    }

    @GetMapping("/getWorksNumPrepareByDepartment")
    public Result getWorksNumPrepareByDepartment(@RequestParam String tid,
                                                @RequestParam String did){
        Integer num = this.worksService.getWorksNumPrepareByDepartment(tid,did);
        return Result.success(num);
    }

    @GetMapping("/getWorksNumCompleteByDepartment")
    public Result getWorksNumCompleteByDepartment(@RequestParam String tid,
                                                 @RequestParam String did){
        Integer num = this.worksService.getWorksNumCompleteByDepartment(tid,did);
        return Result.success(num);
    }

    @GetMapping("/getWorksNumDoingByDepartment")
    public Result getWorksNumDoingByDepartment(@RequestParam String tid,
                                              @RequestParam String did){

        Integer num = this.worksService.getWorksNumDoingByDepartment(tid,did);
        return Result.success(num);
    }

    @GetMapping("/getWorksNumCancelByDepartment")
    public Result getWorksNumCancelByDepartment(@RequestParam String tid,
                                               @RequestParam String did){
        Integer num = this.worksService.getWorksNumCancelByDepartment(tid,did);
        return Result.success(num);
    }

//    @PostMapping("/addWorksLog")
//    public Result addWorksLog(@RequestBody WorksLog worksLog){
//        this.worksService.addWorksLog(worksLog);
//        return Result.success();
//    }
//
//    @PutMapping("/updateWorksLog")
//    public Result updateWorksLog(@RequestBody WorksLog worksLog){
//        this.worksService.updateWorksLog(worksLog);
//        return Result.success();
//    }

    @DeleteMapping("/deleteWorksLog")
    public Result deleteWorksLog(@RequestParam String  plid){
        this.worksService.deleteWorksLog(plid);
        return Result.success();
    }

    @GetMapping("/getWorksLogByPid")
    public Result getWorksLogByPid(@RequestParam String pid){
        this.worksService.getWorksLogByPid(pid);
        return Result.success();
    }


    @PostMapping("/addWorksFile")
    public Result addWorksFile(@RequestBody WorksFiles worksFiles){
        // 身份验证
//        checkUserRoleForFileUpload
        String fileId = this.worksService.addWorksFile(worksFiles);
        return Result.success(fileId);
    }

    @GetMapping("/getWorksFiles")
    public Result getWorksFiles(@RequestParam String pid){
        List<WorksFiles> worksFilesList = this.worksService.getWorksFiles(pid);
        return Result.success(worksFilesList);
    }
    @DeleteMapping("/deleteWorksFiles")
    public Result deleteWorksFiles(@RequestParam String pfid){
        // 身份验证
//        String worksDid = worksMapper.getWorksDidByPfid(pfid);
//        checkUserRoleForWorksFileDelete(worksDid);
        this.worksService.deleteWorksFiles(pfid);
        return Result.success();
    }

    /**
     * 上传Excel并导入数据
     */
    @PostMapping("/import")
    public Result importWorks(@RequestParam("file") MultipartFile file) {
        try {
            this.worksService.importWorksData(file);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(MeetingConstants.FILE_INSERT_ERROR_CODE, e.getMessage());
        }
    }

    /**
     * 分页查询
     */
    @GetMapping("/searchWorksByConditionPage")
    public Result searchWorksByConditionPage(Works works,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if(!AuthCheckUtils.checkAuthority("works:operate:get:college")){
            // 可见范围为“私密”
            if (ObjectUtil.isNotNull(works.getIsopen()) && works.getIsopen().equals(OpenEnum.SECRETE.code)){
                if(ObjectUtil.isNull(works.getDid()) ){
                    works.setDid(user.getDid());
                }
                else {
                    if (AuthCheckUtils.checkAuthority("works:operate:get:ordinary"))
                        throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
                        // 科室领导仅操作本科室的任务
                    else if (AuthCheckUtils.checkAuthority("works:operate:get:office") && !works.getDid().equals(user.getDid())) {
                        throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
                    }
                }
            }
            // 可见范围没有选择，“公开”不用判断
            else if (ObjectUtil.isNull(works.getIsopen())) {
                if(ObjectUtil.isNotNull(works.getDid()) && works.getDid().equals(user.getDid()) && AuthCheckUtils.checkAuthority("works:operate:get:office")){
                    works.setIsopen(null);
                }else {
                    works.setIsopen("1");
                }
            }
        }
        // 执行业务
        PageInfo<Works> page = worksService.getWorksPage(works, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/getAllWorksByPersonPage")
    public Result getAllWorksByPersonPage(@RequestParam String uid,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize){
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if(!user.getId().equals(uid)) {
            return Result.error(ResultCodeEnum.Authority_ERROR);
        }
        PageInfo<Works> works = this.worksService.getAllWorksByPersonPage(uid,pageNum,pageSize);
        return Result.success(works);
    }

    @GetMapping("/getAllWorksByDepartmentPage")
    public Result getAllWorksByDepartmentPage(@RequestParam String tid, @RequestParam String did,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize){
        String isopen = null;
        PageInfo<Works> workss = null;
        // 身份验证
        User user = TokenUtils.getCurrentUser();
        if (user.getRid().equals(RoleEnum.DEPARTMENT_ORDINARY.code)){
            isopen = "1";
        }
        if(user.getRid().equals(RoleEnum.COLLEGE_LEADER.code) || user.getRid().equals(RoleEnum.SUPER_ADMIN.code)){
            workss = this.worksService.getAllWorksByTenantPage(user.getTid(),pageNum,pageSize);
            return Result.success(workss);
        }
        workss = this.worksService.getAllWorksByDepartmentPage(tid, did,isopen,pageNum,pageSize);
        return Result.success(workss);
    }

    @PostMapping("/export")
    public void exportWorks(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        try {
            // 接收前端参数
            List<String> ids = (List<String>) params.get("ids");
            String tid = (String) params.get("tid");

            // 参数校验
            if (ids == null || ids.isEmpty() || tid == null || tid.trim().isEmpty()) {
                throw new RuntimeException("参数异常：工作ID列表和学院ID不能为空");
            }

            // 执行导出
            worksService.exportWorksData(ids, tid, response);
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
