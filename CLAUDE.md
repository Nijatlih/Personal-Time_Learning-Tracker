# CLAUDE.md

## 这是什么

PTLT (Personal Time & Learning Tracker) —— 个人时间、学习与系统稳定度追踪。
Spring Boot 3.5.16 / Java 21 后端（`src/`，112 文件，46 端点，14 张表，workspace 级多租户）
+ React 19 + Vite 8 前端（`frontend/`，纯 JS，6 个页面）。

## 定位：自用工具，不做成产品

见 `adr/0001-personal-tool-not-a-product.md`。**不做**多人协作、邀请流、计费、移动端 App，
也不做 SDD 里的 Phase 4（`insight`，AI 洞察）。

按 `adr/0003-drop-invite-and-role-based-access.md`，invite 与 RBAC 已整套删除，
`WorkspaceRole` 只剩 `OWNER`。**不要重新引入角色。**

## 动手前先读

- `docs/AUDIT_2026_08_10.md` —— 第二轮审计与修复记录。21 条新缺陷已全部修完，含逐条实测证据、
  两处审计误判的更正，以及**「已排除 —— 下一轮不要再查」**那一节（避免重复劳动）。
- `docs/AUDIT_2026_08_09.md` 第 4 节 —— 首轮 14 条，其中 9 条已修，**5 条仍开放**且已逐条标注。

**当前最高优先级**（两轮审计后仍未做的事）：

1. **补 `DailyRecord` 录入页。** 稳定度分数、streak、Dashboard 图表、整个 Analytics 页全部读
   `DailyRecord`，而**没有任何前端页面能创建它**。`DailyRecordController` 的 4 个端点已就绪。
   （注：routine 打卡已修好，所以录入页做出来之后稳定度真的能到 100 —— 第二轮之前上限只有 60。）
2. **决定 sync 的去留。** `push` 返回 501；`pull` 只覆盖 14 张表中的 2 张。要么实现，要么删掉。
3. **补测试基线。** 仓库只有 `JwtUtilTest` 11 个纯单元测试，无 Spring 上下文测试。
   两轮修复全靠真实启动 + 手工端到端脚本验证，一次都没沉淀下来。Testcontainers 依赖已在 `pom.xml`。

首轮剩下的 5 条开放缺陷里，`TimeBlockService.java:50` 的跨租户 `taskId` 引用是唯一一处 IDOR。

## 已知陷阱

前端 lint 还有 5 个错误（4× `react-hooks/set-state-in-effect` + 1×
`react-refresh/only-export-components`），CI 的 lint 步骤设为 `continue-on-error`，
原因写在 `.github/workflows/ci.yml` 的注释里。

> 「后端 serve 的是错的前端」这条陷阱**已消除** —— `src/main/resources/static/` 里那份陈旧的
> vanilla JS SPA 已整个删除，`SecurityConfig` 的静态资源放行项也一并移除。
> 后端现在**只提供 API**，前端走 Vite（开发）或独立静态托管（生产）。

## 命令

```bash
mvn -B clean verify                      # 11 个测试（JwtUtilTest）
cd frontend && npm ci && npm run build
```

`JWT_SECRET`、`DB_USERNAME`、`DB_PASSWORD` **三个都必填**，均无默认值
（`JWT_SECRET` 至少 32 字节，且拒绝占位符值）。本地起库：

```bash
docker run -d --name ptlt-pg -e POSTGRES_PASSWORD=ptltdev \
  -e POSTGRES_DB=ptlt -p 55432:5432 postgres:16
```

**PostgreSQL 必须 ≥ 13**（`gen_random_uuid()` 从 13 起才是内置函数，迁移里没有
`CREATE EXTENSION pgcrypto`）。

**`mvn verify` 跑绿证明不了后端能起来** —— 没有 Spring 上下文测试。改动涉及实体、迁移、
配置或 security 时，必须真启动一次。

## API 约定

- 6 个 `PUT` 端点**必须**带 `version`（强制乐观锁）：缺失 400，不符 409 + `error.details.serverState`。
- 认证失败 401、鉴权失败 403，都是 `ApiResponse` 结构。
- 收 `startDate`/`endDate` 的端点：区间必须正序且跨度 ≤ 366 天，否则 400。

## 待决事项

仓库目前是 **private**。作为作品集公开这件事原则上同意，但推迟到未修缺陷处理完之后。
`docs/PRE_PUBLIC_CHECKLIST.md` 记录了待办项和已完成的密钥/隐私扫描结果 —— **不要重复扫描**。
未经作者确认不要改动仓库可见性。

## 约定

- 提交信息：**不加 `Co-Authored-By: Claude`、不加 "Generated with Claude Code"** 等任何 AI 尾注。
- 修源码来满足 linter，**不要放宽 `eslint.config.js`**（仓库有 hook 会拦）。
- Lombok 版本走 `${lombok.version}`，1.18.36 及以下在 JDK 24+ 上会静默编译失败。
  **不要重新加回 `maven-compiler-plugin` 的 `<fork>true</fork>`** —— 它会吞掉全部编译诊断，
  只剩一句 "Compilation failure"。已在 `07a7e98` 删除。
- 本项目 Java 21，ContentFlow 是 17。不要对齐 —— 见 `adr/0002-separate-repositories.md`。

## 相关

`Nijatlih/ContentFlow-AI` 是同一作者的另一个项目。技术栈几乎相同，但零共享代码，
**刻意不合并**。不要试图把两者整合。
