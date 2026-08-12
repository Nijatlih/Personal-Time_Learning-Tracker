package com.nijat.ptlt.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 所有集成测试的基类。
 *
 * 为什么必须是真的 PostgreSQL 而不是 H2：这套测试要覆盖的东西里有一半是 PostgreSQL
 * 特有的 —— {@code gen_random_uuid()} 默认值、{@code TIMESTAMPTZ}、原生 {@code uuid} 列、
 * {@code findOnOrBeforeWithLimit} 用的原生 {@code LIMIT} 语法、以及各表的 CHECK 约束。
 * 换成内存库会让"迁移能重放"和"实体对得上 schema"这两条最重要的断言失去意义。
 *
 * 容器是**静态单例**并在静态初始化块里启动，整个 JVM 只起一次；Spring 上下文因为所有
 * 子类的注解完全一致，也只创建一次并被缓存复用。代价是数据在测试类之间累积 ——
 * 所以每个测试都用 {@link #newAccount()} 建自己的租户，不依赖库是干净的。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /** 与 README 要求的最低版本保持一致（PostgreSQL ≥ 13，因为 gen_random_uuid()）。 */
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    /** 保证每次调用产生互不冲突的邮箱，让测试之间不会因为唯一约束互相干扰。 */
    private static final AtomicInteger ACCOUNT_SEQ = new AtomicInteger();

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static PostgreSQLContainer<?> postgres() {
        return POSTGRES;
    }

    /** 每个测试方法之前重置认证上下文，避免上一个测试的 token 泄漏进来。 */
    @BeforeEach
    void resetState() {
        // MockMvc 每次请求都显式带 Authorization header，这里不需要额外清理；
        // 保留这个钩子是为了让"测试之间没有隐式共享状态"这件事在代码里可见。
    }

    // ---------------------------------------------------------------- 账号

    /**
     * 一个隔离的租户：新用户 + 新 workspace，返回它的 token。
     *
     * 每个测试用自己的租户，是因为上下文和容器都被复用，库里会累积上一个测试的数据。
     */
    protected Account newAccount() throws Exception {
        int n = ACCOUNT_SEQ.incrementAndGet();
        String email = "it-" + n + "-" + System.nanoTime() + "@example.test";
        JsonNode data = dataOf(exchange(post("/api/v1/auth/register"), null, """
                {"email":"%s","password":"Passw0rd!23","workspaceName":"IT %d"}
                """.formatted(email, n)));
        return new Account(
                email,
                data.get("accessToken").asText(),
                data.get("refreshToken").asText(),
                data.get("userId").asText(),
                data.get("workspaceId").asText());
    }

    public record Account(String email, String accessToken, String refreshToken,
                          String userId, String workspaceId) {
    }

    // ---------------------------------------------------------------- HTTP

    protected MockHttpServletResponse exchange(
            MockHttpServletRequestBuilder builder, String token, String body) throws Exception {
        builder.contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.content(body);
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    protected MockHttpServletResponse getJson(String path, String token) throws Exception {
        return exchange(get(path), token, null);
    }

    protected MockHttpServletResponse postJson(String path, String token, String body)
            throws Exception {
        return exchange(post(path), token, body);
    }

    protected MockHttpServletResponse putJson(String path, String token, String body)
            throws Exception {
        return exchange(put(path), token, body);
    }

    // ---------------------------------------------------------------- 响应

    /** 取 ApiResponse 的 data 段，顺便断言 success 为 true。 */
    protected JsonNode dataOf(MockHttpServletResponse response) throws Exception {
        JsonNode root = objectMapper.readTree(response.getContentAsString());
        if (!root.path("success").asBoolean()) {
            throw new AssertionError("expected a successful ApiResponse but got: "
                    + response.getContentAsString());
        }
        return root.get("data");
    }

    /** 取 ApiResponse 的 error.message。 */
    protected String errorMessageOf(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString())
                .path("error").path("message").asText();
    }

    /** 取 ApiResponse 的 error.code。 */
    protected String errorCodeOf(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString())
                .path("error").path("code").asText();
    }

    /** 取 error.details 里某个字段的校验信息，用于断言是哪个字段没通过 @Valid。 */
    protected String fieldErrorOf(MockHttpServletResponse response, String field) throws Exception {
        return objectMapper.readTree(response.getContentAsString())
                .path("error").path("details").path(field).asText();
    }
}
