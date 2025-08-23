package com.guild.core.time;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间提供器：统一获取“现实中的时间”（操作系统时间）。
 * 默认使用服务器操作系统的本地时区。
 * 后续如需支持“按玩家时区显示”，可在此类扩展按玩家ID读取/缓存时区。
 */
public final class TimeProvider {

    private static final ZoneId SERVER_ZONE = ZoneId.systemDefault();

    public static final DateTimeFormatter FULL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private TimeProvider() {}

    /** 获取服务器本地时区的当前时间（ZonedDateTime）。 */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(SERVER_ZONE);
    }

    /** 获取服务器本地时区的当前时间（LocalDateTime）。 */
    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(SERVER_ZONE);
    }

    /** 当前时间的完整字符串 yyyy-MM-dd HH:mm:ss */
    public static String nowString() {
        return nowLocalDateTime().format(FULL_FORMATTER);
    }

    /** 当前时间加 minutes 分钟，返回完整字符串。 */
    public static String plusMinutesString(int minutes) {
        return nowLocalDateTime().plusMinutes(minutes).format(FULL_FORMATTER);
    }

    /** 当前时间加 days 天，返回完整字符串。 */
    public static String plusDaysString(int days) {
        return nowLocalDateTime().plusDays(days).format(FULL_FORMATTER);
    }

    /** 格式化 LocalDateTime 为完整字符串。 */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return "未知";
        return dateTime.format(FULL_FORMATTER);
    }

    /** 仅日期字符串 yyyy-MM-dd */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "未知";
        return dateTime.format(DATE_FORMATTER);
    }
}


