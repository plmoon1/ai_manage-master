package com.qzb.springboot.utils;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.qzb.springboot.entity.*;
import com.qzb.springboot.mapper.DepartmentMapper;
import com.qzb.springboot.mapper.PlanMapper;
import com.qzb.springboot.mapper.RoleMapper;
import com.qzb.springboot.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Slf4j
@Component
public class UserExcelListener extends AnalysisEventListener<UserExcel> {
    /** 批量插入阈值（避免内存溢出） */
    private static final int BATCH_SIZE = 100;
    /** 临时存储读取的数据 */
    private List<User> userList = new ArrayList<>(BATCH_SIZE);
    /** 获得用户数据信息 */

    @Resource
    private UserMapper userMapper;
    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private RoleMapper roleMapper;
    /**
     * 逐行处理Excel数据
     */
    @Override
    public void invoke(UserExcel excel, AnalysisContext context) {
        log.info("读取到Excel数据：{}", excel);
        // 转换DTO为Plan实体
        User user = new User();
        BeanUtils.copyProperties(excel, user);

        User currentUser = TokenUtils.getCurrentUser();

        // 1. 处理tid
        user.setTid(currentUser.getTid());
//        user.setTid("1");
        // 2. 处理did
        Department department = this.departmentMapper.getDepartmentByDid(user.getDid(),currentUser.getTid());
//        Department department = this.departmentMapper.getDepartmentByDid(user.getDid(),"1");
        user.setDid(department.getId());
        // 3. 处理状态
        user.setStatus("正常");
        // 4. 处理rid
        String rid = this.roleMapper.getRoleIdByName(user.getRid());
        user.setRid(rid);
        // 5. 处理密码，统一设置成教职工号
        user.setPassword(user.getId());
        // 6. 处理创建时间和更新时间
        user.setCreateTime(TimeUtils.getCurrentTime());
        user.setUpdateTime(TimeUtils.getCurrentTime());
        // 添加到临时列表
        userList.add(user);
        // 达到批量阈值则插入数据库
        if (userList.size() >= BATCH_SIZE) {
            saveData();
            // 清空列表
            userList.clear();
        }
    }

    /**
     * Excel读取完成后，处理剩余数据
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 插入剩余数据
        saveData();
        log.info("Excel数据读取完成，共处理{}条数据", userList.size());
        //防止数据重复
        userList.clear();
    }

    /**
     * 批量插入数据库
     */
    private void saveData() {
        if (!userList.isEmpty()) {
            try {
                userMapper.batchInsertUser(userList);
                log.info("批量插入{}条计划数据成功", userList.size());
            } catch (Exception e) {
                log.error("批量插入计划数据失败", e);
                userList.clear();  // 清除缓存数据，避免因为一次失败而导致后面的数据全部插入失败
                throw new RuntimeException("数据导入失败：" + e.getMessage());
            }
        }
    }

    public void exceptionClear(){
        userList.clear();  // 清除缓存数据，避免因为一次失败而导致后面的数据全部插入失败
    }

}
