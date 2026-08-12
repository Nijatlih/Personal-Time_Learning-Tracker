package com.nijat.ptlt.daily.service;

import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Streak计算服务
 *
 * 连续天数定义（按PRD-STRUCT）：
 * - Green streak: 连续GREEN天数
 * - Uptime streak: 连续status != GRAY天数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreakService {
    private final DailyRecordRepository dailyRecordRepository;

    /**
     * 向前回溯的最大天数。连续天数不可能超过这个值，超出部分按中断处理。
     * 存在的意义是给查询一个上限，避免把 workspace 的全部历史都读进内存。
     */
    public static final int STREAK_WINDOW_DAYS = 400;

    /** 该天算进 green streak 的条件。 */
    public static final Predicate<DailyRecord> IS_GREEN =
            r -> r.getStabilityStatus() == StabilityStatus.GREEN;

    /** 该天算进 uptime streak 的条件（非 GRAY 即视为在线）。 */
    public static final Predicate<DailyRecord> IS_UP =
            r -> r.getStabilityStatus() != StabilityStatus.GRAY;

    /**
     * 计算并设置单条记录的 streak。
     *
     * 锚点是这条记录自己的日期，不是"今天"。此前锚在 LocalDate.now()，
     * 而 DailyRecordService.getByDateRange 和 DashboardService 都会对列表里的
     * 每一条记录调它 —— 结果是 30 天区间里每条记录都被盖上同一个（今天的）
     * streak 值，连 GRAY 那天也会显示成 4 天绿色连击。除最新一天外全是错的。
     */
    @Transactional(readOnly = true)
    public void calculateAndSetStreak(DailyRecord dailyRecord, UUID workspaceId) {
        calculateAndSetStreaks(List.of(dailyRecord), workspaceId);
    }

    /**
     * 批量计算一组记录的 streak，**只查一次库**。
     *
     * 逐条调用 calculateAndSetStreak 是一个 N+1：Dashboard 要算 30 条记录 + 今天那条，
     * 就是 31 条各带 LIMIT 400 的查询（最多约 12000 行）；
     * getByDateRange 拉一个 366 天的区间就是 366 条查询。而这些查询的结果高度重叠 ——
     * 它们查的是同一个 workspace 的同一段历史，只是锚点不同。
     *
     * 这里一次把足够宽的窗口读出来，再对每条记录在同一份列表上向前数。
     */
    @Transactional(readOnly = true)
    public void calculateAndSetStreaks(List<DailyRecord> records, UUID workspaceId) {
        if (records.isEmpty()) {
            return;
        }

        LocalDate newest = records.stream()
                .map(DailyRecord::getDate).max(Comparator.naturalOrder()).orElseThrow();
        LocalDate oldest = records.stream()
                .map(DailyRecord::getDate).min(Comparator.naturalOrder()).orElseThrow();

        // 窗口要能覆盖到"最老那条记录再往前 STREAK_WINDOW_DAYS 天"，
        // 所以在窗口宽度上加上这批记录自身的跨度。
        long span = ChronoUnit.DAYS.between(oldest, newest) + 1;
        int limit = (int) Math.min(Integer.MAX_VALUE, STREAK_WINDOW_DAYS + span);

        List<DailyRecord> history =
                dailyRecordRepository.findOnOrBeforeWithLimit(workspaceId, newest, limit);

        List<DailyRecord> merged = mergeByDateDescending(records, history);

        for (DailyRecord record : records) {
            record.setGreenStreak(countStreak(merged, record.getDate(), IS_GREEN));
            record.setUptimeStreak(countStreak(merged, record.getDate(), IS_UP));
        }

        log.debug("Streaks calculated for workspace {}: {} record(s) from {} history row(s)",
                workspaceId, records.size(), history.size());
    }

    /**
     * 把待计算的记录与库里的历史合并成一条按日期倒序、每天最多一条的列表。
     *
     * 待计算的记录优先：DailyRecordService.createOrUpdate 是在 save() **之前**就调
     * streak 计算的，新建的那条还没落库、查不出来；而更新路径下库里同一天那条
     * 与传进来的是同一个实例，去重后不会重复计数。
     */
    private static List<DailyRecord> mergeByDateDescending(
            List<DailyRecord> records, List<DailyRecord> history) {
        Map<LocalDate, DailyRecord> byDate = new LinkedHashMap<>();
        for (DailyRecord record : records) {
            byDate.put(record.getDate(), record);
        }
        for (DailyRecord record : history) {
            byDate.putIfAbsent(record.getDate(), record);
        }

        List<DailyRecord> merged = new ArrayList<>(byDate.values());
        merged.sort(Comparator.comparing(DailyRecord::getDate).reversed());
        return merged;
    }

    /**
     * 从 anchor 当天起逐日向前数，直到日期断档或某天不满足条件为止。
     *
     * @param recordsDesc 按日期倒序排列、每天最多一条的记录。比 anchor 更新的条目会被跳过，
     *                    所以同一份列表可以服务多个不同的锚点（批量计算就依赖这一点）。
     * @param anchor      从哪一天开始往前数
     * @param qualifies   哪些天算进连续段
     */
    public static int countStreak(
            List<DailyRecord> recordsDesc, LocalDate anchor, Predicate<DailyRecord> qualifies) {
        int streak = 0;
        LocalDate expectedDate = anchor;

        for (DailyRecord record : recordsDesc) {
            // 比锚点更新的记录与这次计算无关
            if (record.getDate().isAfter(expectedDate)) {
                continue;
            }
            // 日期断档（那天根本没有记录）中断连续段
            if (!record.getDate().equals(expectedDate)) {
                break;
            }
            if (!qualifies.test(record)) {
                break;
            }
            streak++;
            expectedDate = expectedDate.minusDays(1);
        }

        return streak;
    }
}
