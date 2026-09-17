package com.qzb.springboot.utils;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.qzb.springboot.common.PlanConstants;
import com.qzb.springboot.common.WorkConstants;
import com.qzb.springboot.common.WorkConstants;
import com.qzb.springboot.common.enums.OpenEnum;
import com.qzb.springboot.entity.Department;
import com.qzb.springboot.entity.Works;
import com.qzb.springboot.entity.WorksExcel;
import cn.hutool.core.date.DateUtil;
import com.qzb.springboot.entity.User;
import com.qzb.springboot.mapper.DepartmentMapper;
import com.qzb.springboot.mapper.WorksMapper;
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
 * 逐行读取Excel数据并转换为Works实体
 */
@Slf4j
@Component
public class WorksExcelListener extends AnalysisEventListener<WorksExcel> {

    /** 批量插入阈值（避免内存溢出） */
    private static final int BATCH_SIZE = 100;
    /** 临时存储读取的数据 */
    private List<Works> worksList = new ArrayList<>(BATCH_SIZE);
    private StringBuilder estr = new StringBuilder();
    /**
     * 工作类型路径索引（完整路径 -> id）
     * 2026-08-17 任务类型支持同名叶子（不同父节点），导入时按完整路径消歧
     * 监听器为单例@Component，索引由WorksService在每次导入入口构建并传入，用完即清
     */
    private SysdictService.DictPathIndex typePathIndex;

    @Resource
    private WorksMapper worksMapper;
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
    public void invoke(WorksExcel excel, AnalysisContext context) {
        log.info("读取到Excel数据：{}", excel);
        String row = "第" + context.readRowHolder().getRowIndex().toString() + "行数据：";
        // 转换DTO为Works实体
        Works works = new Works();
        BeanUtils.copyProperties(excel, works);
        // 获取当前用户
        User user = TokenUtils.getCurrentUser();
        if(ObjectUtil.isNull(user) || ObjectUtil.isEmpty(user)){
            estr.append(WorkConstants.USER_ERROR);
        }
        // 0. 格式化开始时间和结束时间
//        works.setStartTime(TimeUtils.formatToYyyyMmDd(works.getStartTime()));
//        works.setEndTime(TimeUtils.formatToYyyyMmDd(works.getEndTime()));
        try{
            if (ObjectUtil.isNull(works.getStartTime())){
                estr.append("\n" + row + WorkConstants.START_TIME_MISSING);
            }else{
                works.setStartTime(TimeUtils.formatToYyyyMmDd(works.getStartTime()));
            }
        }catch (Exception e){
//            planList.clear();
            estr.append("\n" + row + e.getMessage());
//            throw new RuntimeException(row + e.getMessage());
        }
        try{
            if (ObjectUtil.isNull(works.getEndTime())){
                estr.append("\n" + row + WorkConstants.END_TIME_MISSING);
            }else{
                works.setEndTime(TimeUtils.formatToYyyyMmDd(works.getEndTime()));
            }
        }catch (Exception e){
//            planList.clear();
            estr.append("\n" + row + e.getMessage());
//            throw new RuntimeException(row + e.getMessage());
        }

        // 1. 生成创建时间与更新时间
//        works.setCreateTime(DateUtil.today()); // （yyyy-MM-dd）
        String currentTime = TimeUtils.getCurrentTime();
        works.setCreateTime(currentTime); // （yyyy-MM-dd HH:mm:ss）
        works.setUpdateTime(currentTime);
        // 2. 处理tid
        works.setTid(user.getTid());
//        works.setTid("1");
        // 3. 处理uid（当前登录用户ID）
        works.setUid(user.getId());
//        works.setUid("202520701282");
        // 15. 处理任务名称
        if (ObjectUtil.isNull(works.getName())){
            estr.append("\n" + row + WorkConstants.NAME_ERROR);
        }
        // 16. 处理任务详情
        if (ObjectUtil.isNull(works.getDetail())){
            estr.append("\n" +row + WorkConstants.DETAIL_ERROR);
        }

        // 4. 处理did
//        Department department = this.departmentMapper.getDepartmentByDid(works.getDid(),"1");
        if (ObjectUtil.isNull(works.getDid())){
            estr.append("\n" + row + WorkConstants.DID_MISSING);
        }else{
            Department department = this.departmentMapper.getDepartmentByDid(works.getDid(),user.getTid());
            if (ObjectUtil.isNull(department) || ObjectUtil.isEmpty(department)){
//            planList.clear();
                estr.append("\n" +row + works.getDid() + WorkConstants.DEPARTMENT_ERROR);
//            throw new RuntimeException(row+plan.getDid() + WorkConstants.DEPARTMENT_ERROR);
            }else {
                works.setDid(department.getId());
            }
        }

        // 5. 处理delete
        works.setIsdelete("0");
        // 6. 处理id
        String uuid = UUID.randomUUID().toString();
        works.setId(uuid);
        // 7. 处理attribute
//        String attribute = sysdictMapper.getIdByValue(user.getTid(),"attribute",works.getAttribute());
//        works.setAttribute(attribute);
        // 8. 处理cycle
//        String cycle = sysdictMapper.getIdByValue(user.getTid(),"cycle",works.getCycle());
//        works.setCycle(cycle);
        // 9. 处理type
        // 2026-08-17 任务类型支持同名叶子（不同父节点），按完整分类路径解析：
        // 精确匹配完整路径（如 国际合作与社会服务/社会服务）；未精确命中时按唯一后缀匹配（兼容只填叶子名）；
        // 存在多个同名后缀时报歧义错误，提示候选完整路径
        if (ObjectUtil.isNull(works.getType())){
            estr.append("\n" +row + WorkConstants.TYPE_MISSING );
        }else{
            try {
                String type = resolveTypePath(user.getTid(), works.getType());
                works.setType(type);
            } catch (Exception e) {
                estr.append("\n" + row + e.getMessage());
            }
        }
        // 10.处理imp
//        String imp = sysdictMapper.getIdByValue(user.getTid(),"imp",works.getImp());
//        works.setImp(imp);
        // 11.处理status
        if(ObjectUtil.isNull(works.getStatus())){
            estr.append("\n" +row + WorkConstants.STATUS_MISSING);
        }else{
            String status = sysdictMapper.getIdByValue(user.getTid(),"status",works.getStatus());
            if (ObjectUtil.isNull(status) || ObjectUtil.isEmpty(status)){
                estr.append("\n" +row+works.getStatus() + WorkConstants.STATUS_ERROR);
            }else{
                works.setStatus(status);
            }

        }
        // 12. 处理可见范围
        if(ObjectUtil.isNull(works.getIsopen())){
            estr.append("\n" +row + WorkConstants.ISOPEN_MISSING);
        }else{
            String isopen = works.getIsopen().equals("公开")? "1" : "0";
            if (ObjectUtil.isNull(isopen) || ObjectUtil.isEmpty(isopen)){
                estr.append("\n" +row+ WorkConstants.ISOPEN_ERROR);
            }else{
                works.setIsopen(isopen);
            }
        }
        // 13. 处理驾驶舱展示
        if(ObjectUtil.isNull(works.getIsshow())){
            estr.append("\n" +row+ WorkConstants.ISSSHOW_MISSING);
        }else{
            String isshow = works.getIsshow().equals("是")? "1" : "0";
            if (ObjectUtil.isNull(isshow) || ObjectUtil.isEmpty(isshow)){
                estr.append("\n" +row+ WorkConstants.ISSHOW_ERROR);
            }else{
                works.setIsshow(isshow);
            }
        }
        // 14. 处理负责人
        if (ObjectUtil.isNull(works.getHead())){
            estr.append("\n" +row + PlanConstants.HEAD_ERROR);
        }
        // 添加到临时列表
        if(ObjectUtil.isNull(estr) || ObjectUtil.isEmpty(estr)){
            // 添加到临时列表
            worksList.add(works);
        }else{
            worksList.clear();
        }
        // 达到批量阈值则插入数据库
        if (worksList.size() >= BATCH_SIZE) {
            saveData();
            // 清空列表
            worksList.clear();
        }
    }

    /**
     * 解析工作类型文本为字典ID
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
    public void doAfterAllAnalysed(AnalysisContext context) {
        if(ObjectUtil.isNotEmpty(estr)){
            worksList.clear();
            String error = estr.toString();
            estr.setLength(0);
            throw new RuntimeException(error);
        }
        // 插入剩余数据
        saveData();
        log.info("Excel数据读取完成，共处理{}条数据", worksList.size());
        //防止数据重复
        worksList.clear();
    }

    /**
     * 批量插入数据库
     */
    private void saveData() {
        if (!worksList.isEmpty()) {
            try {
                if(ObjectUtil.isNotEmpty(estr)){
                    worksList.clear();
                    String error = estr.toString();
                    estr.setLength(0);
                    throw new RuntimeException(error);
                }
                worksMapper.batchInsertWorks(worksList);
                log.info("批量插入{}条计划数据成功", worksList.size());
            } catch (Exception e) {
                log.error("批量插入计划数据失败", e);
                worksList.clear();  // 清除缓存数据，避免因为一次失败而导致后面的数据全部插入失败
                throw new RuntimeException("数据导入失败：" + e.getMessage());
            }
        }
    }


}
