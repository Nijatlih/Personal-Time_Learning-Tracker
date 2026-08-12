# 下一步 — 2026-08-12

两轮审计的 35 条缺陷已全部关闭（见 [`AUDIT_2026_08_09.md`](AUDIT_2026_08_09.md) 第 4 节与
[`AUDIT_2026_08_10.md`](AUDIT_2026_08_10.md)）。本文件记录**修复过程中发现但本轮没做**的事项，
按优先级排。

每一条都写了「为什么现在没做」—— 有些是取舍未定，有些是范围问题，不要把它们当成漏掉的活。

---

## P1 — Analytics 页有两段编造的数据当真实统计展示

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

## P2 — `POST /api/v1/workspaces` 能创建一个此后永远访问不到的 workspace

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

**为什么本轮没做**：需要产品决定，不是缺陷修复。

---

## P3 — 10 个 controller 各自重复解析 JWT

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

---

## P4 — 前端 5 个 lint 错误，CI 的 lint 步骤仍是 `continue-on-error`

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

## P5 — 零散项

- **`frontend/index.html:5` 引用 `/favicon.svg`，但 `frontend/public/` 目录不存在** ——
  每次加载都 404。要么放一个图标进去，要么删掉这一行。
- **`daily_records.green_streak` / `uptime_streak` 两列是否还该存在。**
  所有读路径（`getByDate`、`getByDateRange`、`getDashboard`）都会**实时重算** streak，
  写路径也会算一遍存进去。于是这两列的值随时可能与返回值不一致，而没有任何东西读它们。
  它们要么该变成纯派生（删列），要么该成为唯一真相（读路径不再重算）。
  现在是两者之间最糟的状态。
  注意：读路径是在 `@Transactional(readOnly = true)` 里改实体的 ——
  因为 readOnly 会把 FlushMode 设成 MANUAL，所以那些改动不会被写回库。
  这是它今天没出问题的原因，但它依赖一个不显眼的机制。
- **`getRoutineCompletionStats` 的 `totalDays` 用的是日历天数**，而不是"有 routine 定义的天数"。
  routine 定义是在某天创建的，创建之前那些天算作"未完成"，会把完成率往下拉。
  与 F3 同一类问题（分母口径），但影响小得多，且需要先决定语义。

---

## 不要再查的地方

见 `AUDIT_2026_08_10.md` 第 3 节「已排除」。简要重述：实体↔schema 已逐列比对且现在有
`SchemaContractTest` 机械守护；Jackson 序列化干净（无双向关联）；service 全部无状态单例；
每个收 `@RequestBody` 的方法都有 `@Valid`；软删除与唯一约束的交互已确认不可达。
