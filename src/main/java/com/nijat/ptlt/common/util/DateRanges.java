package com.nijat.ptlt.common.util;

import com.nijat.ptlt.common.exception.ValidationException;

import java.time.LocalDate;

/**
 * 日期区间参数的校验。
 *
 * 此前所有接收 startDate/endDate 的端点都不校验这两个参数的关系：startDate 晚于 endDate 时
 * BETWEEN 匹配不到任何行，于是接口返回空集或 0%，看起来和"这段时间确实没数据"一模一样，
 * 调用方无从知道自己把参数写反了。区间也没有上限，一个 startDate=0001-01-01 的请求
 * 会让数据库扫全表。
 */
public final class DateRanges {

    /** 单次查询允许的最大跨度。够覆盖"过去一年"这种最宽的正常用法。 */
    public static final long MAX_RANGE_DAYS = 366;

    private DateRanges() {
    }

    /**
     * 校验一个闭区间 [startDate, endDate]。
     *
     * @throws ValidationException 两端为空、顺序颠倒，或跨度超过 {@link #MAX_RANGE_DAYS}
     */
    public static void validate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("startDate and endDate are both required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("startDate must not be after endDate");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new ValidationException(
                    "Date range must not exceed " + MAX_RANGE_DAYS + " days, got " + days);
        }
    }
}
