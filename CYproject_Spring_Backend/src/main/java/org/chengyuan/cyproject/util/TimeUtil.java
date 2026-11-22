package org.chengyuan.cyproject.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间转换工具。
 * 实体层使用 LocalDateTime 存储，DTO 层使用 ISO 8601 的 Instant 输出，
 * 这里统一做带时区的转换，避免直接使用 Instant.from(LocalDateTime) 抛出 DateTimeException。
 */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
