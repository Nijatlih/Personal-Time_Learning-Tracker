# ADR 0002 — PTLT 与 ContentFlow AI 保持两个独立仓库

- **状态**：已接受
- **日期**：2026-08-09
- **涉及**：`Nijatlih/ptlt` 与 `Nijatlih/ContentFlow-AI`

## 背景

两个项目此前并排放在同一个本地目录里，都不在版本控制下，同一天首次纳入 git。它们同一个作者、
同一个 `com.nijat` groupId、几乎同一套技术栈：Maven、Spring Boot、Spring Security、JPA、Flyway、
jjwt、BCrypt、PostgreSQL、Vite + React。显而易见的问题是：要不要合并。

它们长得像是有具体原因的。**ContentFlow 提交在仓库里的 JWT 默认密钥，字面量就是
`ptlt-dev-secret-please-change-…`** —— ContentFlow 是复制 PTLT 的配置文件起步的。
两者的关系仅止于此：脚手架同源，代码不共享。

## 决策

**保持两个独立仓库。不合并代码，也不建 monorepo。**

## 理由

### 没有可去重的东西

对两棵代码树实测：

| 维度 | 结果 |
|---|---|
| 34 张表中的表名冲突 | **0** |
| 端点路径冲突（`/api/*` vs `/api/v1/*`） | **0** |
| 253 个 Java 文件中的重名类 | **3**（`AuthController`、`AuthService`、`SecurityConfig`） |
| 共享的实体 / DTO / import / 外键 | **0** |
| 真正可共享的代码 | 约 400–600 LOC |

可共享的部分是 `JwtUtil`(106) + `JwtService`(71) + `ApiResponse`(59) + 五个单行异常类 +
`GlobalExceptionHandler`(80) + 两个 `SecurityConfig`。为了去掉半个 KLOC 的重复而花 12–20 人天，
投入产出是负的；何况审计时两个项目加起来约 1.8 万行生产代码，只有 18 个测试方法兜底。

产品层面也毫无重叠。ContentFlow 是 AI 内容生成与发布，PTLT 是个人时间与学习追踪。仅有的几个
概念相邻处 —— "周计划"、"表现分析"、"streak" —— 不共享任何代码，在两边含义也不同。

### 完全合并会直接启动失败

放进同一个 Spring 上下文，必然出现：

- `authService`、`authController`、`securityConfig`、`passwordEncoder`、`corsConfigurationSource`
  的 `BeanDefinitionOverrideException`
- 两个无序的 catch-all `SecurityFilterChain`，都以 `anyRequest().authenticated()` 结尾
- Flyway 因两边 `classpath:db/migration` 都存在 `V1`–`V4` 而直接 abort
- 两个 `@ExceptionHandler(Exception.class)` advice，优先级不确定
- 两套互不可读的 token 格式必须统一，已签发的会话全部失效
- 两个 SPA 都写 `localStorage['accessToken']`，同源部署时互相踢掉对方的登录态

而且还要在"给 ContentFlow 的 20 张表加 `workspace_id` 并重写它按 userId 过滤的查询"
和"删掉 PTLT 的 workspace RBAC"之间二选一 —— 而后者恰恰是 PTLT 最好的一块代码。

### 决定性的不对称

ContentFlow 是 v0.15.0，有 ops 工具、部署/回滚脚本、监控和发布流程。PTLT 是 Phase 0–2 骨架，
审计时零测试，还有一个静默丢弃用户数据的 sync 端点。把两者耦合，等于把 PTLT 的风险导入更完整的
那个代码库，而且没有回归测试网能兜住。隔离爆炸半径比省下 500 行更值钱。

### 为什么连 monorepo 也不做

monorepo 曾是最初的建议（3–5 人天，零应用代码改动），最终仍选择完全分离，因为两个项目的
生命周期不同：ContentFlow 依其 ADR 0001 作为工程资产封存，PTLT 则按本仓库
[`adr/0001-personal-tool-not-a-product.md`](0001-personal-tool-not-a-product.md) 继续做成自用工具。
让一个已停更项目的 CI 在另一个活跃项目的每次提交上都跑一遍，没有任何收益。

## 后果

- 两个仓库各自自洽：各有 `.gitignore`、CI、README、ADR 和审计记录。
- 那 400–600 行认证脚手架的概念性重复被接受，保持重复。
- 如果将来两边都要动，值得对齐的约定：Spring Boot 保持同一 minor 线、jjwt 同一线、
  身份一律从 security context 取而不是重新解析 header、新代码统一采用 PTLT 的
  类型化异常 + `ApiResponse` 封装 —— 这是两套设计里更好的一套。
- 若某天出现第三个使用这套共享脚手架的项目，再重新考虑抽取。在那之前不动。

## 相关

- [`0001-personal-tool-not-a-product.md`](0001-personal-tool-not-a-product.md) —— PTLT 的方向
- [`../docs/AUDIT_2026_08_09.md`](../docs/AUDIT_2026_08_09.md) —— 这些决策的来源
- `Nijatlih/ContentFlow-AI` → `adr/0001-strategic-reset.md` —— 另一个项目为什么停止
