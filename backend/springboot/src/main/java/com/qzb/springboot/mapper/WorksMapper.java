package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.Works;
import com.qzb.springboot.entity.WorksFiles;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

public interface WorksMapper {
    void addWorks(Works works);
    void updateWorks(Works works);

    List<Works> getAllWorksByPerson(String uid);
    List<Works> getAllWorksByDepartment(String tid,String did,String isopen);
    List<Works> getAllWorksByTenant(String tid);
    Integer getWorksNumPrepareByPerson(String tid, String did, String uid);
    Integer getWorksNumCompleteByPerson(String tid,String did,String uid);
    Integer getWorksNumDoingByPerson(String tid,String did,String uid);
    Integer getWorksNumCancelByPerson(String tid,String did,String uid);
    Integer getWorksNumPrepareByDepartment(String tid,String did);
    Integer getWorksNumCompleteByDepartment(String tid,String did);
    Integer getWorksNumDoingByDepartment(String tid,String did);
    Integer getWorksNumCancelByDepartment(String tid,String did);
    List<Works> getWorksDetail(Works works);
//    void addWorksLog(WorksLog worksLog);
//    void updateWorksLog(WorksLog worksLog);
    void deleteWorksLog(String plid);
    void getWorksLogByPid(String pid);
    List<Works> searchWorksByCondition(Works works);

    void addWorksFile(WorksFiles worksFiles);

    List<WorksFiles> getWorksFiles(String pid);

    void deleteWorksFiles(String pfid);
    /**
     * 批量插入计划数据
     */
    @Insert({
            "<script>",
            "INSERT INTO works (id, name, detail,  start_time, end_time, tid, did, uid, type, head,  status, achievement, create_time, `isdelete`,`isopen`,update_time,isshow)",
            "VALUES ",
            "<foreach collection='list' item='works' separator=','>",
            "(#{works.id}, #{works.name}, #{works.detail},  #{works.startTime}, #{works.endTime}, #{works.tid}, #{works.did}, #{works.uid}, #{works.type}, #{works.head},  #{works.status}, #{works.achievement}, #{works.createTime}, #{works.isdelete},#{works.isopen},#{works.updateTime},#{works.isshow})",
            "</foreach>",
            "</script>"
    })
    int batchInsertWorks(List<Works> worksList);


    String getWorksDidByFileName(String file);

    String getWorksDidByPfid(String pfid);
    String getWorksDidById(String pid);

    List<Works> screenGetAllWorksByTenant(String tid);

    List<Works> selectWorksByIdsAndTid(List<String> ids, String tid);
}
