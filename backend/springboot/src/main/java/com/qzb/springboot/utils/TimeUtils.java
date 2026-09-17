package com.qzb.springboot.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TimeUtils {
   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 预定义支持的时间格式模板（可根据实际需求扩展）
   private static final List<DateTimeFormatter> FORMATTERS = new ArrayList<>();
    // 时间标准格式：HH:mm
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    // 定义日期格式化器（严格匹配 yyyy-MM-dd）
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static {
        // 初始化支持的格式：年-月-日、年/月/日、年.月.日、无分隔符（yyyyMMdd）、年月日用1位/2位表示的情况
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyyMMdd"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-M-d"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/M/d"));
        FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy.M.d"));
    }


    public static String getCurrentTime(){
        // 1. 获取当前系统时间（本地时间）
        LocalDateTime now = LocalDateTime.now();
        // 2. 格式化为指定字符串
        String formattedTime = now.format(DATE_TIME_FORMATTER);
        return formattedTime;
    }

    /**
     * 将任意格式的时间字符串转换为yyyy-MM-dd格式
     * @param timeStr 输入的时间字符串（如2026/01/24、2026-1-24、20260124等）
     * @return 格式化后的时间字符串，格式为yyyy-MM-dd
     * @throws DateTimeParseException 输入字符串不支持解析时抛出异常
     */
    public static String formatToYyyyMmDd(String timeStr) {
        // 空值校验
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }

        String trimedStr = timeStr.trim();
        // 遍历所有支持的格式，尝试解析
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                // 解析为LocalDate
                LocalDate localDate = LocalDate.parse(trimedStr, formatter);
                // 格式化为yyyy-MM-dd并返回
                return localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e) {
                // 该格式解析失败，继续尝试下一个格式
                continue;
            }
        }

        // 所有格式都解析失败，抛出异常
        throw new DateTimeParseException(
                "不支持的时间格式：" + trimedStr + "，当前支持的格式包括yyyy-MM-dd、yyyy/MM/dd、yyyy.MM.dd、yyyyMMdd等",
                trimedStr,
                0
        );
    }

    /**
     * 格式化time_slot字段
     * @param input 用户传入的原始时间字符串
     * @return 格式化后的标准字符串（如：上午08:30、下午14:30、全天）
     */
    public static String formatTimeSlot(String input) {
        // 1. 空值校验
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("时间段不能为空");
        }

        // 去除首尾空格
        String timeStr = input.trim();

        // 2. 特殊值：全天 直接返回，不处理
        if ("全天".equals(timeStr)) {
            return timeStr;
        }

        try {
            // 3. 统一替换不规范符号
            // 全角冒号 → 半角冒号；点号 → 半角冒号
            timeStr = timeStr.replace("：", ":")
                    .replace(".", ":")
                    .replace("上午", "")
                    .replace("下午", "");

            // 4. 解析并格式化为标准 HH:mm 格式
            LocalTime localTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            String standardTime = localTime.format(TIME_FORMATTER);

            // 5. 添加时段前缀（上午/下午）
            if (localTime.isBefore(LocalTime.NOON)) {
                // 00:00 ~ 12:00 上午
                return "上午" + standardTime;
            } else {
                // 12:00 ~ 24:00 下午
                return "下午" + standardTime;
            }

        } catch (DateTimeParseException e) {
            // 解析失败：无效时间格式
            throw new IllegalArgumentException("时间格式不合法，请传入正确的时间（如：8:30、08.30、8：30）");
        }
    }







    /**
     * 根据 yyyy-MM-dd 格式的字符串，获取中文星期
     * @param time 日期字符串 例：2026-04-26
     * @return 中文星期 例：星期日
     */
    public static String getChineseWeek(String time) {
        try {
            // 1. 解析字符串为LocalDate
            LocalDate localDate = LocalDate.parse(time, DATE_FORMATTER);
            // 2. 获取星期枚举 (MONDAY, TUESDAY...SUNDAY)
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();

            // 3. 传统switch语句（JDK8完美支持）
            String weekName;
            switch (dayOfWeek) {
                case MONDAY:
                    weekName = "星期一";
                    break;
                case TUESDAY:
                    weekName = "星期二";
                    break;
                case WEDNESDAY:
                    weekName = "星期三";
                    break;
                case THURSDAY:
                    weekName = "星期四";
                    break;
                case FRIDAY:
                    weekName = "星期五";
                    break;
                case SATURDAY:
                    weekName = "星期六";
                    break;
                case SUNDAY:
                    weekName = "星期日";
                    break;
                default:
                    weekName = "";
            }
            return weekName;
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式错误，请使用 yyyy-MM-dd 格式");
        }
    }



    /**
     * 格式化当前时间为 HH:mm (24小时制)
     */
    public static String getCurrentTimeHHMM() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 获取当天日期 yyyy-MM-dd
     */
    public static String getTodayDate() {
        return java.time.LocalDate.now().toString();
    }

    /**
     * 将数据库时间（上午09:09/下午19:09）转为 24小时制 HH:mm
     */
    public static String convertDbTime(String dbTime) {
        if (dbTime == null || dbTime.isEmpty()) {
            return "";
        }
        // 截取时间部分：上午09:09 → 09:09，下午19:09 → 19:09
        return dbTime.replaceAll("[上午|下午]", "");
    }


    /**
     * 需求1：将带"上午/下午"的字符串时间，统一格式化为 24小时制 HH:mm 字符串
     * 支持格式："上午 8：30"、"上午 8:30"、"下午2:30"、"下午2：30"
     * @param time 原始时间字符串
     * @return 24小时制纯时间字符串 HH:mm
     */
    public static String formatTo24Hour(String time) {
        // 1. 非空校验
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }

        // 2. 数据清洗：去除所有空格 + 全角冒号「：」替换为半角冒号「:」
        String cleanedTime = time.replaceAll("\\s+", "").replace("：", ":");

        int hour;
        int minute;
        // 3. 区分上午/下午，截取纯时间部分
        if (cleanedTime.startsWith("上午")) {
            // 截取"上午"后面的时间：如 8:30
            String timePart = cleanedTime.substring(2);
            String[] hm = timePart.split(":");
            hour = Integer.parseInt(hm[0]);
            minute = Integer.parseInt(hm[1]);
        } else if (cleanedTime.startsWith("下午")) {
            // 截取"下午"后面的时间 + 小时+12 转为24小时制
            String timePart = cleanedTime.substring(2);
            String[] hm = timePart.split(":");
            if(Integer.parseInt(hm[0]) >= 12){
                hour = Integer.parseInt(hm[0]);
            }else{
                hour = Integer.parseInt(hm[0]) + 12;
            }
            minute = Integer.parseInt(hm[1]);
        } else {
            throw new IllegalArgumentException("不支持的时间格式：" + time);
        }

        // 4. 格式化：自动补前导零（如 8:30 → 08:30）
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * 需求2：对 HH:mm 格式的时间，加2小时，返回新的 HH:mm 字符串
     * 自动处理跨天（如 23:30 +2小时 → 01:30）
     * @param hhmm 24小时制纯时间字符串 HH:mm
     * @return 加2小时后的时间字符串 HH:mm
     */
    public static String addTwoHours(String hhmm) {
        // 1. 非空校验
        if (hhmm == null || hhmm.trim().isEmpty()) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }

        // 2. 定义时间格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        // 3. 解析字符串为 LocalTime（只处理时间，无日期，完美适配需求）
        LocalTime localTime = LocalTime.parse(hhmm, formatter);
        // 4. 加2小时
        LocalTime timeAfterAdd = localTime.plusHours(2);
        // 5. 格式化为字符串返回
        return timeAfterAdd.format(formatter);
    }

}
