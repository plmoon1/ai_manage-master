package com.qzb.springboot.utils;

import cn.hutool.core.util.ObjectUtil;
import com.qzb.springboot.common.enums.OpenEnum;
import com.qzb.springboot.common.enums.ResultCodeEnum;
import com.qzb.springboot.common.enums.RoleEnum;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.exception.AuthorityException;
import com.qzb.springboot.mapper.PlanMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class AuthCheckUtils {
    @Resource
    PlanMapper planMapper;

    public static void checkUserRoleForPlan(String planDid){
        User user = TokenUtils.getCurrentUser();
        List<String> permissions = TokenUtils.getPermissions();
        // 科室人员不可操作私密任务
        if (permissions.contains("plan:operate:get:ordinary") )
            throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
            // 科室领导仅操作本科室的任务
        else if (permissions.contains("plan:operate:get:office")  && !planDid.equals(user.getDid())) {
            throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
        }
    }

    public static void checkUserRoleForPlanImport(){
        User user = TokenUtils.getCurrentUser();
        // 科室人员不可操作import
        if (user.getRid().equals(RoleEnum.DEPARTMENT_ORDINARY.code) )
            throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
    }

    public static void checkUserRoleForFileDelete(String planDid){
        User user = TokenUtils.getCurrentUser();
        // 科室人员不可操作删除
        if (user.getRid().equals(RoleEnum.DEPARTMENT_ORDINARY.code) )
            throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
        else if (user.getRid().equals(RoleEnum.DEPARTMENT_LEADER.code)  && !planDid.equals(user.getDid())) {
            throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
        }
    }

    public static void checkUserRoleForPlanFileDelete(String planDid){
        User user = TokenUtils.getCurrentUser();
        // 科室人员不可操作删除文件
        if (user.getRid().equals(RoleEnum.DEPARTMENT_ORDINARY.code) )
            throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
        else if (user.getRid().equals(RoleEnum.DEPARTMENT_LEADER.code)  && !planDid.equals(user.getDid())) {
            throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
        }
    }

    public static boolean checkAuthority(String auth){
        List<String> permissions = TokenUtils.getPermissions();
        return permissions.contains(auth);
    }

    /**
     * 任务查询的数据范围控制（供 /plan/searchPlanByConditionPage 与 AI 工具共用）
     * 无 plan:operate:get:college 权限时：
     * - 查“私密”任务：科室人员不可跨科室查私密任务
     * - 未指定可见范围：仅能看到公开任务（本科室领导可看本科室全部）
     */
    public static void applyPlanQueryScope(Plan plan){
        if(!checkAuthority("plan:operate:get:college")){
            User user = TokenUtils.getCurrentUser();
            // 可见范围为“私密”
            if (ObjectUtil.isNotNull(plan.getIsopen()) && plan.getIsopen().equals(OpenEnum.SECRETE.code)){
                if(ObjectUtil.isNull(plan.getDid()) ){
                    plan.setDid(user.getDid());
                }
                else {
                    if (checkAuthority("plan:operate:get:ordinary"))
                        throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
                        // 科室领导仅操作本科室的任务
                    else if (checkAuthority("plan:operate:get:office") && !plan.getDid().equals(user.getDid())) {
                        throw new AuthorityException(ResultCodeEnum.Authority_ERROR);
                    }
                }
            }
            // 可见范围没有选择，“公开”不用判断
            else if (ObjectUtil.isNull(plan.getIsopen())) {
                if(ObjectUtil.isNotNull(plan.getDid()) && plan.getDid().equals(user.getDid()) && checkAuthority("plan:operate:get:office")){
                    plan.setIsopen(null);
                }else {
                    plan.setIsopen("1");
                }
            }
        }
    }


}
