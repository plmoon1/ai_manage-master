package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.Department;

import java.util.List;

public interface DepartmentMapper {
    List<Department> getDepartmentByTid(String tid);
    void addDepartment(Department department);
    void updateDepartment(Department department);
    void deleteDepartment(Department department);

    Department getDepartmentByDid(String name,String tid);
}
