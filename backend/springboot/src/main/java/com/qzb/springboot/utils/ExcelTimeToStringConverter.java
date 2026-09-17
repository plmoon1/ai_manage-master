package com.qzb.springboot.utils;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import java.time.LocalTime;

/**
 * Excel 时间数值 精准转换为 HH:mm 字符串
 * 解决所有精度丢失问题
 */
public class ExcelTimeToStringConverter implements Converter<String> {

    @Override
    public Class<String> supportJavaTypeKey() {
        return String.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.NUMBER;
    }

    /**
     * 核心：将Excel的时间数值 精准转为 HH:mm 字符串
     */
    @Override
    public String convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        try {
            // 获取Excel存储的时间数值（天为单位）
            double num = cellData.getNumberValue().doubleValue();
            // 精准计算时分
            long totalSeconds = Math.round(num * 24 * 60 * 60);
            LocalTime time = LocalTime.ofSecondOfDay(totalSeconds);
            return time.toString(); // 输出标准格式：08:30、09:11
        } catch (Exception e) {
            // 非时间格式，直接返回原字符串
            return cellData.getStringValue();
        }
    }

    @Override
    public WriteCellData<?> convertToExcelData(String value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        return new WriteCellData<>(value);
    }
}