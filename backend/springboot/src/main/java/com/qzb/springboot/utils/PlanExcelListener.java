package com.qzb.springboot.utils;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.qzb.springboot.common.PlanConstants;
import com.qzb.springboot.common.enums.OpenEnum;
import com.qzb.springboot.entity.Department;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.PlanExcel;
import cn.hutool.core.date.DateUtil;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.mapper.DepartmentMapper;
import com.qzb.springboot.mapper.PlanMapper;
import com.qzb.springboot.mapper.SysdictMapper;
import com.qzb.springboot.service.SysdictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Excel导入监听器
 * 逐行读取Excel数据并转换为Plan实体
 */
@Slf4j
@Component
public class PlanExcelListener extends AnalysisEventListener<PlanExcel> {

    /** 批量插入阈值（避免内存溢出） */
    private static final int BATCH_SIZE = 100;
    /** 临时存储读取的数据 */
    private List<Plan> planList = new ArrayList<>(BATCH_SIZE);
    /** 批量存储异常信息 */
    private StringBuilder estr = new StringBuilder();
    /**
     * 任务类型路径索引（完整路径 -> id）
     * 2026-08-17 任务类型支持同名叶子（不同父节点），导入时按完整路径消歧
     * 监听器为单例@Component，索引由PlanService在每次导入入口构建并传入，用完即清
     */
    private SysdictService.DictPathIndex typePathIndex;

    @Resource
    private PlanMapper planMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private SysdictMapper sysdictMapper;
    @Resource
    private SysdictService sysdictService;

    public void setTypePathIndex(SysdictService.DictPathIndex typePathIndex) {
        this.typePathIndex = typePathIndex;
    }

    public void clearTypePathIndex() {
        this.typePathIndex = null;
    }


    /**
     * 逐行处理Excel数据
     */
    @Override
    public void invoke(PlanExcel excel, AnalysisContext context) {
        log.info("读取到Excel数据：{}", excel);
        String row = "第" + context.readRowHolder().getRowIndex().toString() + "行数据：";
        // 转换DTO为Plan实体
        Plan plan = new Plan();
        BeanUtils.copyProperties(excel, plan);
        // 获取当前用户
        User user = TokenUtils.getCurrentUser();
        if(ObjectUtil.isNull(user) || ObjectUtil.isEmpty(user)){
//            planList.clear();
            estr.append(PlanConstants.USER_ERROR);
//            throw new RuntimeException(PlanConstants.USER_ERROR);
        }
        // 0. 格式化开始时间和结束时间
        try{
            if (ObjectUtil.isNull(plan.getStartTime())){
                estr.append("\n" + row + PlanConstants.START_TIME_MISSING);
            }else{
                plan.setStartTime(TimeUtils.formatToYyyyMmDd(plan.getStartTime()));
            }
        }catch (Exception e){
//            planList.clear();
            estr.append("\n" + row + e.getMessage());
//            throw new RuntimeException(row + e.getMessage());
        }
        try{
            if (ObjectUtil.isNull(plan.getEndTime())){
                estr.append("\n" + row + PlanConstants.END_TIME_MISSING);
            }else{
                plan.setEndTime(TimeUtils.formatToYyyyMmDd(plan.getEndTime()));
            }
        }catch (Exception e){
//            planList.clear();
            estr.append("\n" + row + e.getMessage());
//            throw new RuntimeException(row + e.getMessage());
        }


        // 1. 生成创建时间与更新时间
//        plan.setCreateTime(DateUtil.today()); // （yyyy-MM-dd）
        String currentTime = TimeUtils.getCurrentTime();
        plan.setCreateTime(currentTime); // （yyyy-MM-dd HH:mm:ss）
        plan.setUpdateTime(currentTime);
        // 2. 处理tid
        plan.setTid(user.getTid());
//        plan.setTid("1");
        // 3. 处理uid（当前登录用户ID）
        plan.setUid(user.getId());
//        plan.setUid("202520701282");
        // 15. 处理任务名称
        if (ObjectUtil.isNull(plan.getName())){
//            planList.clear();
            estr.append("\n" + row + PlanConstants.NAME_ERROR);
//            throw new RuntimeException(row + PlanConstants.NAME_ERROR);
        }
        // 16. 处理任务详情
        if (ObjectUtil.isNull(plan.getDetail())){
//            planList.clear();
            estr.append("\n" +row + PlanConstants.DETAIL_ERROR);
//            throw new RuntimeException(row + PlanConstants.DETAIL_ERROR);
        }

        // 4. 处理did
        if (ObjectUtil.isNull(plan.getDid())){
            estr.append("\n" + row + PlanConstants.DID_MISSING);
        }else{
            Department department = this.departmentMapper.getDepartmentByDid(plan.getDid(),user.getTid());
            if (ObjectUtil.isNull(department) || ObjectUtil.isEmpty(department)){
//            planList.clear();
                estr.append("\n" +row + plan.getDid() + PlanConstants.DEPARTMENT_ERROR);
//            throw new RuntimeException(row+plan.getDid() + PlanConstants.DEPARTMENT_ERROR);
            }else {
                plan.setDid(department.getId());
            }
        }

//        Department department = this.departmentMapper.getDepartmentByDid(plan.getDid(),"1");
        // 5. 处理delete
        plan.setIsdelete("0");
        // 6. 处理id
        String uuid = UUID.randomUUID().toString();
        plan.setId(uuid);
        // 7. 处理attribute
        if (ObjectUtil.isNull(plan.getAttribute())){
            estr.append("\n" +row + PlanConstants.ATTRIBUTE_MISSING );
        }else{
            String attribute = sysdictMapper.getIdByValue(user.getTid(),"attribute",plan.getAttribute());
            if (ObjectUtil.isNull(attribute) || ObjectUtil.isEmpty(attribute)){
//            planList.clear();
                estr.append("\n" +row + plan.getAttribute() + PlanConstants.ATTRIBUTE_ERROR);
//            throw new RuntimeException(row+plan.getAttribute() + PlanConstants.ATTRIBUTE_ERROR);
            }else{
                plan.setAttribute(attribute);
            }
        }

        // 8. 处理cycle
//        String cycle = sysdictMapper.getIdByValue(user.getTid(),"cycle",plan.getCycle());
//        plan.setCycle(cycle);
        // 9. 处理type
        // 2026-08-17 任务类型支持同名叶子（不同父节点），按完整分类路径解析：
        // 精确匹配完整路径（如 国际合作与社会服务/社会服务）；未精确命中时按唯一后缀匹配（兼容只填叶子名）；
        // 存在多个同名后缀时报歧义错误，提示候选完整路径
        if (ObjectUtil.isNull(plan.getType())){
            estr.append("\n" +row + PlanConstants.TYPE_MISSING );
        }else{
            try {
                String type = resolveTypePath(user.getTid(), plan.getType());
                plan.setType(type);
            } catch (Exception e) {
                estr.append("\n" + row + e.getMessage());
            }

        }

        // 10.处理imp
        if (ObjectUtil.isNull(plan.getImp())){
            estr.append("\n" +row + PlanConstants.IMP_MISSING);
        }else{
            String imp = sysdictMapper.getIdByValue(user.getTid(),"imp",plan.getImp());
            if (ObjectUtil.isNull(imp) || ObjectUtil.isEmpty(imp)){
//            planList.clear();
                estr.append("\n" +row+plan.getImp() + PlanConstants.IMP_ERROR);
//            throw new RuntimeException(row+plan.getImp() + PlanConstants.IMP_ERROR);
            }else{
                plan.setImp(imp);
            }

        }

        // 11.处理status
        if(ObjectUtil.isNull(plan.getStatus())){
            estr.append("\n" +row + PlanConstants.STATUS_MISSING);
        }else{
            String status = sysdictMapper.getIdByValue(user.getTid(),"status",plan.getStatus());
            if (ObjectUtil.isNull(status) || ObjectUtil.isEmpty(status)){
//            planList.clear();
                estr.append("\n" +row+plan.getStatus() + PlanConstants.STATUS_ERROR);
//            throw new RuntimeException(row+plan.getStatus() + PlanConstants.STATUS_ERROR);
            }else{
                plan.setStatus(status);
            }

        }

        // 12. 处理可见范围
        if(ObjectUtil.isNull(plan.getIsopen())){
            estr.append("\n" +row + PlanConstants.ISOPEN_MISSING);
        }else{
            String isopen = plan.getIsopen().equals(OpenEnum.OPEN.status)? "1" : "0";
            if (ObjectUtil.isNull(isopen) || ObjectUtil.isEmpty(isopen)){
//            planList.clear();
                estr.append("\n" +row+ PlanConstants.ISOPEN_ERROR);
//            throw new RuntimeException(row+plan.getStatus() + PlanConstants.STATUS_ERROR);
            }else{
                plan.setIsopen(isopen);
            }
        }

        // 13. 处理驾驶舱展示
        if(ObjectUtil.isNull(plan.getIsshow())){
            estr.append("\n" +row+ PlanConstants.ISSSHOW_MISSING);
        }else{
            String isshow = plan.getIsshow().equals("是")? "1" : "0";
            if (ObjectUtil.isNull(isshow) || ObjectUtil.isEmpty(isshow)){
//            planList.clear();
                estr.append("\n" +row+ PlanConstants.ISSHOW_ERROR);
//            throw new RuntimeException(row+plan.getStatus() + PlanConstants.STATUS_ERROR);
            }else{
                plan.setIsshow(isshow);
            }
        }
        // 14. 处理负责人
        if (ObjectUtil.isNull(plan.getHead())){
//            planList.clear();
            estr.append("\n" +row + PlanConstants.HEAD_ERROR);
//            throw new RuntimeException(row + PlanConstants.HEAD_ERROR);
        }
        if(ObjectUtil.isNull(estr) || ObjectUtil.isEmpty(estr)){
            // 添加到临时列表
            planList.add(plan);
        }else{
            planList.clear();
        }
        // 达到批量阈值则插入数据库
        if (planList.size() >= BATCH_SIZE) {
            saveData();
            // 清空列表
            planList.clear();
        }
    }

    /**
     * 解析任务类型文本为字典ID
     * 优先使用导入入口传入的路径索引；若未传入（异常场景）则即时构建，保证单行仍可解析
     */
    private String resolveTypePath(String tid, String typeText) {
        SysdictService.DictPathIndex index = this.typePathIndex;
        if (index == null) {
            index = sysdictService.buildTypePathIndex(tid);
        }
        return sysdictService.resolveByIndex(index, typeText);
    }

    /**
     * Excel读取完成后，处理剩余数据
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {        if(ObjectUtil.isNotEmpty(estr)){
            planList.clear();
            String error = estr.toString();
            estr.setLength(0);
            throw new RuntimeException(error);
        }
        // 插入剩余数据
        saveData();
        log.info("Excel数据读取完成，共处理{}条数据", planList.size());
        //防止数据重复
        planList.clear();
    }

    /**
     * 批量插入数据库
     */
    private void saveData() {
        if (!planList.isEmpty()) {
            try {
                if(ObjectUtil.isNotEmpty(estr)){
                    planList.clear();
                    String error = estr.toString();
                    estr.setLength(0);
                    throw new RuntimeException(error);
                }
                planMapper.batchInsertPlan(planList);
                log.info("批量插入{}条计划数据成功", planList.size());
            } catch (Exception e) {
                log.error("批量插入计划数据失败", e);
                planList.clear();  // 清除缓存数据，避免因为一次失败而导致后面的数据全部插入失败
                throw new RuntimeException("数据导入失败：" + e.getMessage());
            }
        }
    }


}
