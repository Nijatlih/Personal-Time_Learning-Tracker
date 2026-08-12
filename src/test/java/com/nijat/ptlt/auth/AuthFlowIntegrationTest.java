package com.nijat.ptlt.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.nijat.ptlt.auth.util.TokenHasher;
import com.nijat.ptlt.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 认证链路的回归测试：401 语义、token 轮换与重用检测、摘要存储、logout 校验。
 */
@DisplayName("认证链路")
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("未认证返回 401 且带 ApiResponse 结构（此前是裸 403）")
    void unauthenticatedRequestsReturn401() throws Exception {
        MockHttpServletResponse noHeader = getJson("/api/v1/tasks", null);
        assertThat(noHeader.getStatus()).isEqualTo(401);
        assertThat(errorCodeOf(noHeader)).isEqualTo("UNAUTHORIZED");

        MockHttpServletResponse garbage = getJson("/api/v1/tasks", "garbage.token.here");
        assertThat(garbage.getStatus()).isEqualTo(401);
        assertThat(errorCodeOf(garbage)).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("refresh token 只以 SHA-256 摘要入库，明文不落盘")
    void refreshTokensAreStoredHashed() throws Exception {
        Account acc = newAccount();

        Integer matching = jdbc.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE token_hash = ?",
                Integer.class, TokenHasher.sha256Hex(acc.refreshToken()));
        assertThat(matching).isEqualTo(1);

        // 明文 JWT 都以 eyJ 开头。一行都不该有。
        Integer plaintextLooking = jdbc.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE token_hash LIKE 'eyJ%'", Integer.class);
        assertThat(plaintextLooking).isZero();
    }

    @Test
    @DisplayName("刷新会轮换两个 token；旧 token 再用即视为失陷，整条链一起撤销")
    void refreshRotatesAndReuseRevokesWholeChain() throws Exception {
        Account acc = newAccount();

        JsonNode rotated = dataOf(postJson("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(acc.refreshToken())));
        String newAccess = rotated.get("accessToken").asText();
        String newRefresh = rotated.get("refreshToken").asText();
        assertThat(newAccess).isNotEqualTo(acc.accessToken());
        assertThat(newRefresh).isNotEqualTo(acc.refreshToken());

        // 用已经轮换掉的旧 token 再刷一次 —— 这是 token 被盗后的典型表现
        MockHttpServletResponse replay = postJson("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(acc.refreshToken()));
        assertThat(replay.getStatus()).isEqualTo(401);

        // 关键断言：刚才那个**合法**的新 token 现在也必须失效。
        // 只拒绝重放请求是不够的 —— 那样攻击者和受害者会各持一条有效链并存。
        MockHttpServletResponse afterBreach = postJson("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(newRefresh));
        assertThat(afterBreach.getStatus()).isEqualTo(401);

        // 而且撤销必须真的落库 —— refresh() 上的 noRollbackFor 就是为了这件事，
        // 否则抛异常会把撤销一起回滚掉。
        Integer stillActive = jdbc.queryForObject(
                "SELECT count(*) FROM refresh_tokens WHERE user_id = ?::uuid AND revoked_at IS NULL",
                Integer.class, acc.userId());
        assertThat(stillActive).isZero();
    }

    @Test
    @DisplayName("logout 必须是有效的 access token：refresh token 或缺前缀都拒绝")
    void logoutRequiresValidAccessToken() throws Exception {
        Account acc = newAccount();

        // 此前只调 getUserIdFromToken（仅验签名和过期），任何签名有效的 token ——
        // 包括已轮换掉的 refresh token —— 都能把该用户全部会话踢掉
        MockHttpServletResponse withRefresh =
                postJson("/api/v1/auth/logout", acc.refreshToken(), null);
        assertThat(withRefresh.getStatus()).isEqualTo(401);

        // authHeader.replace("Bearer ", "") 会接受完全没有前缀的 header
        MockHttpServletResponse noPrefix = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/auth/logout")
                                .header("Authorization", acc.accessToken()))
                .andReturn().getResponse();
        assertThat(noPrefix.getStatus()).isEqualTo(401);

        MockHttpServletResponse ok = postJson("/api/v1/auth/logout", acc.accessToken(), null);
        assertThat(ok.getStatus()).isEqualTo(200);

        // logout 之后该用户的 refresh token 全部撤销，刷新不再可用
        MockHttpServletResponse afterLogout = postJson("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}
                """.formatted(acc.refreshToken()));
        assertThat(afterLogout.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("登录失败次数超限后锁定，且消息与密码错误完全一致")
    void loginIsRateLimited() throws Exception {
        Account acc = newAccount();

        String wrongPassword = """
                {"email":"%s","password":"definitely-not-the-password"}
                """.formatted(acc.email());

        String message = null;
        for (int i = 0; i < 6; i++) {
            MockHttpServletResponse response = postJson("/api/v1/auth/login", null, wrongPassword);
            assertThat(response.getStatus()).isEqualTo(401);
            message = errorMessageOf(response);
        }
        // 锁定时不额外透露"这个邮箱存在且被锁了"
        assertThat(message).isEqualTo("Invalid email or password");

        // 锁定期内即使密码正确也拒绝
        MockHttpServletResponse correctButLocked = postJson("/api/v1/auth/login", null, """
                {"email":"%s","password":"Passw0rd!23"}
                """.formatted(acc.email()));
        assertThat(correctButLocked.getStatus()).isEqualTo(401);
    }
}
