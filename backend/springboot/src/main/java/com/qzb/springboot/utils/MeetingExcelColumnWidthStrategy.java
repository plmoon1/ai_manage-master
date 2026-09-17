package com.qzb.springboot.utils;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.Cell;

import java.util.List;

public class MeetingExcelColumnWidthStrategy extends AbstractColumnWidthStyleStrategy {
    // 按照PlanExcelDTO字段顺序设置列宽
    private static final Integer[] COLUMN_WIDTHS = {
            20,  // 日期
            20,  // 时间
            20,  // 结束日期
            20,  // 结束时间
            50,  // 会议名称
            35,  // 主持人
            50,  // 参加对象
            50,  // 会议地点
            15,  // 会议状态
            15,  // 大屏展示
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
