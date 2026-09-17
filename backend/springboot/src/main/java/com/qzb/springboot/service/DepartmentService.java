package com.qzb.springboot.service;

import com.qzb.springboot.entity.Department;
import com.qzb.springboot.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    @Resource
    DepartmentMapper departmentMapper;

    public List<Department> getDepartmentByTid(String tid){
        return this.departmentMapper.getDepartmentByTid(tid);
    }

    public void addDepartment(Department department){
        String uuid = UUID.randomUUID().toString();
        department.setId(uuid);
        this.departmentMapper.addDepartment(department);
    }
    public void updateDepartment(Department department){
        this.departmentMapper.updateDepartment(department);
    }

    public void deleteDepartment(Department department){
        this.departmentMapper.deleteDepartment(department);
    }
}
