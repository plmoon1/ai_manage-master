package com.qzb.springboot.service;

import com.qzb.springboot.entity.SysDict;
import com.qzb.springboot.mapper.SysdictMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysdictService {

    @Resource
    SysdictMapper sysdictMapper;

//    public List<SysDict> getAllSysdicByTid(String tid) {
//        // 1. 查询平级字典数据（仅按tid过滤）
//        List<SysDict> dictList = sysdictMapper.getAllSysdicByTid(tid); // 修改：调用新的Mapper方法
//        if (dictList.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        // 2. 构建ID->字典对象的映射（快速查找父节点）
//        Map<String, SysDict> dictMap = new HashMap<>();
//        for (SysDict dict : dictList) {
//            dict.setChildren(new ArrayList<>()); // 初始化子节点列表
//            dictMap.put(dict.getId(), dict);
//        }
//
//        // 3. 构建树形结构（逻辑不变，适配fid=id的设计）
//        List<SysDict> rootList = new ArrayList<>();
//        for (SysDict dict : dictList) {
//            String parentId = dict.getFid();
//
//            // 情况1：fid≠id → 有父节点，添加到父节点的子列表中
//            if (!dict.getId().equals(parentId)) {
//                SysDict parentDict = dictMap.get(parentId);
//                if (parentDict != null) {
//                    parentDict.getChildren().add(dict);
//                } else {
//                    // 父节点不存在，作为根节点
//                    rootList.add(dict);
//                }
//            }
//            // 情况2：fid=id → 叶子节点（无下级），若未被添加到任何父节点则作为根节点
//            else {
//                boolean hasParent = dictList.stream()
//                        .anyMatch(d -> d.getChildren().contains(dict));
//                if (!hasParent) {
//                    rootList.add(dict);
//                }
//            }
//        }
//
//        // 过滤最终根节点（排除被其他节点包含的节点）
//        return rootList.stream()
//                .filter(root -> dictList.stream()
//                        .noneMatch(d -> d.getChildren().contains(root)))
//                .toList();
//
//    }

//    public void addSysdict(SysDict sysDict) {
//        this.sysdictMapper.addSysdict(sysDict);
//    }
//
//    public void updateSysdict(SysDict sysDict) {
//        this.sysdictMapper.updateSysdict(sysDict);
//    }
//
//    public void deleteSysdict(String tid, String sid) {
//        this.sysdictMapper.deleteSysdict(tid,sid);
//    }


    /** ********************************************
     * 2026-01-19
     * 新增字典接口，以上接口暂时不用
     * ********************************************/


    public List<SysDict> getAllSysdicByTid(String tid) {
        // 1. 查询平级字典数据（仅按tid过滤）
        List<SysDict> dictList = sysdictMapper.getAllSysdicByTid(tid);
        if (dictList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 构建ID->字典对象的映射（快速查找父节点）
        Map<String, SysDict> dictMap = new HashMap<>();
        for (SysDict dict : dictList) {
            dict.setChildren(new ArrayList<>()); // 初始化子节点列表
            dictMap.put(dict.getId(), dict);
        }

        // 3. 构建树形结构（逻辑不变，适配fid=id的设计）
        List<SysDict> rootList = new ArrayList<>();
        for (SysDict dict : dictList) {
            String parentId = dict.getFid();
            // 情况1：fid≠id → 有父节点，添加到父节点的子列表中
            if (!dict.getId().equals(parentId)) {
                SysDict parentDict = dictMap.get(parentId);
                if (parentDict != null) {
                    parentDict.getChildren().add(dict);
                } else {
                    // 父节点不存在，作为根节点
                    rootList.add(dict);
                }
            }
            // 情况2：fid=id → 叶子节点（无下级），若未被添加到任何父节点则作为根节点
            else {
                boolean hasParent = dictList.stream()
                        .anyMatch(d -> d.getChildren().contains(dict));
                if (!hasParent) {
                    rootList.add(dict);
                }
            }
        }

        // 过滤最终根节点（排除被其他节点包含的节点）
        return rootList.stream()
                .filter(root -> dictList.stream()
                        .noneMatch(d -> d.getChildren().contains(root)))
                .collect(Collectors.toList());
    }


    public SysDict getSysDictById(String id) {
        return sysdictMapper.getSysDictById(id);
    }


    public List<SysDict> getDistinctFields(String tid) {
        return sysdictMapper.getDistinctFields(tid);
    }


    @Transactional(rollbackFor = Exception.class)
    public void insertSysDict(SysDict sysDict) {
        // 生成UUID作为ID
        sysDict.setId(UUID.randomUUID().toString());

        // 如果未指定fid，则设置为id（表示没有下级）
        if (!StringUtils.hasText(sysDict.getFid())) {
            sysDict.setFid(sysDict.getId());
        }

        // 2026-08-17 校验规则调整：允许不同父节点下同名（完整路径唯一），仅禁止同级同名
        // 校验：value 不允许包含 /（会破坏完整路径解析）
        checkValueNoSlash(sysDict.getValue());
        // 校验：兄弟节点（含根节点之间）不能存在相同名称的字典项
        checkSiblingDuplicate(sysDict.getTid(), sysDict.getField(), sysDict.getFid(),
                sysDict.getValue(), sysDict.getId(), null);
        sysdictMapper.insertSysDict(sysDict);
    }


    @Transactional(rollbackFor = Exception.class)
    public void updateSysDict(SysDict sysDict) {
        SysDict existingDict = sysdictMapper.getSysDictById(sysDict.getId());
        if (existingDict == null) {
            throw new RuntimeException("字典不存在");
        }

        // 2026-08-17 校验规则调整：允许不同父节点下同名（完整路径唯一），仅禁止同级同名
        // 更新时fid/value可能未传（MyBatis动态SQL表示不修改该字段），取生效值校验
        String effectiveFid = StringUtils.hasText(sysDict.getFid()) ? sysDict.getFid() : existingDict.getFid();
        String effectiveValue = StringUtils.hasText(sysDict.getValue()) ? sysDict.getValue() : existingDict.getValue();
        String effectiveField = StringUtils.hasText(sysDict.getField()) ? sysDict.getField() : existingDict.getField();
        String effectiveTid = StringUtils.hasText(sysDict.getTid()) ? sysDict.getTid() : existingDict.getTid();
        // 校验：value 不允许包含 /（会破坏完整路径解析）
        checkValueNoSlash(effectiveValue);
        // 校验：兄弟节点（含根节点之间）不能存在相同名称的字典项
        checkSiblingDuplicate(effectiveTid, effectiveField, effectiveFid,
                effectiveValue, sysDict.getId(), sysDict.getId());

        // 如果修改了fid，需要更新所有下级数据的fid（可选功能，根据需求决定是否需要）
        // 这里只更新当前数据
        this.sysdictMapper.updateSysDict(sysDict);
    }

    /**
     * 校验字典value不允许包含 /
     * 路径分隔符 / 用于拼接和解析完整分类路径（如：科学研究/社会服务），value 含 / 会导致歧义
     */
    private void checkValueNoSlash(String value) {
        if (StringUtils.hasText(value) && value.contains("/")) {
            throw new RuntimeException("字典项名称不允许包含 \"/\"");
        }
    }

    /**
     * 兄弟节点（同级）同名校验
     * 根节点（fid=id）之间互为同级；非根节点的同级=同一fid下的节点
     * @param excludeId 排除的ID（更新时传自身id，新增时传null）
     */
    private void checkSiblingDuplicate(String tid, String field, String fid,
                                       String value, String id, String excludeId) {
        // 根节点判定：fid为空 或 fid=自身id
        boolean isRoot = !StringUtils.hasText(fid) || fid.equals(id);
        int count = sysdictMapper.checkDuplicateAmongSiblings(tid, field, fid, value,
                isRoot ? 1 : 0, excludeId);
        if (count > 0) {
            if (isRoot) {
                throw new RuntimeException("顶级字典项下已存在相同名称的字典项");
            }
            throw new RuntimeException("同一父节点下已存在相同名称的字典项");
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void deleteSysDict(String id) {
        List<String> ids = getDeleteIds(id);
        if (CollectionUtils.isEmpty(ids)) {
            throw new RuntimeException("字典不存在");
        }

        // 批量删除
        if (ids.size() == 1) {
            sysdictMapper.deleteSysDictById(ids.get(0));
        } else {
            sysdictMapper.deleteSysDictByIds(ids);
        }
    }


    public List<String> getDeleteIds(String id) {
        List<String> ids = new ArrayList<>();
        collectDeleteIds(id, ids);
        return ids;
    }

    /**
     * 递归收集需要删除的所有ID
     * @param id 当前ID
     * @param ids 收集的ID列表
     */
    private void collectDeleteIds(String id, List<String> ids) {
        ids.add(id);
        List<SysDict> children = sysdictMapper.getSysDictByFid(id);
        if (!CollectionUtils.isEmpty(children)) {
            for (SysDict child : children) {
                collectDeleteIds(child.getId(), ids);
            }
        }
    }

    /* ****************************************
      新需求：找到当前节点的完整树形结构
      time：2026-03-18
     ******************************************/

    /**
     * 根据字典ID查询整个树形结构
     * @param id 任意层级的字典节点ID
     * @return 整棵树的根节点（含所有子节点）
     */
    public SysDict getDictTreeById(String id) {
        // 1. 验证节点是否存在
        SysDict targetNode = sysdictMapper.selectById(id);
        if (targetNode == null) {
            throw new RuntimeException("字典节点不存在，ID：" + id);
        }

        // 2. 【修复核心】Java递归找根节点（替代原SQL递归）
        SysDict rootNode = findRootNode(targetNode);

        // 3. 递归构建整棵树
        buildDictTree(rootNode,id);

        return rootNode;
    }

    /**
     * Java递归查找根节点（fid = id 的节点）
     */
    private SysDict findRootNode(SysDict currentNode) {
        // 终止条件：当前节点fid等于id，说明是根节点
        if (currentNode.getFid().equals(currentNode.getId())) {
            return currentNode;
        }
        // 递归查询父节点
        SysDict parentNode = sysdictMapper.selectById(currentNode.getFid());
        if (parentNode == null) {
            throw new RuntimeException("字典节点的父节点不存在，父ID：" + currentNode.getFid());
        }
        return findRootNode(parentNode);
    }

    /**
     * 递归构建子节点
     */
    private void buildDictTree(SysDict parentNode, String currentId) {
        // 标记当前节点是否为目标节点
        if (parentNode.getId().equals(currentId)) {
            parentNode.setCurrent(true);
        }
        // 查询当前节点的所有子节点（加tid过滤，保证多租户隔离）
        List<SysDict> children = sysdictMapper.selectChildrenByFid(
                parentNode.getId(), parentNode.getTid()
        );
        if (children != null && !children.isEmpty()) {
            parentNode.setChildren(children);
            // 递归处理每个子节点
            for (SysDict child : children) {
                buildDictTree(child,currentId);
            }
        }
    }

    public List<SysDict> getMeetingStatus(String tid) {
        return sysdictMapper.getMeetingStatus(tid);
    }

    /* ****************************************
      新需求：Excel导入时按完整分类路径解析字典ID
      背景：允许不同父节点下存在同名叶子（如 科学研究/社会服务 与 国际合作与社会服务/社会服务），
      导入时需通过完整路径（或唯一后缀）确定目标节点
      time：2026-08-17
     ******************************************/

    /** 任务类型字典的field标识 */
    private static final String TYPE_FIELD = "type";

    /**
     * 按完整路径（或唯一后缀）解析任务类型字典ID
     * 解析策略：
     *   1. 归一化输入（按/切分、trim、丢弃空段）
     *   2. 精确匹配完整路径（根/.../叶子）
     *   3. 后缀匹配（兼容只填叶子名或部分路径的旧模板）：唯一命中则返回，多命中则报歧义
     * @param tid 租户ID
     * @param input Excel中填写的分类文本（如 "国际合作与社会服务/社会服务" 或 "社会服务"）
     * @return 字典节点ID
     * @throws RuntimeException 分类不存在或存在歧义时抛出（message 含候选路径，可直接展示给用户）
     */
    public String resolveTypeIdByPath(String tid, String input) {
        DictPathIndex index = buildTypePathIndex(tid);
        return resolveByIndex(index, input);
    }

    /**
     * 构建任务类型字典的「完整路径 → id」索引（每次导入构建一次，供逐行复用）
     * @param tid 租户ID
     */
    public DictPathIndex buildTypePathIndex(String tid) {
        List<SysDict> dictList = sysdictMapper.getAllSysdicByTid(tid);
        Map<String, SysDict> idMap = new HashMap<>();
        for (SysDict dict : dictList) {
            if (TYPE_FIELD.equals(dict.getField())) {
                idMap.put(dict.getId(), dict);
            }
        }
        // path -> id；一个id只对应一条路径（父链唯一），同级不同名保证路径唯一
        Map<String, String> pathIndex = new HashMap<>();
        for (SysDict dict : idMap.values()) {
            pathIndex.put(buildNodePath(idMap, dict.getId()), dict.getId());
        }
        return new DictPathIndex(idMap, pathIndex);
    }

    /**
     * 在既有索引上解析分类文本（供导入监听器逐行调用，避免每行重建索引）
     */
    public String resolveByIndex(DictPathIndex index, String input) {
        if (!StringUtils.hasText(input)) {
            throw new RuntimeException("任务类型不能为空");
        }
        // 1. 归一化：按/切分 -> trim -> 丢弃空段 -> 重新拼接
        String[] segments = input.split("/");
        StringBuilder normalized = new StringBuilder();
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (!trimmed.isEmpty()) {
                if (normalized.length() > 0) {
                    normalized.append("/");
                }
                normalized.append(trimmed);
            }
        }
        String normalizedPath = normalized.toString();
        if (normalizedPath.isEmpty()) {
            throw new RuntimeException("任务类型不能为空");
        }

        // 2. 精确匹配完整路径
        String exactId = index.getPathIndex().get(normalizedPath);
        if (exactId != null) {
            return exactId;
        }

        // 3. 后缀匹配（兼容只填叶子名或部分路径）：path == input 或 path 以 "/input" 结尾
        List<String> candidates = new ArrayList<>();
        for (String path : index.getPathIndex().keySet()) {
            if (path.equals(normalizedPath) || path.endsWith("/" + normalizedPath)) {
                candidates.add(path);
            }
        }
        if (candidates.isEmpty()) {
            throw new RuntimeException("任务类型【" + input + "】不存在，请检查分类路径");
        }
        if (candidates.size() > 1) {
            // 排序保证报错文案中候选顺序稳定
            Collections.sort(candidates);
            throw new RuntimeException("任务类型【" + input + "】存在多个同名项："
                    + String.join("、", candidates) + "，请填写完整分类路径（如：" + candidates.get(0) + "）");
        }
        return index.getPathIndex().get(candidates.get(0));
    }

    /**
     * 沿fid上溯到根（fid=id），拼接完整路径：根value/.../自身value
     */
    private String buildNodePath(Map<String, SysDict> idMap, String dictId) {
        LinkedList<String> valueList = new LinkedList<>();
        SysDict current = idMap.get(dictId);
        // 防御：父链出现环时最多上溯 idMap.size() 层
        int maxDepth = idMap.size() + 1;
        while (current != null && maxDepth-- > 0) {
            valueList.addFirst(current.getValue());
            // 终止条件：根节点（fid=id）
            if (current.getFid() == null || current.getFid().equals(current.getId())) {
                break;
            }
            current = idMap.get(current.getFid());
        }
        return String.join("/", valueList);
    }

    /**
     * 任务类型字典路径索引（不可变，供导入监听器逐行复用）
     */
    public static class DictPathIndex {
        /** id -> 字典节点 */
        private final Map<String, SysDict> idMap;
        /** 完整路径 -> id */
        private final Map<String, String> pathIndex;

        public DictPathIndex(Map<String, SysDict> idMap, Map<String, String> pathIndex) {
            this.idMap = idMap;
            this.pathIndex = pathIndex;
        }

        public Map<String, SysDict> getIdMap() {
            return idMap;
        }

        public Map<String, String> getPathIndex() {
            return pathIndex;
        }
    }
}
