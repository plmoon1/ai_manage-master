package com.qzb.springboot.utils;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.Cell;

import java.util.List;

public class PlanExcelColumnWidthStrategy extends AbstractColumnWidthStyleStrategy {
    // 按照PlanExcelDTO字段顺序设置列宽
    private static final Integer[] COLUMN_WIDTHS = {
            35,  // 任务名称
            40,  // 任务详情
            18,  // 任务所属部门
            12,  // 任务性质
            15,  // 开始时间
            15,  // 结束时间
            50,  // 任务类型
            15,  // 可见范围
            15,  // 大屏展示
            25,  // 负责人
            12,  // 重要程度
            12,  // 任务状态
            50   // 任务成果
    };
    /**
     * 适配 EasyExcel 3.3.2 正确的重写方法
     */
    @Override
    protected void setColumnWidth(WriteSheetHolder writeSheetHolder, List<WriteCellData<?>> cellDataList,
                                  Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        // 仅对表头设置列宽（只执行一次）
        if (Boolean.TRUE.equals(isHead)) {
            int columnIndex = cell.getColumnIndex();
            if (columnIndex < COLUMN_WIDTHS.length) {
                // 设置列宽（256为Excel单位基数）
                writeSheetHolder.getSheet().setColumnWidth(columnIndex, COLUMN_WIDTHS[columnIndex] * 256);
            }
        }
    }
}
