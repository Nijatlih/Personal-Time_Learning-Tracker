# 下一步 — 2026-08-12

两轮审计的 35 条缺陷已全部关闭（见 [`AUDIT_2026_08_09.md`](AUDIT_2026_08_09.md) 第 4 节与
[`AUDIT_2026_08_10.md`](AUDIT_2026_08_10.md)）。本文件记录**修复过程中发现但本轮没做**的事项，
按优先级排。

每一条都写了「为什么现在没做」—— 有些是取舍未定，有些是范围问题，不要把它们当成漏掉的活。

> **2026-08-12 同日更新**：**P1–P5 全部完成。** P5 里关于 streak 两列的那条
> **是我写错了**，已在原处标注撤销并说明原因。下面各节保留原文并在标题上标了状态。
>
> **2026-08-20 更新**：`totalDays` 口径也已确定并落地。Routine 创建前的日期不算未完成，
> Morning 与 Evening 分别返回自己的有效天数分母。当前清单已全部关闭。

---

## ~~P1~~ ✅ 已修（`a56285d`）— Analytics 页有两段编造的数据当真实统计展示

**位置**：`frontend/src/pages/Analytics.jsx:138` 与 `:146`

```
Your task completion speed is optimal. Focus on maintaining High Priority tasks.
Your morning routines have a 85% consistency rate. Keep it up!
```

两段都是**硬编码的文案**，不来自任何接口。"85%" 是一个凭空写死的数字，会被当成用户自己的
真实统计来读。第二句里的 "High Priority tasks" 更是指向一个后端从来没有的概念 ——
`priority` 是那个已删除的陈旧 SPA 的幻影字段（见 F8）。

**为什么比 F6 更严重**：F6 显示的是 `+undefined Stability`，一眼就知道坏了；
"85% consistency rate" 看起来完全合理，因此会被相信。**报假数比报错更糟。**

**修法（成本很低，后端已就绪）**：
`AnalyticsService.getTaskStats` / `getRoutineStats`（`frontend/src/api/AnalyticsService.js:7,10`）
定义了但**零调用点**，而它们对应的后端端点在本轮已经修好（F3 的分母问题）。
把这两段文案换成真实数据：

- Task Velocity → `GET /api/v1/analytics/tasks/completion-stats` 的 `completionRate`
  与 `coreTaskCompletionRate`
- Habit Consistency → `GET /api/v1/analytics/routines/completion-stats` 的
  `morningCompletionRate` / `eveningCompletionRate`

数据为空时显示"数据不足"，**不要**回退到一句听起来正确的话。

**为什么本轮没做**：本轮的授权范围是修缺陷 + 补录入页；这一条是在最后复核时才确认的。

---

## ~~P2~~ ✅ 已修 — `POST /api/v1/workspaces` 能创建一个此后永远访问不到的 workspace

**位置**：`workspace/controller/WorkspaceController.java`（create 端点）、
`auth/service/AuthService.java:106,150`

token 里的 `workspaceId` 只来自
`findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc`（"最早创建的那个"），
而 44 个端点里没有任何一个接受显式的 workspaceId 或能切换当前 workspace。
所以第二个及以后创建的 workspace 一旦建出来就再也访问不到。

更不自洽的是 `WorkspaceService.list()` 会把用户的**全部** workspace 都返回 ——
界面因此可以列出一堆点不进去的东西。

这是 F1/F4 未修的那一半，按 `adr/0003` 记录为"单用户单 workspace"的已接受限制。

**三个选项**：

| 方案 | 代价 |
|---|---|
| A. 删掉 create 端点，`list()` 只返回当前 workspace | 最小、最自洽；如果将来想支持多 workspace 要重新加回来 |
| B. 支持切换：登录/刷新时接受可选的 workspaceId，校验成员身份后写进 token | 改动面涉及 auth + 前端选择器，是新功能而不是修缺陷 |
| C. 保持现状，只在 `list()` 上标注哪个是"活的" | 最省事，但把不自洽留在 API 表面上 |

**倾向 A**，理由与 `adr/0003`、`adr/0004` 一致：不做的功能就整块删掉，不要留半个。
但这需要作者确认，因为它删掉的是一个"看起来该有"的端点。

**落地结果**：作者选了方案 A。已删掉 create 端点、`WorkspaceService.create`、
`WorkspaceCreateRequest`，并把 `list()` 收敛为只返回 token 指向的那一个
（返回类型仍是数组，长度恒为 1，不改 API 形状）。决定记在 `adr/0005-single-workspace-per-user.md`，
含"将来若真要多 workspace，切换能力必须先于创建能力存在"这一条。端点数 44 → 43。

---

## ~~P3~~ ✅ 已修（`bcc8304`）— 11 个 controller 各自重复解析 JWT

**位置**：全部 11 个 controller，每个 2 个私有方法（`WorkspaceController` 只有 1 个）

```java
private UUID getUserIdFromHeader(@RequestHeader("Authorization") String authHeader) {
    String token = authHeader.replace("Bearer ", "");
    return jwtUtil.getUserIdFromToken(token);
}
```

**先说清楚：这目前不是活缺陷。**

- `replace` 是全局替换而非前缀剥离，但 JWT 是 base64url 编码，不可能含空格，
  所以 `"Bearer "` 不会出现在 token 正文里；
- 它也接受没有 `Bearer ` 前缀的 header，但 `JwtAuthenticationFilter:38` 已经强制要求了前缀，
  所以走到 controller 时前缀必然存在。

真正的问题是**重复**：同一个 token 在 filter 里已经解析并验证过一次，controller 又解析两次
（userId 和 workspaceId 各一次），一共三次。21 份几乎相同的代码，任何一处认证语义的调整
都要记得同步 21 个地方 —— `AuthController.logout` 那条缺陷（F19）就是这个模式的产物。

**修法**：`JwtAuthenticationFilter` 已经把 userId 放进了 `SecurityContext` 的 principal，
但 workspaceId 没有。把两者一起放进去（自定义 `Authentication` 或用 `details`），
controller 改成从 `SecurityContext` 读，或者用一个 `@AuthenticationPrincipal` 解析器 /
`HandlerMethodArgumentResolver` 直接注入。

**为什么本轮没做**：11 个文件、21 个方法的机械改动，没有对应的行为缺陷。
不过现在有 34 个集成测试兜着，回归风险比本轮开始时低得多 —— 这件事现在做比之前安全。

**落地结果**：新增 `auth/model/AuthenticatedUser`（record：userId + workspaceId），
filter 验证 token 后把它作为 principal 放进 SecurityContext —— 这成为整个应用**唯一**
解析 token 的地方。10 个 controller 的 38 个端点方法改用
`@AuthenticationPrincipal AuthenticatedUser principal`，20 个 helper 全删，不再注入 JwtUtil；
方法体内保留 `UUID userId = principal.userId();` 局部变量，所以业务代码一行未动。

`AuthController` 有意保留读原始 header 的写法：它在 permitAll 路径上、SecurityContext
可能合法为空，且需要区分"没有 Bearer 前缀"和"token 无效"。

这次重构正是测试基线存在的理由：35 个集成测试全绿且一次未改测试代码。

---

## ~~P4~~ ✅ 已修（`0599cbc`）— 前端 5 个 lint 错误，CI 的 lint 步骤仍是 `continue-on-error`

原因写在 `.github/workflows/ci.yml` 的注释里。现在**两条都有了现成的修法**：

- **`react-hooks/set-state-in-effect` ×4**（`Dashboard.jsx:46`、`Routines.jsx:95`、
  `Tasks.jsx:28`、`useAuth.jsx:15`）—— 都是
  `useEffect(() => { fetchX(); }, [fetchX])` 的形状。这条规则是静态的，会标记 effect 体里
  任何间接触发 setState 的调用，与 `await` 出现在第几句无关。
  **仓库里已有两个通过的范例**：`Analytics.jsx` 和本轮新增的 `DailyLog.jsx`
  都改成了在 effect 里直接写 `.then/.catch`，setState 放在 promise 回调里。
  照抄这个形态即可；`DailyLog.jsx` 还多做了一个 `cancelled` 标志处理切换参数的竞态，
  `Dashboard`/`Routines`/`Tasks` 没有会变的查询参数，不需要那一层。
- **`react-refresh/only-export-components` ×1**（`useAuth.jsx:47`）—— 该文件同时导出
  `AuthProvider` 组件和 `useAuth` hook。拆成 `AuthProvider.jsx` + `useAuth.js` 两个文件。

做完之后把 CI 的 `continue-on-error: true` 去掉，让 lint 重新变成阻塞项。

**不要放宽 `eslint.config.js`** —— 修源码，不改规则（仓库有 hook 会拦）。

---

## P5 — ✅ 已完成（favicon、streak 核实、Routine 分母口径）

- ~~**`frontend/index.html:5` 引用 `/favicon.svg`，但 `frontend/public/` 目录不存在**~~ ——
  ✅ 已修（`0599cbc`）：补了一个 SVG 图标，dev server 上从 404 变成 200。
- ~~**`daily_records.green_streak` / `uptime_streak` 两列是否还该存在。**~~
  **这一条我写错了，已撤销。** 去动手时核实了 schema 和实体：`daily_records` 表里
  **根本没有这两列**（`grep -rn streak src/main/resources/db/migration/` 零命中，
  `\d daily_records` 也只有 14 列），而 `DailyRecord` 的两个字段本来就标了 `@Transient`
  （`entity/DailyRecord.java:63,66`，注释写的是"计算字段，不持久化"）。
  也就是说 streak **已经是纯派生值**，正是当时建议的那个终态 —— 不存在"写路径又存一份"
  这回事，也不存在两边不一致的可能。原文关于 readOnly / FlushMode 的那段推理是在一个
  错误前提上展开的。无需任何改动。
- ~~**`getRoutineCompletionStats` 的 `totalDays` 分母口径。**~~ ✅ 已完成：定义创建前的日期
  不进入分母；Morning 与 Evening 可能在不同日期创建，因此 API 改为分别返回
  `morningTotalDays` / `eveningTotalDays`，不再用一个共享的 `totalDays`。新增集成测试守护
  “查询过去 30 天、今天才创建并完成”必须显示 `1/1 = 100%`，而不是 `1/31`。

---

## 不要再查的地方

见 `AUDIT_2026_08_10.md` 第 3 节「已排除」。简要重述：实体↔schema 已逐列比对且现在有
`SchemaContractTest` 机械守护；Jackson 序列化干净（无双向关联）；service 全部无状态单例；
每个收 `@RequestBody` 的方法都有 `@Valid`；软删除与唯一约束的交互已确认不可达。
