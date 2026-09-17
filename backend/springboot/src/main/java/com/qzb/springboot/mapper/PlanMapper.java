package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.PlanFiles;
import com.qzb.springboot.entity.PlanLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface PlanMapper {

    void addPlan(Plan plan);

    /** 各负责人的任务数聚合(降序)，用于AI"按负责人筛选"时给出真实有任务的负责人清单 */
    List<Map<String, Object>> countByHead(@Param("tid") String tid, @Param("limit") int limit);
    void updatePlan(Plan plan);

    List<Plan> getAllPlanByPerson(String uid);
    List<Plan> getAllPlanByDepartment(String tid,String did,String isopen);
    List<Plan> getAllPlanByTenant(String tid);
    Integer getPlanNumPrepareByPerson(String tid, String did, String uid);
    Integer getPlanNumCompleteByPerson(String tid,String did,String uid);
    Integer getPlanNumDoingByPerson(String tid,String did,String uid);
    Integer getPlanNumCancelByPerson(String tid,String did,String uid);
    Integer getPlanNumPrepareByDepartment(String tid,String did);
    Integer getPlanNumCompleteByDepartment(String tid,String did);
    Integer getPlanNumDoingByDepartment(String tid,String did);
    Integer getPlanNumCancelByDepartment(String tid,String did);
    List<Plan> getPlanDetail(Plan plan);
    void addPlanLog(PlanLog planLog);
    void updatePlanLog(PlanLog planLog);
    void deletePlanLog(String plid);
    void getPlanLogByPid(String pid);
    List<Plan>  searchPlanByCondition(Plan plan);

    void addPlanFile(PlanFiles planFiles);

    List<PlanFiles> getPlanFiles(String pid);

    void deletePlanFiles(String pfid);
    /**
     * 批量插入计划数据
     */
    @Insert({
            "<script>",
            "INSERT INTO plan (id, name, detail, attribute,  start_time, end_time, tid, did, uid, type, head, imp, status, achievement, create_time, `isdelete`,`isopen`,update_time,isshow)",
            "VALUES ",
            "<foreach collection='list' item='plan' separator=','>",
            "(#{plan.id}, #{plan.name}, #{plan.detail}, #{plan.attribute},  #{plan.startTime}, #{plan.endTime}, #{plan.tid}, #{plan.did}, #{plan.uid}, #{plan.type}, #{plan.head}, #{plan.imp}, #{plan.status}, #{plan.achievement}, #{plan.createTime}, #{plan.isdelete},#{plan.isopen},#{plan.updateTime},#{plan.isshow})",
            "</foreach>",
            "</script>"
    })
    int batchInsertPlan(List<Plan> planList);


    String getPlanDidByFileName(String file);

    String getPlanDidByPfid(String pfid);
    String getPlanDidById(String pid);

    List<Plan> screenGetAllPlanByTenant(String tid);

    List<Plan> screenGetAllPlanByTenantAndTime(String tid, String time);

    /**
     * 根据任务ID列表和学院ID查询计划数据（导出用）
     */
//    @Select({
//            "<script>",
//            "SELECT id, name, detail, attribute, start_time, end_time, tid, did, uid, type, head, imp,  status, achievement, create_time, isdelete ,isopen,isshow",
//            "FROM plan ",
//            "WHERE id IN ",
//            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
//            "#{id}",
//            "</foreach>",
//            "AND tid = #{tid} ",
//            "AND `delete` = 0", // 只导出未删除的数据
//            "</script>"
//    })
    List<Plan> selectPlanByIdsAndTid(@Param("ids") List<String> ids, @Param("tid") String tid);
}
