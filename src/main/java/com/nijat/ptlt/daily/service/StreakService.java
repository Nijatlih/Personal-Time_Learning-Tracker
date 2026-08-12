package com.nijat.ptlt.daily.service;

import com.nijat.ptlt.daily.entity.DailyRecord;
import com.nijat.ptlt.daily.entity.StabilityStatus;
import com.nijat.ptlt.daily.repository.DailyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
     * 计算并设置 streak。
     *
     * 锚点是 dailyRecord 自己的日期，不是"今天"。此前锚在 LocalDate.now()，
     * 而 DailyRecordService.getByDateRange 和 DashboardService 都会对列表里的
     * 每一条记录调本方法 —— 结果是 30 天区间里每条记录都被盖上同一个（今天的）
     * streak 值，连 GRAY 那天也会显示成 4 天绿色连击。除最新一天外全是错的。
     */
    @Transactional(readOnly = true)
    public void calculateAndSetStreak(DailyRecord dailyRecord, UUID workspaceId) {
        List<DailyRecord> history = historyEndingAt(workspaceId, dailyRecord);
        LocalDate anchor = dailyRecord.getDate();

        dailyRecord.setGreenStreak(countStreak(history, anchor, IS_GREEN));
        dailyRecord.setUptimeStreak(countStreak(history, anchor, IS_UP));

        log.debug("Streak calculated for workspace {} on {}: green={}, uptime={}",
                workspaceId, anchor, dailyRecord.getGreenStreak(), dailyRecord.getUptimeStreak());
    }

    /**
     * 取出以 dailyRecord 那天结尾、按日期倒序的历史。
     *
     * dailyRecord 本身显式放在队首而不是依赖查询结果，因为
     * DailyRecordService.createOrUpdate 是在 save() 之前就调用 streak 计算的 ——
     * 新建的那条还没落库，查不出来。库里同一天的那条（更新路径下存在）因此被过滤掉，
     * 避免同一天被数两次。
     */
    private List<DailyRecord> historyEndingAt(UUID workspaceId, DailyRecord dailyRecord) {
        LocalDate anchor = dailyRecord.getDate();
        List<DailyRecord> history = new ArrayList<>();
        history.add(dailyRecord);
        dailyRecordRepository.findOnOrBeforeWithLimit(workspaceId, anchor, STREAK_WINDOW_DAYS)
                .stream()
                .filter(r -> r.getDate().isBefore(anchor))
                .forEach(history::add);
        return history;
    }

    /**
     * 从 anchor 当天起逐日向前数，直到日期断档或某天不满足条件为止。
     *
     * @param recordsDesc 按日期倒序排列的记录，且第一条的日期必须就是 anchor
     * @param anchor      从哪一天开始往前数
     * @param qualifies   哪些天算进连续段
     */
    public static int countStreak(
            List<DailyRecord> recordsDesc, LocalDate anchor, Predicate<DailyRecord> qualifies) {
        int streak = 0;
        LocalDate expectedDate = anchor;

        for (DailyRecord record : recordsDesc) {
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
