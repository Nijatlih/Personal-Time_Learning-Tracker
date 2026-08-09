# CLAUDE.md

## 这是什么

PTLT (Personal Time & Learning Tracker) —— 个人时间、学习与系统稳定度追踪。
Spring Boot 3.2.0 / Java 21 后端（`src/`，109 文件，47 端点，14 张表，workspace 级多租户
+ OWNER/EDITOR/VIEWER RBAC）+ React 19 + Vite 8 前端（`frontend/`，纯 JS，6 个页面）。

## 定位：自用工具，不做成产品

见 `adr/0001-personal-tool-not-a-product.md`。**不做**多人协作、邀请流、计费、移动端 App，
也不做 SDD 里的 Phase 4（`insight`，AI 洞察）。

## 动手前先读

`docs/AUDIT_2026_08_09.md` —— 完整审计，含 **14 条未修复缺陷**（1 high / 10 medium / 3 low）
的文件位置与失效场景，以及按优先级排的下一步。

**最高优先级的两件事**（约 2–3 天，能把项目从骨架变成每天真能用）：
1. **补 `DailyRecord` 录入页。** 稳定度分数、当前/最长 streak、Dashboard 图表、整个 Analytics 页
   全部读 `DailyRecord`，而**没有任何前端页面能创建它**。`DailyRecordController` 的 4 个端点已就绪。
2. **决定 sync 的去留。** `push` 现在返回 501（此前静默丢数据并返回 200）；`pull` 只覆盖
   14 张表中的 2 张。要么真正实现，要么整块删掉。

唯一未修的 high：乐观锁是"可选的" —— 6 个更新路径里不传 `version` 就静默跳过冲突检测
（`task/service/TaskService.java:115`）。

## 两个已知陷阱

- **后端 serve 的是错的前端。** `pom.xml` 没有 `frontend-maven-plugin`，React 应用不进 jar；
  `SecurityConfig` 放行了 `/index.html`，所以直接跑后端 serve 的是 `src/main/resources/static/`
  里更早的 vanilla JS 版本。
- 前端 lint 还有 5 个错误，CI 的 lint 步骤设为 `continue-on-error`，原因写在 `.github/workflows/ci.yml`
  的注释里。

## 命令

```bash
mvn -B clean verify                      # 11 个测试（JwtUtilTest）
cd frontend && npm ci && npm run build
```

`JWT_SECRET` 必填 —— 无默认值，未设置或使用占位符时**拒绝启动**（至少 32 字节）。

## 约定

- 提交信息：**不加 `Co-Authored-By: Claude`、不加 "Generated with Claude Code"** 等任何 AI 尾注。
- 修源码来满足 linter，**不要放宽 `eslint.config.js`**（仓库有 hook 会拦）。
- Lombok 版本走 `${lombok.version}`，1.18.36 及以下在 JDK 24+ 上会静默编译失败
  （forked javac 会吞掉全部诊断，只剩一句 "Compilation failure"）。
- 本项目 Java 21，ContentFlow 是 17。不要对齐 —— 见 `adr/0002-separate-repositories.md`。

## 相关

`Nijatlih/ContentFlow-AI` 是同一作者的另一个项目。技术栈几乎相同，但零共享代码，
**刻意不合并**。不要试图把两者整合。
