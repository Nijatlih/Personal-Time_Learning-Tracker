# CLAUDE.md

## 这是什么

PTLT (Personal Time & Learning Tracker) —— 个人时间、学习与系统稳定度追踪。
Spring Boot 3.5.16 / Java 21 后端（`src/`，107 文件，44 端点，14 张表，6 个 Flyway 迁移，
workspace 级多租户）+ React 19 + Vite 8 前端（`frontend/`，纯 JS，7 个页面）。

## 定位：自用工具，不做成产品

见 `adr/0001-personal-tool-not-a-product.md`。**不做**多人协作、邀请流、计费、移动端 App，
也不做 SDD 里的 Phase 4（`insight`，AI 洞察）。

三条已经执行到底、**不要重新引入**的决定：

- `adr/0003` —— invite 与 RBAC 已整套删除，`WorkspaceRole` 只剩 `OWNER`
- `adr/0004` —— 离线同步（sync）已整块删除
- 后端**只提供 API**，不 serve 任何前端资源（旧的 vanilla SPA 已删）

## 状态：两轮审计的缺陷已全部关闭

- `docs/AUDIT_2026_08_09.md` —— 首轮 20 条，14 条曾未修，**现已全部关闭**（逐条标注状态）
- `docs/AUDIT_2026_08_10.md` —— 第二轮 21 条新缺陷 + 第三轮新发现 5 条，全部关闭。
  含逐条实测证据、**两处审计误判的更正**，以及「已排除 —— 下一轮不要再查」那一节（避免重复劳动）

**下次排查前先读那一节**，它记录了已经验证干净的地方（实体↔schema 逐列比对、
Jackson 序列化、共享可变状态、请求校验覆盖面等）。

## 现在开放的事项

**完整清单见 [`docs/NEXT_STEPS.md`](docs/NEXT_STEPS.md)**（含每条的修法、取舍和"为什么本轮没做"）。
摘要：

1. **P1 —— Analytics 页有两段编造的数据当真实统计展示**（`Analytics.jsx:138,146`）。
   "85% consistency rate" 是硬编码文案，不来自任何接口。这是唯一一条真正的活缺陷，
   而且后端端点已就绪、前端 service 方法已写好只是零调用点，成本很低。
2. **P2 —— `POST /api/v1/workspaces` 能创建一个此后永远访问不到的 workspace**（需要产品决定）。
3. **P3 —— 11 个 controller 各自重复解析 JWT**（目前不是活缺陷，是重复；34 个集成测试已能兜住重构）。
4. **P4 —— 前端 5 个 lint 错误**，CI 的 lint 步骤仍是 `continue-on-error`。
   两条都有了现成修法：`Analytics.jsx` 和 `DailyLog.jsx` 是通过该规则的范例。
5. **P5 —— 零散项**：favicon 404、`green_streak`/`uptime_streak` 两列的定位、
   routine 完成率的 `totalDays` 口径。

## 命令

```bash
mvn -B clean verify                      # 34 个测试（11 单元 + 23 集成）
cd frontend && npm ci && npm run build
```

`mvn verify` 现在包含 Testcontainers 集成测试，**需要本机有可用的 Docker daemon**，
会拉起一个真实的 `postgres:16-alpine`。不要为了在无 Docker 环境跑通而换成内存库 ——
迁移重放和 `ddl-auto: validate` 的契约只对真实 PostgreSQL 才有意义。

本地手动跑后端：

```bash
docker run -d --name ptlt-pg -e POSTGRES_PASSWORD=ptltdev \
  -e POSTGRES_DB=ptlt -p 55432:5432 postgres:16
```

**四个必填环境变量**，均无默认值：`JWT_SECRET`（≥32 字节，且拒绝占位符值）、
`DB_USERNAME`、`DB_PASSWORD`。`APP_REGISTRATION_ENABLED` 默认 `false` ——
库里还没有任何用户时会自动放行一次，之后要再注册必须显式打开
（本地跑验证脚本需要多个账号时记得设它）。

**PostgreSQL 必须 ≥ 13**（`gen_random_uuid()` 从 13 起才是内置函数，迁移里没有
`CREATE EXTENSION pgcrypto`）。

## API 约定

- 6 个 `PUT` 端点**必须**带 `version`（强制乐观锁）：缺失 400，不符 409 +
  `error.details.serverState`。响应里的 `version` 是**递增后**的值，可以直接用于下一次更新。
- `POST /api/v1/daily-records` 是 create-or-update（按 workspace + date 唯一），**不需要** `version`。
- 认证失败 401、鉴权失败 403，都是 `ApiResponse` 结构。
- 收 `startDate`/`endDate` 的端点：区间必须正序且跨度 ≤ 366 天，否则 400。
- 不属于当前 workspace 的资源一律回 **404**（不是 403）—— 让"不属于你的"和"不存在的"无法区分。

## 待决事项

仓库目前是 **private**。作为作品集公开这件事原则上同意，但推迟到未修缺陷处理完之后。
`docs/PRE_PUBLIC_CHECKLIST.md` 记录了待办项和已完成的密钥/隐私扫描结果 —— **不要重复扫描**。
未经作者确认不要改动仓库可见性。

## 约定

- 提交信息：**不加 `Co-Authored-By: Claude`、不加 "Generated with Claude Code"** 等任何 AI 尾注。
- 修源码来满足 linter，**不要放宽 `eslint.config.js`**（仓库有 hook 会拦）。
  新增页面时注意别添第 6 个 lint 错误：取数用 `.then/.catch`，不要在 `useEffect` 里调一个
  会 setState 的函数（`react-hooks/set-state-in-effect` 是静态规则，与 await 在第几句无关）。
- Lombok 版本走 `${lombok.version}`，1.18.36 及以下在 JDK 24+ 上会静默编译失败。
  **不要重新加回 `maven-compiler-plugin` 的 `<fork>true</fork>`** —— 它会吞掉全部编译诊断，
  只剩一句 "Compilation failure"。
- 改动实体、迁移、配置或 security 之后，`mvn verify` 绿了还不够 —— 集成测试覆盖的是契约，
  真实启动一次仍是最快发现配置类问题的方式。
- 本项目 Java 21，ContentFlow 是 17。不要对齐 —— 见 `adr/0002-separate-repositories.md`。

## 相关

`Nijatlih/ContentFlow-AI` 是同一作者的另一个项目。技术栈几乎相同，但零共享代码，
**刻意不合并**。不要试图把两者整合。
