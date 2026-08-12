package com.nijat.ptlt.daily;

import com.fasterxml.jackson.databind.JsonNode;
import com.nijat.ptlt.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 稳定度、streak、Gray Alert 的回归测试。
 *
 * 覆盖的是第二轮审计的 F5、F2 和 critic 第 2 条 —— 三条都曾经"表现正常但数字是错的"，
 * 正是最需要机械校验的一类缺陷。
 */
@DisplayName("稳定度与 streak")
class StabilityAndStreakIntegrationTest extends AbstractIntegrationTest {

    /** 提交一条 daily record，返回响应的 data 段。 */
    private JsonNode putRecord(Account acc, LocalDate date, boolean core,
                               String energy, boolean aligned) throws Exception {
        return dataOf(postJson("/api/v1/daily-records", acc.accessToken(), """
                {"date":"%s","coreTaskCompleted":%s,"cognitiveEnergy":"%s","alignedWithGoals":%s}
                """.formatted(date, core, energy, aligned)));
    }

    private String createMorningRoutine(Account acc) throws Exception {
        return dataOf(postJson("/api/v1/routines/definitions", acc.accessToken(), """
                {"name":"Morning","type":"MORNING"}
                """)).get("id").asText();
    }

    @Test
    @DisplayName("F5: routine 打卡走 {routineDefinitionId,date,completed}，并真的贡献 20 分")
    void routineCheckInContributesToStability() throws Exception {
        Account acc = newAccount();
        String routineId = createMorningRoutine(acc);
        LocalDate today = LocalDate.now();

        // 前端此前发的 payload：completedAt 后端不存在，两个必填字段一个都没发。
        // 这个断言钉住的是"契约没有回退"，而不只是"现在能用"。
        MockHttpServletResponse legacy = postJson("/api/v1/routines/completions", acc.accessToken(),
                """
                {"routineDefinitionId":"%s","completedAt":"%sT07:00:00Z"}
                """.formatted(routineId, today));
        assertThat(legacy.getStatus()).isEqualTo(400);
        assertThat(fieldErrorOf(legacy, "date")).isNotEmpty();
        assertThat(fieldErrorOf(legacy, "completed")).isNotEmpty();

        // 正确的 payload
        JsonNode completion = dataOf(postJson("/api/v1/routines/completions", acc.accessToken(), """
                {"routineDefinitionId":"%s","date":"%s","completed":true}
                """.formatted(routineId, today)));
        assertThat(completion.get("completed").asBoolean()).isTrue();
        // critic 第 1 条：@PrePersist 此前不设 completedAt，首次落库即 completed=true/completed_at=NULL
        assertThat(completion.get("completedAt").isNull()).isFalse();

        // core 30 + HIGH 15 + aligned 15 = 60；morning routine 再加 20 = 80 → GREEN。
        // 打卡坏掉的时候上限只有 60，GREEN 根本不可能出现。
        JsonNode record = putRecord(acc, today, true, "HIGH", true);
        assertThat(record.get("morningRoutineCompleted").asBoolean()).isTrue();
        assertThat(record.get("stabilityScore").asInt()).isEqualTo(80);
        assertThat(record.get("stabilityStatus").asText()).isEqualTo("GREEN");
    }

    @Test
    @DisplayName("F2: 区间里每条记录的 streak 锚在它自己那天，而不是全都等于今天的值")
    void streakIsAnchoredPerRecord() throws Exception {
        Account acc = newAccount();
        LocalDate today = LocalDate.now();

        // 全部是补录，没有一条是今天 —— 这正是旧实现会把所有值算成同一个（今天的）的场景。
        // 没有 routine 定义时上限 60，所以用 GRAY/YELLOW 验证 uptime streak。
        putRecord(acc, today.minusDays(5), false, "LOW", false);   // 0  → GRAY
        putRecord(acc, today.minusDays(4), false, "LOW", false);   // 0  → GRAY
        putRecord(acc, today.minusDays(3), true, "HIGH", true);    // 60 → YELLOW
        putRecord(acc, today.minusDays(2), true, "HIGH", true);    // 60 → YELLOW
        putRecord(acc, today.minusDays(1), true, "HIGH", true);    // 60 → YELLOW

        JsonNode rows = dataOf(getJson(
                "/api/v1/daily-records/range?startDate=" + today.minusDays(10)
                        + "&endDate=" + today, acc.accessToken()));

        // 响应按日期倒序；翻成升序再断言，读起来和上面写入的顺序一致
        List<Integer> uptimeAscending = new ArrayList<>();
        List<String> statusAscending = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            uptimeAscending.add(rows.get(i).get("uptimeStreak").asInt());
            statusAscending.add(rows.get(i).get("stabilityStatus").asText());
        }

        assertThat(statusAscending)
                .containsExactly("GRAY", "GRAY", "YELLOW", "YELLOW", "YELLOW");
        // 递增序列是这条缺陷的判别式：旧实现下 5 个值全都相同。
        assertThat(uptimeAscending).containsExactly(0, 0, 1, 2, 3);
    }

    @Test
    @DisplayName("critic#2: 补录连续两天 GRAY 也会生成 Gray Alert")
    void grayAlertFiresForBackfilledRecords() throws Exception {
        Account acc = newAccount();
        LocalDate today = LocalDate.now();

        assertThat(dataOf(getJson("/api/v1/alerts", acc.accessToken()))).isEmpty();

        // 两天都不是今天。旧实现锚在 LocalDate.now()，第一条日期就对不上而立刻 break，
        // grayCount 恒为 0，于是全系统唯一的告警功能只在当天录入时可用。
        putRecord(acc, today.minusDays(4), false, "LOW", false);
        putRecord(acc, today.minusDays(3), false, "LOW", false);

        JsonNode alerts = dataOf(getJson("/api/v1/alerts", acc.accessToken()));
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).get("type").asText()).isEqualTo("GRAY_ALERT");
        assertThat(alerts.get(0).get("isRead").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Dashboard 的当前 streak 锚在今天：没录今天就是 0，不报一个早已中断的连击")
    void dashboardCurrentStreakIsAsOfToday() throws Exception {
        Account acc = newAccount();
        LocalDate today = LocalDate.now();

        // 截至前天的连续 3 天非 GRAY，但今天和昨天都没录
        putRecord(acc, today.minusDays(4), true, "HIGH", true);
        putRecord(acc, today.minusDays(3), true, "HIGH", true);
        putRecord(acc, today.minusDays(2), true, "HIGH", true);

        JsonNode dashboard = dataOf(getJson("/api/v1/dashboard", acc.accessToken()));

        // 旧实现取 recentRecords.get(0)（最近一条）的 streak 当作"当前"值，
        // 在 streak 改成按记录自身日期锚定之后，那会把"截至前天的 3 天连击"
        // 报成仍在进行中。没录今天，连续段就是断的。
        assertThat(dashboard.get("uptimeStreak").asInt()).isZero();
        assertThat(dashboard.get("greenStreak").asInt()).isZero();

        // 但历史记录本身仍带着各自那天正确的 streak
        JsonNode recent = dashboard.get("recentRecords");
        assertThat(recent).isNotEmpty();
        int maxUptimeInHistory = 0;
        for (JsonNode row : recent) {
            maxUptimeInHistory = Math.max(maxUptimeInHistory, row.get("uptimeStreak").asInt());
        }
        assertThat(maxUptimeInHistory).isEqualTo(3);

        // 补上今天之后，当前 streak 应该接上（前天、昨天缺口仍在，所以只算今天这一天）
        putRecord(acc, today, true, "HIGH", true);
        JsonNode afterToday = dataOf(getJson("/api/v1/dashboard", acc.accessToken()));
        assertThat(afterToday.get("uptimeStreak").asInt()).isEqualTo(1);
        assertThat(afterToday.get("currentStabilityScore").asInt()).isEqualTo(60);
    }

    @Test
    @DisplayName("日期区间参数颠倒或过大时返回 400，而不是静默返回空集")
    void invalidDateRangeIsRejected() throws Exception {
        Account acc = newAccount();
        LocalDate today = LocalDate.now();

        MockHttpServletResponse reversed = getJson(
                "/api/v1/daily-records/range?startDate=" + today + "&endDate=" + today.minusDays(7),
                acc.accessToken());
        assertThat(reversed.getStatus()).isEqualTo(400);
        assertThat(errorMessageOf(reversed)).contains("startDate must not be after endDate");

        MockHttpServletResponse tooWide = getJson(
                "/api/v1/analytics/stability/summary?startDate=2000-01-01&endDate=" + today,
                acc.accessToken());
        assertThat(tooWide.getStatus()).isEqualTo(400);
        assertThat(errorMessageOf(tooWide)).contains("366");
    }
}
