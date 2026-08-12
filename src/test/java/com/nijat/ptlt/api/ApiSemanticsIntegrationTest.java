package com.nijat.ptlt.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nijat.ptlt.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/**
 * HTTP 语义的回归测试：客户端错误必须是 4xx，强制乐观锁，租户隔离。
 *
 * 这一类断言的价值在于它们钉住的是**状态码**。状态码的回退不会让任何功能"看起来坏掉"，
 * 只会让客户端无法区分该重试、该刷新、还是该报错 —— 正是最容易悄悄退化的东西。
 */
@DisplayName("HTTP 语义")
class ApiSemanticsIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("格式错误的请求返回 4xx，而不是 500")
    void malformedRequestsAreClientErrors() throws Exception {
        Account acc = newAccount();
        String token = acc.accessToken();

        // 日期参数不是日期
        MockHttpServletResponse badDate = getJson(
                "/api/v1/analytics/stability/summary?startDate=abc&endDate=2026-08-12", token);
        assertThat(badDate.getStatus()).isEqualTo(400);
        assertThat(errorCodeOf(badDate)).isEqualTo("BAD_REQUEST");

        // 缺必需查询参数
        MockHttpServletResponse missingParam = getJson(
                "/api/v1/analytics/stability/summary?startDate=2026-08-01", token);
        assertThat(missingParam.getStatus()).isEqualTo(400);

        // 请求体不是合法 JSON
        MockHttpServletResponse badJson = postJson("/api/v1/tasks", token, "{\"title\": broken");
        assertThat(badJson.getStatus()).isEqualTo(400);

        // HTTP 方法不对
        MockHttpServletResponse wrongMethod =
                exchange(delete("/api/v1/analytics/stability/summary"), token, null);
        assertThat(wrongMethod.getStatus()).isEqualTo(405);

        // 端点不存在
        MockHttpServletResponse unknown = getJson("/api/v1/no-such-thing", token);
        assertThat(unknown.getStatus()).isEqualTo(404);

        // 缺 Authorization header 的 logout（这个端点是 permitAll，所以走的是 400 而非 401）
        MockHttpServletResponse missingHeader = postJson("/api/v1/auth/logout", null, null);
        assertThat(missingHeader.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("时间范围倒置返回 400（此前是手写 IllegalArgumentException → 500）")
    void invertedTimeRangeIsBadRequest() throws Exception {
        String token = newAccount().accessToken();

        MockHttpServletResponse timeBlock = postJson("/api/v1/time-blocks", token, """
                {"date":"%s","startTime":"10:00:00","endTime":"09:00:00","title":"bad"}
                """.formatted(LocalDate.now()));
        assertThat(timeBlock.getStatus()).isEqualTo(400);
        assertThat(errorMessageOf(timeBlock)).contains("End time must be after start time");

        MockHttpServletResponse slot = postJson("/api/v1/schedule-slots", token, """
                {"dayOfWeek":"MONDAY","startTime":"10:00:00","endTime":"10:00:00","title":"bad"}
                """);
        assertThat(slot.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("更新必须带 version：缺失 400，陈旧 409 并回传 serverState")
    void optimisticLockingIsMandatory() throws Exception {
        String token = newAccount().accessToken();
        String taskId = dataOf(postJson("/api/v1/tasks", token, """
                {"title":"lock target","isCoreTask":false}
                """)).get("id").asText();

        // 不带 version：此前静默覆盖并返回 200
        MockHttpServletResponse missing = putJson("/api/v1/tasks/" + taskId, token, """
                {"title":"silent overwrite"}
                """);
        assertThat(missing.getStatus()).isEqualTo(400);
        assertThat(fieldErrorOf(missing, "version")).isEqualTo("Version is required");

        // 正确的 version：成功且递增
        JsonNode updated = dataOf(putJson("/api/v1/tasks/" + taskId, token, """
                {"title":"legit edit","version":0}
                """));
        assertThat(updated.get("title").asText()).isEqualTo("legit edit");
        assertThat(updated.get("version").asInt()).isEqualTo(1);

        // 陈旧的 version：409，并回传服务端当前状态供客户端比对
        MockHttpServletResponse stale = putJson("/api/v1/tasks/" + taskId, token, """
                {"title":"stale write","version":0}
                """);
        assertThat(stale.getStatus()).isEqualTo(409);
        JsonNode serverState = objectMapper.readTree(stale.getContentAsString())
                .path("error").path("details").path("serverState");
        assertThat(serverState.get("title").asText()).isEqualTo("legit edit");
        assertThat(serverState.get("version").asInt()).isEqualTo(1);

        // 冲突的写入不能改到数据
        JsonNode current = dataOf(getJson("/api/v1/tasks/" + taskId, token));
        assertThat(current.get("title").asText()).isEqualTo("legit edit");
    }

    @Test
    @DisplayName("time block 不能引用别的 workspace 的 task（唯一一处 IDOR）")
    void timeBlockCannotReferenceForeignTask() throws Exception {
        Account victim = newAccount();
        Account attacker = newAccount();

        String victimTaskId = dataOf(postJson("/api/v1/tasks", victim.accessToken(), """
                {"title":"victim task","isCoreTask":true}
                """)).get("id").asText();

        String body = """
                {"taskId":"%s","date":"%s","startTime":"09:00:00","endTime":"10:00:00","title":"x"}
                """.formatted(victimTaskId, LocalDate.now());

        // create 路径。回 404 而不是 403 —— "不属于你的"和"不存在的"对调用方应当无法区分，
        // 否则响应本身就成了一个存在性预言机。
        MockHttpServletResponse created =
                postJson("/api/v1/time-blocks", attacker.accessToken(), body);
        assertThat(created.getStatus()).isEqualTo(404);

        // update 路径 —— 此前两条都没有校验
        JsonNode own = dataOf(postJson("/api/v1/time-blocks", attacker.accessToken(), """
                {"date":"%s","startTime":"11:00:00","endTime":"12:00:00","title":"own"}
                """.formatted(LocalDate.now())));
        MockHttpServletResponse updated = putJson(
                "/api/v1/time-blocks/" + own.get("id").asText(), attacker.accessToken(), """
                {"taskId":"%s","version":%d}
                """.formatted(victimTaskId, own.get("version").asInt()));
        assertThat(updated.getStatus()).isEqualTo(404);

        // 回归：引用自己 workspace 的 task 必须成功
        String ownTaskId = dataOf(postJson("/api/v1/tasks", attacker.accessToken(), """
                {"title":"own task","isCoreTask":false}
                """)).get("id").asText();
        JsonNode legit = dataOf(postJson("/api/v1/time-blocks", attacker.accessToken(), """
                {"taskId":"%s","date":"%s","startTime":"14:00:00","endTime":"15:00:00","title":"ok"}
                """.formatted(ownTaskId, LocalDate.now())));
        assertThat(legit.get("taskId").asText()).isEqualTo(ownTaskId);
    }

    @Test
    @DisplayName("invite、sync、workspace create 端点均已删除（ADR 0003 / 0004 / 0005）")
    void removedEndpointsAreGone() throws Exception {
        Account acc = newAccount();

        assertThat(postJson("/api/v1/workspaces/" + acc.workspaceId() + "/invite",
                acc.accessToken(), """
                {"email":"someone@example.test","role":"EDITOR"}
                """).getStatus()).isEqualTo(404);

        assertThat(postJson("/api/v1/sync/pull", acc.accessToken(), "{}").getStatus())
                .isEqualTo(404);
        assertThat(postJson("/api/v1/sync/push", acc.accessToken(), "{}").getStatus())
                .isEqualTo(404);

        // ADR 0005：create 端点删掉了，因为它能造出一个此后永远访问不到的 workspace。
        // POST 到集合路径现在是"方法不支持"（该路径上只剩 GET）。
        assertThat(postJson("/api/v1/workspaces", acc.accessToken(), """
                {"name":"second workspace"}
                """).getStatus()).isEqualTo(405);
    }

    @Test
    @DisplayName("workspace 列表只返回 token 指向的那一个（ADR 0005）")
    void workspaceListReturnsOnlyTheReachableOne() throws Exception {
        Account acc = newAccount();

        JsonNode list = dataOf(getJson("/api/v1/workspaces", acc.accessToken()));

        // 此前返回用户名下的全部 workspace，而 token 里的 workspaceId 固定取"最早创建的
        // 那个"、没有任何端点能切换 —— 于是列表里会出现点不进去的条目。
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("id").asText()).isEqualTo(acc.workspaceId());

        // 详情端点对这一个仍然可用
        JsonNode detail = dataOf(getJson("/api/v1/workspaces/" + acc.workspaceId(), acc.accessToken()));
        assertThat(detail.get("id").asText()).isEqualTo(acc.workspaceId());
    }
}
