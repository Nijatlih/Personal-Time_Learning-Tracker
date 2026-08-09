# Personal Time & Learning Tracker (PTLT)

个人时间、学习与系统稳定度追踪系统。Spring Boot 后端 + React SPA，workspace 级多租户。

**状态：** Phase 0–2 已实现，最后一次代码改动 2026-05-03。核心录入路径缺失（见 Known Issues）。

---

## 技术栈

| 层 | 技术 |
|---|---|
| Backend | Spring Boot 3.2.0 / Java 21 / Maven |
| Database | PostgreSQL + Flyway |
| Auth | JWT（access + refresh token） |
| Frontend | React 19 + React Router 7 + Vite 8（纯 JavaScript，非 TypeScript） |
| 图表 | Recharts · 图标 Lucide |

## 实现规模

109 个 Java 源文件（~6.6k LOC）· 14 个实体 / 14 个 Repository / 14 个 Service ·
12 个 Controller / **47 个端点** · 38 个 DTO · 4 个 Flyway 迁移 / **14 张表**

## 包结构

```
com.nijat.ptlt
├── auth          # 认证授权（JWT、refresh token 轮换）
├── workspace     # 工作空间 + 成员 RBAC（OWNER/EDITOR/VIEWER）
├── daily         # 日常记录、稳定度分数、streak
├── routine       # 例行事项定义与完成记录
├── task          # 任务、任务实例、时间块
├── bootcamp      # 训练营模块、每日模块记录
├── analytics     # 聚合分析、Dashboard、告警、日程槽位
├── sync          # 同步机制（pull 部分实现，push 未实现）
└── common        # 异常体系、统一响应封装
```

`insight`（SDD 中的 Phase 4）从未实现。

## 数据库表

| 迁移 | 表 |
|---|---|
| `V1__initial_schema.sql` | users, workspaces, workspace_members, refresh_tokens |
| `V2__phase1_daily_tracking.sql` | routine_definitions, routine_completions, daily_records |
| `V3__phase2_tasks_time_blocks.sql` | tasks, task_instances, time_blocks |
| `V4__phase1_supplement_bootcamp_alerts_schedule.sql` | bootcamp_modules, daily_module_entries, alerts, schedule_slots |

所有业务实体带 `workspace_id`（索引）、`version`（乐观锁）、`deleted_at`（软删除）。

---

## ⚠️ Known Issues

如实记录，未修复。

- **核心功能没有录入入口。** 稳定度分数、当前/最长 streak、Dashboard 图表、整个 Analytics 页
  全部读取 `DailyRecord`，但**没有任何 React 页面能创建它**。`DailyRecordController` 的 4 个端点
  存在且可用，前端从不调用。这是一个为不存在的数据做的仪表盘。
- **前端只覆盖了一部分后端。** 已有页面：Login、Register、Dashboard、Tasks、Routines、Analytics。
  **没有 UI** 的模块：DailyRecord、TimeBlock、ScheduleSlot、Bootcamp、Alert、Workspace 管理、Sync。
- **`SyncService.push` 尚未实现，现在返回 501。** 它此前校验成员身份后完全忽略请求体，返回
  `pull()` 的结果包在 `ApiResponse.success()` 里 —— 离线客户端会收到 200 和一个新游标，认为
  队列已被接受并清空发件箱，实际什么都没持久化。已改为明确失败。另外 `pull()` 只覆盖
  14 张表中的 2 张，所以离线优先这条路本来也还没打通。
- **后端 jar 不打包前端。** `pom.xml` 没有 `frontend-maven-plugin`，而 `SecurityConfig` 放行了
  `/`、`/index.html`、`/*.js`、`/*.css` —— 所以直接跑后端 serve 的是
  `src/main/resources/static/` 里那个更早的 vanilla JS 版本，不是 `frontend/` 里的 React 应用。
  两个 UI 并存，赢的是被废弃的那个。
- **测试极少。** 目前只有 `JwtUtilTest`（11 个用例，覆盖认证层的两个 critical 缺陷）。
  Service 层、Controller 层、多租户隔离规则均无测试覆盖。`pom.xml` 已声明 Testcontainers
  依赖但尚未使用 —— 真正验证 14 张表的 Flyway 迁移与 JPA 实体是否匹配需要它。
- 前端从不存储/使用 refresh token，因此每 15 分钟（access token TTL）强制登出一次。
- 前端 API client 把每个响应体打到 console，其中包含登录返回的 bearer token。

### Security（2026-08-09 审计 —— critical / high 已修复）

多租户隔离本身**实现正确** —— 11 个 Service 都定义了 `validateWorkspaceAccess`，Repository 查询
一致地按 `workspace_id` 过滤。问题都在认证层，现已全部修复：

| 严重度 | 问题 | 修复 |
|---|---|---|
| critical | `application.yml` 提交了可用的 JWT 默认密钥且无启动守卫。每个 Controller 的 `userId` 和 `workspaceId` 都只从 token 派生，这一个密钥是全部认证和全部租户隔离的根 —— 伪造 token 可绕过上面那层正确的隔离 | 删除默认值；`JwtUtil` 在构造时拒绝空值、<32 字节、以及占位符密钥。**旧密钥视为已泄露** |
| critical | `validateToken` 不检查 `type` claim，refresh token 可当 access token 用；登出形同虚设，被撤销的 token 7 天内仍能调 `DELETE /workspaces/{id}` | access token 打上 `type=access`；拆分为 `validateAccessToken` / `validateRefreshToken` |
| high | workspace 软删除只置 `workspaces.deleted_at`，成员行不动，`findFirstByOwnerId` 也不过滤 —— 重新登录后数据照常可访问 | 同事务软删成员行；登录查询加 `AndDeletedAtIsNull` |
| high | `invite` 只挡 VIEWER，EDITOR 可邀请一个 OWNER，两步提权到删库 | 只有 OWNER 能授予 OWNER |
| high | CORS `allowedOriginPatterns=*` + `allowCredentials=true` | 改为 `app.cors.allowed-origins` 显式列表 |
| high | `SyncService.push` 静默丢弃客户端数据并返回 200 | 改为抛 `NotImplementedException` → **501** |

测试：`JwtUtilTest` 11 个用例覆盖上述两个 critical 缺陷。这是本仓库第一批自动化测试。

---

## 快速开始

### 前置要求

Java 21+ · Maven 3.8+ · PostgreSQL 12+ · Node 20+

### 环境变量

| 变量 | 必需 | 用途 |
|---|---|---|
| `JWT_SECRET` | **是** | JWT 签名密钥，至少 32 字节。无默认值，未设置时应用**拒绝启动** |
| `SPRING_DATASOURCE_URL` | 否 | 默认 `jdbc:postgresql://localhost:5432/ptlt` |
| `DB_USERNAME` / `DB_PASSWORD` | 否 | 默认 `postgres` / `postgres` |
| `APP_CORS_ALLOWED_ORIGINS` | 否 | 默认本地开发端口；部署时必须设为真实域名 |

生成密钥：

```bash
openssl rand -base64 48
```

### 数据库

```sql
CREATE DATABASE ptlt;
```

Flyway 会在启动时自动执行迁移。

### 运行后端

```bash
mvn spring-boot:run
```

### 运行前端

```bash
cd frontend
npm install
npm run dev
```

---

## 主要 API 端点

```
POST   /api/v1/auth/register            注册
POST   /api/v1/auth/login               登录
POST   /api/v1/auth/refresh             刷新 token
POST   /api/v1/auth/logout              登出

GET    /api/v1/workspaces               列表
POST   /api/v1/workspaces               创建
GET    /api/v1/workspaces/{id}          详情
PUT    /api/v1/workspaces/{id}          更新（校验 version）
DELETE /api/v1/workspaces/{id}          软删除
POST   /api/v1/workspaces/{id}/invite   邀请成员

GET/POST/PUT/DELETE  /api/v1/daily-records      日常记录（无前端 UI）
GET/POST/PUT/DELETE  /api/v1/routines           例行事项
GET/POST/PUT/DELETE  /api/v1/tasks              任务
GET/POST/...         /api/v1/time-blocks        时间块（无前端 UI）
GET                  /api/v1/analytics/...      分析
GET                  /api/v1/dashboard          仪表盘

POST   /api/v1/sync/pull?since={cursor}  同步拉取（仅覆盖 2/14 张表）
POST   /api/v1/sync/push                 同步推送（占位实现，见 Known Issues）
```

完整设计见 [`SDD.md`](SDD.md)。
