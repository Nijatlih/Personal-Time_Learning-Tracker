package com.nijat.ptlt.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.nijat.ptlt.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务完成率的回归测试（第二轮审计 F3）。
 *
 * 这条缺陷的性质是分子分母不同源：分子按日期区间过滤，分母却是 workspace 的全量 Task。
 * 它不会报错、不会崩，只会给出一个越用越离谱的数字 —— workspace 用得越久、
 * 查的区间越窄，偏差越大。所以断言必须构造"分母被污染"的场景，而不只是查一个平凡的区间。
 */
@DisplayName("任务完成率")
class TaskCompletionStatsIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("F3: 分母是区间内实际有安排的任务数，不是 workspace 全量任务数")
    void completionRateUsesScheduledTasksAsDenominator() throws Exception {
        Account acc = newAccount();
        String token = acc.accessToken();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 20 个任务定义，其中前 3 个是 core task。
        // 这些就是旧实现的分母 —— 与查询区间毫无关系。
        List<String> taskIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            taskIds.add(dataOf(postJson("/api/v1/tasks", token, """
                    {"title":"task %d","isCoreTask":%s}
                    """.formatted(i, i < 3))).get("id").asText());
        }

        // 昨天只安排了 2 个（都是 core），两个都完成
        for (String taskId : taskIds.subList(0, 2)) {
            postJson("/api/v1/tasks/instances", token, """
                    {"taskId":"%s","date":"%s","completed":true}
                    """.formatted(taskId, yesterday));
        }

        JsonNode stats = dataOf(getJson(
                "/api/v1/analytics/tasks/completion-stats?startDate=" + yesterday
                        + "&endDate=" + yesterday, token));

        // 旧实现：totalTasks=20、completionRate=10.0（全做完却显示 10%）
        assertThat(stats.get("totalTasks").asLong()).isEqualTo(2);
        assertThat(stats.get("completedTasks").asLong()).isEqualTo(2);
        assertThat(stats.get("completionRate").asDouble()).isEqualTo(100.0);

        // 旧实现：coreTasksTotal=3、coreTaskCompletionRate≈66.67
        assertThat(stats.get("coreTasksTotal").asLong()).isEqualTo(2);
        assertThat(stats.get("coreTasksCompleted").asLong()).isEqualTo(2);
        assertThat(stats.get("coreTaskCompletionRate").asDouble()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("区间内有安排但未完成时，完成率按比例计算")
    void partialCompletionIsProportional() throws Exception {
        Account acc = newAccount();
        String token = acc.accessToken();
        LocalDate day = LocalDate.now().minusDays(2);

        List<String> taskIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            taskIds.add(dataOf(postJson("/api/v1/tasks", token, """
                    {"title":"partial %d","isCoreTask":false}
                    """.formatted(i))).get("id").asText());
        }
        // 4 个都安排，只完成 1 个
        for (int i = 0; i < 4; i++) {
            postJson("/api/v1/tasks/instances", token, """
                    {"taskId":"%s","date":"%s","completed":%s}
                    """.formatted(taskIds.get(i), day, i == 0));
        }

        JsonNode stats = dataOf(getJson(
                "/api/v1/analytics/tasks/completion-stats?startDate=" + day
                        + "&endDate=" + day, token));

        assertThat(stats.get("totalTasks").asLong()).isEqualTo(4);
        assertThat(stats.get("completedTasks").asLong()).isEqualTo(1);
        assertThat(stats.get("completionRate").asDouble()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("区间内没有任何安排时返回 0% 而不是除零")
    void emptyRangeReturnsZero() throws Exception {
        String token = newAccount().accessToken();
        LocalDate longAgo = LocalDate.now().minusDays(200);

        JsonNode stats = dataOf(getJson(
                "/api/v1/analytics/tasks/completion-stats?startDate=" + longAgo
                        + "&endDate=" + longAgo, token));

        assertThat(stats.get("totalTasks").asLong()).isZero();
        assertThat(stats.get("completionRate").asDouble()).isZero();
        assertThat(stats.get("coreTaskCompletionRate").asDouble()).isZero();
    }
}
