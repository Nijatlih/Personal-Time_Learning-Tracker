# Schedule Phase A（日程执行层）设计

## Context

`docs/VISION_ROADMAP.md` 把 Phase A 列为优先级最高的远景方向：后端已有完整的
`TimeBlock`（某天的时间块，可选关联 Task）和 `ScheduleSlot`（每周固定模板）CRUD
API，但前端从未使用过这两套接口。用户已确认开始 Phase A，本文档记录经过
brainstorming 确认的设计。

**这不是新后端能力，是把已经存在、已经测试覆盖的两套 API 接上前端。**

## Scope

**做的事**（对应 `VISION_ROADMAP.md` Phase A 的六条）：
- 今日时间轴
- 周视图
- 创建、编辑（含删除）时间块
- 固定每周日程模板的管理（查看/增删改）
- 时间块关联 Task
- 计划时间汇总

**明确不做**（brainstorming 过程中逐条确认，避免过度设计）：
- 模板不支持"一键应用到某天"自动生成 TimeBlock——模板是纯参考列表，创建实际时间块仍是手动操作
- 时间块关联 Task 后，块上不提供"直接标记完成"的快捷操作——完成任务仍去 Tasks 页
- 编辑时间块不支持修改日期——挪到别的日子这版不做
- 今天视图不做日期前进/后退——想看别的日子用周视图
- 时间汇总只显示总时长（如 "6h 30m"），不按任务关联与否拆分，纯前端对已加载数据求和
- 不改动任何后端代码（entity、controller、service、migration 均不变）

## Information Architecture

- Sidebar 新增一个 **Schedule** 入口，路由 `/schedule`，图标用 `CalendarClock`
  （lucide-react，其余页面同款图标库）
- 页面内用一个分段控件在三个视图间切换：**今天 / 本周 / 模板**
- 三个视图共享同一个页面级 `activeTab` state，不放进 URL query（和现有页面一样，
  没有页面把子视图状态放进 URL）

## Data Layer

不新增、不修改任何后端端点。新增两个前端 API client，形状照抄
`frontend/src/api/TaskService.js`：

```
frontend/src/api/TimeBlockService.js
  getByDate(date)        → GET /time-blocks/by-date?date=
  getByDateRange(s, e)   → GET /time-blocks/range?startDate=&endDate=
  create(data)           → POST /time-blocks
  update(id, data)       → PUT /time-blocks/{id}   (data 必须带 version)
  delete(id)             → DELETE /time-blocks/{id}

frontend/src/api/ScheduleSlotService.js
  list()                 → GET /schedule-slots
  create(data)           → POST /schedule-slots
  update(id, data)       → PUT /schedule-slots/{id}  (data 必须带 version)
  delete(id)             → DELETE /schedule-slots/{id}
```

## Component Breakdown

```
components/schedule/TimelineGrid.jsx     — 共享的刻度轴渲染
components/schedule/TimeBlockModal.jsx   — 创建/编辑 TimeBlock 的弹窗表单
components/schedule/ScheduleSlotModal.jsx— 创建/编辑 ScheduleSlot 的弹窗表单
pages/Schedule.jsx                       — 容器：Tab 切换
pages/Schedule/TodayView.jsx
pages/Schedule/WeekView.jsx
pages/Schedule/TemplatesView.jsx
```

**`TimelineGrid`** 是核心共享件：接收 `days: [{ date, blocks }]`，画左侧
小时刻度 + 每个 day 一列、按 `(startTime, endTime)` 换算成 `top`/`height` 的
绝对定位色块。今天视图传一个 `day`，周视图传七个——今天视图是周视图的单列
特例，定位换算逻辑只写一次。

刻度轴固定覆盖完整一天 `00:00`–`24:00`（不是某个"典型工作时段"窗口），
这样任何 `TimeBlock` 都落在网格内、不存在"块超出范围显示不出来"的情况；
默认滚动位置定在 `07:00`（`useRef` + 挂载后 `scrollTop` 一次），避免用户
一打开就要往下滚才能看到白天的安排。空白处点击按 Y 坐标换算时间时，
吸附到最近的 **15 分钟**刻度（`Math.round(minutes / 15) * 15`），预填的
默认时长是 1 小时。点击色块之间的空白格子会打开创建模式，点击已有色块会带上
该块数据打开编辑模式，两种情况都触发同一个 `onSlotAction(date, time,
existingBlock?)` 回调，由父视图决定 `TimeBlockModal` 的模式。

**`TodayView`**：固定用系统当天日期（不做日期导航），`useEffect` 里调
`TimeBlockService.getByDate(today)`，取数模式照抄现有页面的
`reloadCount` + `.then/.catch` 形状（`Tasks.jsx` 是参照）。头部显示
"今天已排 Xh Ym"，纯前端对已加载的 blocks 求和 `(endTime - startTime)`。

**`WeekView`**：维护 `weekOffset` state（0 = 本周），据此算出周一到周日的日期范围，
调 `TimeBlockService.getByDateRange(start, end)`，按日期分组喂给
`TimelineGrid`。头部有"上一周/下一周"按钮和"本周已排 Xh Ym"。

**`TemplatesView`**：不用 `TimelineGrid`——模板是参考列表，不需要时间轴视觉。
调 `ScheduleSlotService.list()`，按 `dayOfWeek`（周一到周日）分组渲染成列表段落，
每条一个编辑入口，顶部一个"新增模板"按钮打开 `ScheduleSlotModal`。

**`TimeBlockModal`**：字段为开始时间、结束时间、标题、描述、Task 下拉（可选）。
Task 下拉数据来自 `TaskService.list()`，前端过滤掉 `status === 'COMPLETED'`
和 `'ARCHIVED'`（后端 `list(status)` 只接受单个状态值，这里要 TODO +
IN_PROGRESS 两种，所以不传 status、拿全量后前端过滤）。日期不在表单里，
由调用方（点击的格子 / 正在编辑的块）决定。

**`ScheduleSlotModal`**：字段为星期几（下拉）、开始时间、结束时间、标题、描述。

## Interaction Details

- 点时间轴空白处 → 换算出日期+时间 → 打开 `TimeBlockModal` 创建模式，预填该时间
  起 1 小时的默认时长
- 点已有色块 → 打开同一个 Modal 编辑模式，带上当前 `version`
- 提交更新走 `PUT + version`；冲突（409）按仓库现有约定处理：
  `alert(err.message)` 后触发 reload，不做自动合并 UI（`Tasks.jsx` 的
  `handleToggleComplete` 失败分支是这个模式的参照）
- 删除：编辑弹窗内放一个删除按钮，确认后调 `delete(id)` 并 reload
- 所有取数遵循 `CLAUDE.md` 的强制形态：effect 里直接 `.then/.catch`，setState
  放进 promise 回调，用 `reloadCount` 触发手动刷新，不额外导出取数函数

## Testing / Verification

- 前端：`cd frontend && npm run lint && npm run build`——lint 是阻塞项，新代码
  必须过 5 页面已经在用的 hooks 规则
- 手动验证（`run` skill 起前后端）：
  - 今天视图创建一个时间块、编辑、删除，确认乐观锁 version 递增后仍可连续编辑
  - 周视图前进/后退，确认跨周数据正确（复用的是已有集成测试覆盖的
    `getByDateRange` 端点）
  - 模板视图新增一条每周固定安排，确认它不会自动出现在今天/周视图的实际数据里
    （因为没有"应用"这回事——这是本设计明确排除的功能，用来验证边界没做反）
  - Task 下拉只列出未完成的任务，关联后色块上正确显示只读的任务标题
- 不需要新的后端集成测试——没有改动任何后端代码
