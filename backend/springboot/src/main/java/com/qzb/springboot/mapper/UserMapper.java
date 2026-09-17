package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.Permission;
import com.qzb.springboot.entity.Plan;
import com.qzb.springboot.entity.User;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

public interface UserMapper {

    void addUser(User user);

    void updateUser(User user);

    void updateUserBySelf(User user);

    User getUserInfo(String uid);

    List<User> getAllUser(String tid);

    void deleteUser(String uid, String tid);

    List<User> getUserByCondition(User user);

    @Insert({
            "<script>",
            "INSERT INTO user (id, name, password, tid, did, rid, phone, email, status,create_time,update_time)",
            "VALUES ",
            "<foreach collection='list' item='user' separator=','>",
            "(#{user.id}, #{user.name}, #{user.password}, #{user.tid}, #{user.did}, #{user.rid,}, #{user.phone}, #{user.email}, #{user.status},#{user.createTime},#{user.updateTime})",
            "</foreach>",
            "</script>"
    })
    int batchInsertUser(List<User> userList);

    User getUserByUsernameAndTid(String username,String tid);

    List<Permission> getPermissionsByRoleIdAndTid(String rid,String tid);

    void resetUser(String uid);
}
