package com.qzb.springboot.mapper;

import com.qzb.springboot.entity.SysDict;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysdictMapper {
//    List<SysDict> getAllSysdicByTid(String tid);
//    void addSysdict(SysDict sysDict);
//
//    void updateSysdict(SysDict sysDict);
//
//    void deleteSysdict(String tid, String sid);

    /** ********************************************
     * 2026-01-19
     * 新增字典接口，以上接口暂时不用
     * ********************************************/


    /**
     * 根据租户ID查询所有字典数据（平级列表）
     * @param tid 租户ID
     * @return 字典列表
     */
    List<SysDict> getAllSysdicByTid(@Param("tid") String tid);

    /**
     * 根据ID查询字典
     * @param id 字典ID
     * @return 字典对象
     */
    SysDict getSysDictById(@Param("id") String id);

    /**
     * 根据父级ID查询所有下级字典
     * @param fid 父级ID
     * @return 字典列表
     */
    List<SysDict> getSysDictByFid(@Param("fid") String fid);

    /**
     * 查询字典类型列表（去重）
     * @param tid 租户ID
     * @return 字典类型列表
     */
    List<SysDict> getDistinctFields(@Param("tid") String tid);

    /**
     * 新增字典
     * @param sysDict 字典对象
     * @return 影响行数
     */
    int insertSysDict(SysDict sysDict);

    /**
     * 更新字典
     * @param sysDict 字典对象
     * @return 影响行数
     */
    int updateSysDict(SysDict sysDict);

    /**
     * 根据ID删除字典
     * @param id 字典ID
     * @return 影响行数
     */
    int deleteSysDictById(@Param("id") String id);

    /**
     * 根据ID批量删除字典
     * @param ids 字典ID列表
     * @return 影响行数
     */
    int deleteSysDictByIds(@Param("ids") List<String> ids);

    /**
     * 检查字典类型和名称是否已存在（全局）
     * 2026-08-17 已废弃：允许不同父节点下存在相同value（完整路径唯一即可），保留SQL仅供回退
     * @param tid 租户ID
     * @param field 字典类型
     * @param name 字典名称
     * @param value 字典值
     * @param id 排除的ID（更新时使用）
     * @return 存在的记录数
     */
    int checkDuplicate(@Param("tid") String tid, @Param("field") String field,@Param("name") String name,
                       @Param("value") String value, @Param("id") String id);

    /**
     * 检查兄弟节点（同级）下字典名称是否已存在
     * 2026-08-17 替代 checkDuplicateByFid：根节点（fid=id）之间互为同级，非根节点同级=fid相同
     * @param tid 租户ID
     * @param field 字典类型
     * @param fid 父级ID（新增根节点时传null）
     * @param value 字典值
     * @param isRoot 是否为根节点（1=根节点，0=普通节点）
     * @param excludeId 排除的ID（更新时使用）
     * @return 存在的记录数
     */
    int checkDuplicateAmongSiblings(@Param("tid") String tid, @Param("field") String field, @Param("fid") String fid,
                                    @Param("value") String value, @Param("isRoot") int isRoot,
                                    @Param("excludeId") String excludeId);

    String getIdByValue(String tid,String field,String value);

    /**
     * 根据ID查询单个字典节点
     */
    SysDict selectById(@Param("id") String id);

    /**
     * 根据父ID+租户ID查询子节点列表
     */
    List<SysDict> selectChildrenByFid(@Param("fid") String fid, @Param("tid") String tid);

    List<SysDict> getMeetingStatus(String tid);

    /**
     * 查询【已结束】状态的字典ID
     * @param tid 租户ID
     * @return 字典ID
     */
    String selectByIdAndStatus(@Param("tid") String tid,String status);
}
