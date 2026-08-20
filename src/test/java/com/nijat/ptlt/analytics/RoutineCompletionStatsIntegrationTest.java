package com.nijat.ptlt.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.nijat.ptlt.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Routine 完成率")
class RoutineCompletionStatsIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("定义创建前的日期不进入完成率分母")
    void daysBeforeDefinitionAreNotEligible() throws Exception {
        Account account = newAccount();
        String token = account.accessToken();
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        String routineId = dataOf(postJson("/api/v1/routines/definitions", token, """
                {"type":"MORNING","name":"Morning reset","description":"Start deliberately"}
                """)).get("id").asText();

        postJson("/api/v1/routines/completions", token, """
                {"routineDefinitionId":"%s","date":"%s","completed":true}
                """.formatted(routineId, today));

        JsonNode stats = dataOf(getJson(
                "/api/v1/analytics/routines/completion-stats?startDate=" + thirtyDaysAgo
                        + "&endDate=" + today, token));

        assertThat(stats.get("morningTotalDays").asLong()).isEqualTo(1);
        assertThat(stats.get("morningCompletedDays").asLong()).isEqualTo(1);
        assertThat(stats.get("morningCompletionRate").asDouble()).isEqualTo(100.0);
        assertThat(stats.get("eveningTotalDays").asLong()).isZero();
        assertThat(stats.get("eveningCompletionRate").asDouble()).isZero();
        assertThat(stats.has("totalDays")).isFalse();
    }

    @Test
    @DisplayName("定义在查询区间之后创建时有效天数为零")
    void definitionCreatedAfterRangeHasNoEligibleDays() throws Exception {
        Account account = newAccount();
        String token = account.accessToken();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        postJson("/api/v1/routines/definitions", token, """
                {"type":"EVENING","name":"Evening shutdown"}
                """);

        JsonNode stats = dataOf(getJson(
                "/api/v1/analytics/routines/completion-stats?startDate=" + yesterday
                        + "&endDate=" + yesterday, token));

        assertThat(stats.get("morningTotalDays").asLong()).isZero();
        assertThat(stats.get("eveningTotalDays").asLong()).isZero();
        assertThat(stats.get("eveningCompletionRate").asDouble()).isZero();
    }
}
