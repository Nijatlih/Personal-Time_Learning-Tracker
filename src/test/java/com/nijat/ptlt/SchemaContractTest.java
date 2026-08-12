package com.nijat.ptlt;

import com.nijat.ptlt.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * schema 与实体之间的契约测试。
 *
 * 这是本仓库此前完全缺失的那一类保障。首轮和第二轮审计都是靠**人手**逐列比对 14 个实体
 * 和 4 个迁移得出"ddl-auto: validate 能过"这个结论的 —— 结论当时成立，但没有任何东西
 * 保证它继续成立。本测试类把它变成机械校验：
 *
 * - 光是这个测试能跑起来，就已经证明了三件事：Flyway 从空库把全部迁移重放成功、
 *   flyway-database-postgresql 与 Flyway 版本配对正确、以及 ddl-auto: validate 下
 *   全部实体都能对上 schema（对不上时 EntityManagerFactory 初始化就会失败，
 *   上下文起不来，测试直接报错）。
 * - 下面的断言再钉住几个容易在改动中悄悄漂移的具体事实。
 */
@DisplayName("schema ↔ 实体契约")
class SchemaContractTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("全部迁移从空库重放成功，且没有 baseline 跳过任何一个")
    void migrationsReplayFromEmptyDatabase() {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class);

        // 断言的是"每一个都跑过"，而不是"总数等于 N" —— 后者在加迁移时会无意义地失败。
        assertThat(versions).contains("1", "2", "3", "4", "5", "6");

        Integer failed = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
        assertThat(failed).isZero();

        // baseline-on-migrate 是 false，所以历史里不该出现 baseline 记录。
        // 出现了就说明有人为了绕过某个问题把它打开了，而那正是 V1 会被静默跳过的路径。
        Integer baselines = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE type = 'BASELINE'", Integer.class);
        assertThat(baselines).isZero();
    }

    @Test
    @DisplayName("14 张业务表都存在")
    void allBusinessTablesExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' "
                        + "AND table_name <> 'flyway_schema_history' ORDER BY table_name",
                String.class);

        assertThat(tables).containsExactlyInAnyOrder(
                "alerts", "bootcamp_modules", "daily_module_entries", "daily_records",
                "refresh_tokens", "routine_completions", "routine_definitions",
                "schedule_slots", "task_instances", "tasks", "time_blocks",
                "users", "workspace_members", "workspaces");
    }

    @Test
    @DisplayName("workspace_members.role 已收敛到只允许 OWNER（V5 / ADR 0003）")
    void workspaceRoleCheckAllowsOwnerOnly() {
        String definition = jdbc.queryForObject(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
                        + "WHERE conname = 'workspace_members_role_check'", String.class);

        assertThat(definition).contains("OWNER");
        assertThat(definition).doesNotContain("EDITOR").doesNotContain("VIEWER");
    }

    @Test
    @DisplayName("refresh_tokens 存的是 64 字符摘要，明文 JWT 写不进来（V6）")
    void refreshTokenColumnCannotHoldPlaintextJwt() {
        String columnName = jdbc.queryForObject(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'refresh_tokens' AND column_name = 'token_hash'",
                String.class);
        assertThat(columnName).isEqualTo("token_hash");

        Integer maxLength = jdbc.queryForObject(
                "SELECT character_maximum_length FROM information_schema.columns "
                        + "WHERE table_name = 'refresh_tokens' AND column_name = 'token_hash'",
                Integer.class);
        // SHA-256 十六进制恒为 64 字符；JWT 远超这个长度，所以 schema 层面就装不下明文。
        assertThat(maxLength).isEqualTo(64);

        // 旧的明文列必须已经不存在，否则说明 V6 没跑或被改坏了
        Integer plaintextColumn = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_name = 'refresh_tokens' AND column_name = 'token'",
                Integer.class);
        assertThat(plaintextColumn).isZero();
    }

    @Test
    @DisplayName("每张带 version 列的表都是 NOT NULL —— 乐观锁依赖它")
    void versionColumnsAreNotNull() {
        List<String> nullableVersionColumns = jdbc.queryForList(
                // 排除 flyway_schema_history：它自己也有一个 version 列且允许为空
                // （repeatable migration 就是 version IS NULL），那是 Flyway 的表，不是业务表。
                "SELECT table_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND column_name = 'version' "
                        + "AND is_nullable = 'YES' AND table_name <> 'flyway_schema_history'",
                String.class);

        assertThat(nullableVersionColumns).isEmpty();
    }
}
