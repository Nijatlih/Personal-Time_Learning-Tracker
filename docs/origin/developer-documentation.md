# 个人时间管理与学习追踪系统 — 开发文档（合并版）

**Personal Time & Learning Tracker (PTLT)**

| | |
|---|---|
| 版本 | 合并版 v1.2（由 v1.0 + v1.1 合成） |
| 原始文档日期 | 2026-01-20 |
| 合并日期 | 2026-08-20 |
| 作者 | Nijat Ehmet（原文档署名「需求分析团队」） |

## 关于这份文档

这份文档由 2026-01-20 同一个下午产出的三份文件合并而成，原件已归档：

| 原件 | 自称版本 | 在本文档中的角色 |
|---|---|---|
| `personal_tracker_doc.md` | v1.0 | 最早的草稿；内容已被 v1.docx 吸收，独有的简版任务模型见[附录 B](#附录-b增强前的原始任务模型) |
| `... Developer Documentation v1.docx` | v1.0 | **功能规格主干**——第 2、5、6、7、8、9、10 章以它为准 |
| `... Developer Documentation v2.docx` | **v1.1 修正版** | **技术修正**——第 3、4 章与状态算法以它为准 |

**为什么要合并：** 名字叫 v2 的那份其实自称 v1.1，是一份**砍掉约三分之二内容**的修正备忘录，不是 v1 的升级。
正文字数 v1 为 13,371、v2 为 7,220。v2 删掉了整个 §2.5 高级功能模块（番茄钟、能量预算、目标树、
干扰追踪、决策日志、数据洞察、协作分享）、§2.3 的五个子章节、附录术语表、四阶段路线图、`src/` 结构等；
但它独有加权评分算法、修正后的 TypeScript 接口、`localStorage` 修正和 `ptlt_*` 存储命名空间。
两者互补，谁都不能单独删——于是合并成这一份。

**⚠️ 这份文档描述的不是任何已实现的代码。** 三条实现线各走各路：

- 本目录的单页应用（`day month year planfiles/`，2026-02-04）用 vanilla JS + Chart.js，
  存储键是 `scheduleData`，**没有**采用这里的 React/Tailwind/Recharts 技术栈或 `ptlt_*` 键
- GitHub `Nijatlih/Personal-Time_Learning-Tracker`（2026-01-23 init，Java 包 `com.nijat.ptlt`）
  是 Spring Boot + PostgreSQL + React，与这里的「浏览器离线、localStorage」前提完全不同
- 本文档描述的这个形态从未被完整实现

它的价值是**设计记录与需求清单**，不是实现说明。`PTLT` 这个代号首次出现在 v1.1 的
[§4 数据存储结构](#4-数据存储结构设计)（`ptlt_settings` / `ptlt_records` / `ptlt_tasks`），
三天后成为 GitHub 仓库的 Java 包名。

## v1.1 修正说明（原文照录）

> 这是基于你提供的原始文档，经过深度修订、Bug 修复和逻辑优化后的完整开发文档 (v1.1)。
>
> 主要修复与改进点说明：
> 1. **严重技术错误修复**：将不存在的 `window.storage` API 更正为标准的 `localStorage`。
> 2. **逻辑漏洞修复**：重写了"每日状态判定"算法。原算法过于严苛（一票否决制），容易挫败用户；
>    新版改为加权评分制，更符合真实的人类行为模式。
> 3. **数据模型补全**：在 TypeScript 接口定义中补全了原文档遗漏的 `quadrant`（象限）、
>    `energyLevel`（能量）等关键字段。
> 4. **结构优化**：增加了版本控制和更清晰的类型定义。

---

## 1. 项目概述

### 1.1 项目名称

Personal Time & Learning Tracker（个人时间与学习追踪系统）

### 1.2 项目目标

构建一个基于浏览器的离线个人追踪系统，用于管理每日时间规划、学习进度、习惯养成，
并通过数据可视化提供系统稳定性分析和预警机制。

### 1.3 核心价值

- 帮助用户建立稳定的学习与生活系统
- 通过数据驱动发现系统退化信号
- 防止认知能量透支
- 长期目标与日常行为的可视化联动

---

## 2. 功能需求

### 2.1 时间规划模块

#### 2.1.1 工作日时间表

核心逻辑：基于时间块（Time Blocking）的固定规划。固定时间段包含以下字段：

| 字段 | 说明 |
|---|---|
| 时间段 | 如 "07:00 – 08:30" |
| 模块名称 | 如 "🟦 个人核心认知时段" |
| 内容定位 | 该时段的具体活动描述 |
| 反人性规则 | 该时段需要遵守的约束条件 |

预设时间段：

| 时间段 | 模块 |
|---|---|
| 07:00 – 08:30 | 🟦 个人核心认知时段 |
| 09:00 – 12:30 | 🎓 Bootcamp 核心教学 |
| 12:30 – 13:30 | 🍱 休息 / 午餐 |
| 13:30 – 18:30 | 🛠️ Bootcamp 实操与项目 |
| 19:30 – 20:30 | 📝 结构化沉淀 |
| 20:30 – 22:30 | 🛌 无负罪休息 |

功能要求：

- 支持编辑时间段、名称、内容和规则（增删改查）
- 支持添加/删除时间段
- 每日查看当前时段提醒
- 反人性规则可配置，如"此时段禁手机"

> 数据结构见 [`interface TimeSlot`](#321-时间表timeslot)。

#### 2.1.2 晨间 / 日间 / 夜间例程

**🌅 晨间例程（60–90 分钟，目标：激活）**

- [ ] 起床 → 喝水
- [ ] 2 分钟感恩练习
- [ ] 10 分钟正念冥想
- [ ] 30 分钟运动/拉伸/散步
- [ ] 10 分钟规划今日
- [ ] 定义 1 个核心任务
- [ ] 定义 3 个不可妥协事项

**🧠 日间例程**

- [ ] 遵循预定义时间表
- [ ] 单标签页原则
- [ ] 多听少说
- [ ] 不重新规划

**🌙 夜间例程（30–45 分钟，目标：复盘）**

- [ ] 10 分钟写作（今天什么强化了系统？）
- [ ] 10 分钟阅读（慢速、非刺激性）
- [ ] 10 分钟整理空间
- [ ] 写下明天的 3 个不可妥协事项

功能要求：

- 每日勾选完成状态
- 记录完成时间（可选）
- 计算例程完成率

> v1.1 把日间例程整块删掉了，这里按 v1.0 保留——它是"单标签页原则"等反人性规则的唯一出处。

### 2.2 Bootcamp 模块追踪系统

#### 2.2.1 模块列表

| 模块 ID | 模块名称 | 核心内容 | 典型行为 |
|---|---|---|---|
| M1 | Foundations | Python, Math/Statistics, ML 基本概念 | Understand, Clarify, Build mental models |
| M2 | Data | Data cleaning, Feature engineering, EDA | Inspect, Clean, Transform, Validate |
| M3 | Machine Learning | Classical ML, Training/Evaluation, Metrics | Train, Evaluate, Compare, Debug |
| M4 | Deep Learning | Neural networks, CNN/RNN/Transformer | Implement, Tune, Experiment |
| M5 | Projects | Individual/Group projects, Capstone | Design, Integrate, Deliver |
| M6 | Engineering & MLOps | Git, Pipelines, Deployment, Automation | Refactor, Automate, Stabilize |
| M7 | Review & Consolidation | 回顾、笔记结构化、方法论抽象 | Compress, Abstract, Document |

#### 2.2.2 每日记录字段

- **日期**：YYYY-MM-DD
- **主要模块**：当日主要学习的模块（可多选 M1–M7）
- **贡献类型**：
  - 🧠 认知建设（深度理解、模型构建）
  - 🛠️ 工程实践（编码、调试、部署）
  - 📊 项目推进（集成、交付）
  - 📝 结构化沉淀（笔记、总结、复盘）
- **完成状态**：未开始 / 进行中 / 已完成
- **笔记**：自由文本输入（支持 Markdown）
- **认知能量水平**：低 / 中 / 高
- **是否与长期目标对齐**：是 / 否

### 2.3 任务与计划管理（增强版）

#### 2.3.1 第一性原理任务分类系统

基于艾森豪威尔矩阵的四象限分类：

| 象限 | 定义 | 行动原则 | 典型任务 |
|---|---|---|---|
| 🔴 Q1：重要且紧急 | 必须立即处理的危机 | 立即执行 | Bootcamp 截止项目、系统崩溃修复 |
| 🟢 Q2：重要但不紧急 | 长期价值建设 | 计划执行（**核心区**） | 深度学习、系统优化、认知建设 |
| 🟡 Q3：紧急但不重要 | 干扰性事务 | 委托/快速处理 | 临时会议、非关键邮件 |
| ⚪ Q4：不重要不紧急 | 时间浪费 | 削减/消除 | 无目的浏览、过度娱乐 |

智能分类辅助问题：

1. "如果不做这件事，会有什么后果？"（判断重要性）
2. "这件事必须在什么时候完成？"（判断紧急性）
3. "这件事是否与我的长期目标对齐？"（判断价值）
4. "这件事是否能由他人/工具完成？"（判断必要性）

#### 2.3.2 增强任务字段

**基础信息**

- 任务名称（必填）
- 任务描述（可选，支持 Markdown）
- 创建时间（自动）
- 最后更新时间（自动）

**分类维度**

- 象限分类：Q1/Q2/Q3/Q4（必填，带智能建议）
- 优先级：P0（最高）/ P1（高）/ P2（中）/ P3（低）
- 能量需求：🔋 高能量 / ⚡ 中能量 / 🌱 低能量
- 任务类型：
  - 🎯 单次任务（one-off）
  - 🔁 重复任务（recurring）
  - 🧩 项目任务（multi-step project）
  - 🌊 习惯养成（habit building）

**时间管理**

- 预估耗时：15 分钟 / 30 分钟 / 1 小时 / 2 小时 / 半天 / 全天
- 截止日期：可选，支持日期 + 时间
- 提醒设置：提前 1 天 / 提前 3 小时 / 当天早上 7:00 / 自定义时间
- 最佳执行时段：根据能量需求建议（如高能量任务 → 建议 07:00–08:30）

**关联与依赖**

- 关联 Bootcamp 模块：M1–M7（多选）
- 关联例程：晨间 / 日间 / 夜间
- 依赖任务：必须先完成哪些任务（可选）
- 标签系统：自定义标签（如 `#深度工作` `#快速处理` `#等待他人`）

**执行追踪**

- 状态：📋 待开始 / 🏃 进行中 / ⏸️ 暂停 / ✅ 已完成 / ❌ 已取消
  （v1.1 的类型定义收敛为 `todo | in_progress | completed | archived`，见 [§3.2.2](#322-任务task-已修复缺失字段)）
- 实际耗时：记录实际花费时间（用于修正预估偏差）
- 完成度：0%–100%（用于大型项目）
- 执行记录：每次开始/暂停/完成时自动记录时间戳

#### 2.3.3 智能任务管理功能

**1. 智能添加任务向导**

```
步骤1：输入任务名称
步骤2：AI辅助分类
  - 系统分析关键词自动建议象限（如"紧急"→Q1，"学习"→Q2）
  - 显示4个象限定义，用户确认或调整
步骤3：智能建议
  - 根据当前时间和个人时间表，建议最佳执行时段
  - 根据Bootcamp进度，建议关联模块
  - 根据历史数据，建议预估耗时
步骤4：设置提醒（可选）
步骤5：确认创建
```

**2. 批量操作**

- 批量移动到不同象限
- 批量设置优先级
- 批量标记完成/删除
- 批量导出到日历

**3. 任务自动归档**

- 已完成任务 7 天后自动归档
- 归档任务可搜索和查看
- 支持恢复归档任务

**4. 任务模板系统**

- 保存常用任务为模板
- 预设模板：
  - "Bootcamp 每日复习"（Q2，1 小时，晚间）
  - "每周项目检查"（Q2，2 小时，周日）
  - "紧急 Bug 修复"（Q1，预估 2 小时）
- 一键从模板创建任务

#### 2.3.4 时间块分配（Time Blocking）

**功能说明：** 将任务分配到具体时间段，实现从"计划"到"执行"的闭环。

核心特性：

- **拖拽分配**：从任务列表拖拽到时间表的具体时段
- **容量检查**：每个时段显示已分配任务总耗时，超出时段容量时警告
- **自动建议**：
  - 高能量任务 → 自动建议 07:00–08:30
  - 工程任务 → 自动建议 13:30–18:30
  - 复盘任务 → 自动建议 19:30–20:30
- **冲突检测**：同一时段任务过多时高亮提示

视图模式：

- 日视图：显示今日所有时间块
- 周视图：一周 7 天的时间块分布
- 月视图：显示每天的任务密度热力图

#### 2.3.5 任务过滤与视图

**快速过滤器**

- 按象限查看（Q1/Q2/Q3/Q4）
- 按优先级查看
- 按能量需求查看
- 按 Bootcamp 模块查看
- 按状态查看（今日/本周/已逾期/已完成）
- 按标签查看

**智能视图**

- **今日聚焦**：显示今日必须完成的任务（Q1 + 今日截止的 Q2）
- **本周计划**：显示本周所有任务的甘特图
- **能量匹配**：根据当前时间和能量水平，推荐最适合现在做的任务
- **逾期任务**：高亮显示所有逾期未完成任务
- **Q2 关注区**：专门显示重要但不紧急的长期价值任务（防止被忽略）

#### 2.3.6 重复任务系统

**重复规则**

- 每日（如每日晨间例程检查）
- 每周特定日期（如每周日复盘）
- 每月特定日期（如每月 1 号月度规划）
- 自定义间隔（如每 3 天复习一次）

**智能生成**

- 自动在截止日期前生成下一个实例
- 保留历史完成记录
- 支持批量调整未来实例

**跳过与调整**

- 允许跳过某次重复（记录原因）
- 允许临时调整某次的时间/内容

#### 2.3.7 多维度计划层级

**日计划（Daily）**

- 来源：从周/月计划自动分解 + 临时添加
- 特点：具体可执行，有明确时间块
- 容量限制：根据能量预算，每日建议 3–5 个核心任务

**周计划（Weekly）**

- 每周日生成下周计划
- 将大型任务分解为每日子任务
- 设置本周 OKR（1–3 个关键结果）

**月计划（Monthly）**

- 每月初设定月度目标
- 关联到 Bootcamp 模块进度
- 月末自动生成完成度报告

**季度/年度计划（Long-term）**

- 设定长期目标（如完成 Bootcamp、掌握某技术栈）
- 自动提示目标进度
- 支持里程碑标记

### 2.4 数据分析与可视化

#### 2.4.1 核心指标

| 指标名称 | 计算逻辑 | 用途 |
|---|---|---|
| 连续稳定天数（Streak） | 连续状态非 Gray 的天数 | 判断系统是否形成惯性 |
| 稳定度折线图 | 每日稳定性评分（Stability Score，0–100）的时间序列 | 识别系统进化/退化趋势 |
| 灰色提醒 | 连续 2 天灰色状态触发警报 | 系统降载预警 |
| 认知能量趋势 | 认知能量水平的统计分布 | 判断是否透支 |
| Bootcamp 完成率 | 各模块完成任务数 / 总任务数 | 目标对齐度检查 |

> 「连续稳定天数」的定义在 v1.1 改过：v1.0 是"连续绿色天数"，v1.1 放宽为"连续非灰色天数"。
> 这里采用 v1.1 的定义。

#### 2.4.2 每日状态判定规则（v1.1 加权评分制）

**这是现行规则。** 稳定性评分 0–100，每日计算：

| 项 | 权重 | 计分方式 |
|---|---|---|
| 晨间例程完成率 | 20 | `completionRate × 20` |
| 夜间例程完成率 | 20 | `completionRate × 20` |
| 核心任务完成 | 30 | 完成得 30，否则 0（底线项） |
| 认知能量 | 15 | 高 15 / 中 10 / 低 0 |
| 与长期目标对齐 | 15 | 对齐得 15，否则 0 |

状态判定：

- 🟢 **Green（健康）**：评分 ≥ 80
- 🟡 **Yellow（波动）**：50 ≤ 评分 < 80
- ⚫ **Gray（风险）**：评分 < 50 → **触发预警**

实现见 [§3.3.1](#331-每日状态计算加权评分制)。

<details>
<summary><b>已废弃：v1.0 的一票否决制规则（保留作为设计沿革）</b></summary>

v1.0 用的是全与无逻辑，v1.1 认为"过于严苛，容易挫败用户"而重写。原规则：

**🟢 绿色状态（系统健康）** — 需**同时**满足：

- 晨间例程完成率 ≥ 80%
- 夜间例程完成率 ≥ 80%
- 至少完成 1 个核心任务
- 认知能量 ≥ 中等
- 与长期目标对齐 = 是

**🟡 黄色状态（系统波动）** — 满足以下任意 2–3 项：

- 例程完成率 50–79%
- 核心任务完成
- 认知能量 = 中等

**⚫ 灰色状态（系统风险）**：

- 晨间或夜间例程完成率 < 50%
- 未完成核心任务
- 认知能量 = 低
- 与长期目标对齐 = 否

原始判定代码（一票否决制）：

```js
function calculateDailyStatus(record) {
  const { morning, evening } = routines;
  const greenCriteria =
    morning.completionRate >= 0.8 &&
    evening.completionRate >= 0.8 &&
    record.coreTaskCompleted &&
    record.cognitiveEnergy !== "low" &&
    record.alignedWithGoals;

  if (greenCriteria) return "green";

  const yellowCount = [
    morning.completionRate >= 0.5,
    evening.completionRate >= 0.5,
    record.coreTaskCompleted,
    record.cognitiveEnergy === "medium"
  ].filter(Boolean).length;

  return yellowCount >= 2 ? "yellow" : "gray";
}
```

对应的 v1.0 版 `calculateStreak`（按"连续绿色"而非"连续非灰色"计数）：

```js
function calculateStreak(records) {
  let streak = 0;
  const sortedDates = Object.keys(records).sort().reverse();

  for (const date of sortedDates) {
    if (records[date].dailyStatus === "green") {
      streak++;
    } else {
      break;
    }
  }
  return streak;
}
```

</details>

#### 2.4.3 周 / 月视图

**周总结**

- 本周绿色天数 / 黄色天数 / 灰色天数
- 主要学习模块分布（饼图）
- 认知能量平均值
- 完成的核心任务数

**月总结**

- 月度稳定度趋势图
- 各 Bootcamp 模块投入时间占比
- 月度目标完成率
- 连续稳定天数峰值

### 2.5 高级功能模块

> v1.1 把这一整章删掉了，但它的 §5.2 又引用了「能量预算」——那个机制只在这里定义（§2.5.2）。
> 全章按 v1.0 保留。

#### 2.5.1 番茄钟集成（Pomodoro Timer）

**功能设计**

- 任务启动时可选择开启番茄钟（25 分钟工作 + 5 分钟休息）
- 自动记录完成的番茄数
- 统计每个任务/模块的总番茄数
- 番茄钟期间锁定界面，显示当前任务和倒计时
- 完成 4 个番茄后提示长休息（15–30 分钟）

**数据追踪**

- 每日番茄数趋势图
- 不同时段的番茄效率分布
- 高效时段识别（哪个时段完成番茄最多）

#### 2.5.2 能量预算系统（Energy Budget）

**核心理念：** 认知能量是有限资源，需要像金钱一样预算和分配。

**每日能量池**

- 满能量：100 点（早晨起床后）
- 高能量任务：消耗 30–50 点
- 中能量任务：消耗 15–25 点
- 低能量任务：消耗 5–10 点
- 例程恢复：晨间 +15 点，夜间 +10 点

**智能提醒**

- 当能量池 < 30 点时，提示"只做低能量任务"
- 当能量池 < 10 点时，提示"进入恢复模式"
- 每日能量消耗曲线可视化

**能量优化建议**

- "你倾向于在 9:00–11:00 完成高能量任务"
- "下午 3:00 后能量下降，建议安排低能量任务"

#### 2.5.3 目标树与反向规划（Goal Tree）

**功能说明：** 从长期目标反向分解到每日行动。

```
🎯 年度目标：成为AI工程师
  └─ 📅 Q1里程碑：完成Bootcamp全部模块
      └─ 📊 1月目标：完成M1-M3
          └─ 📝 本周任务：完成M2的3个实验
              └─ ✅ 今日行动：Feature Engineering Lab 1
```

**关键功能**

- 可视化目标树（树状图或思维导图）
- 点击任意节点查看进度
- 自动计算每层完成度（子节点完成度的加权平均）
- 目标对齐检查：当前任务是否支撑上层目标

#### 2.5.4 干扰追踪与反思系统（Distraction Tracker）

**追踪内容**

- 记录被打断的次数和原因（如消息通知、突发任务、自我分心）
- 记录拖延行为（如刷社交媒体、过度规划）
- 记录违反"反人性规则"的次数

**反思提示**

- 每日结束时：
  - "今天被打断了几次？主要原因是什么？"
  - "有哪些时间被浪费了？下次如何避免？"
- 每周总结时：
  - 生成"干扰热力图"（哪个时段最容易被打断）
  - 建议改进措施

#### 2.5.5 决策日志（Decision Log）

**用途：** 记录重要决策，避免重复思考和决策疲劳。

**记录内容**

- 决策内容（如"放弃 X 项目，聚焦 Y 方向"）
- 决策时间
- 决策理由
- 当时的上下文（能量状态、时间压力等）
- 3 个月后回顾：这个决策是否正确

**智能提醒**

- 当遇到类似情况时，自动显示历史决策
- "你在 2 个月前做过类似决策，要查看当时的思考吗？"

#### 2.5.6 数据洞察与 AI 建议（Insights）

**自动生成洞察**

- "你在周二和周四的完成率最高"
- "你倾向于高估任务耗时 20%"
- "当你完成晨间例程时，当日状态有 85% 概率为绿色"
- "你的 M4 模块投入时间不足，可能影响整体进度"

**预测与建议**

- 基于历史数据预测下周的稳定性风险
- 基于当前任务量，建议本周需要削减的 Q3/Q4 任务
- 基于能量消耗模式，建议最优时间块分配

#### 2.5.7 协作与分享（可选）

**导出功能**

- 导出周报（Markdown 格式）
- 导出月度总结（PDF 格式）
- 导出 Bootcamp 学习笔记（Notion/Obsidian 格式）

**匿名对比（社区功能，长期愿景）**

- 与同期 Bootcamp 学员的匿名数据对比
- "你的 M2 完成速度处于前 30%"
- "你的稳定性评分高于平均水平 15%"

---

## 3. 技术方案

> 本章以 v1.1 为准——v1.0 的 `window.storage` 是个不存在的 API。

### 3.1 技术栈

| 层 | 选型 |
|---|---|
| 前端框架 | React 18+（Function Components + Hooks） |
| 构建工具 | **Vite** |
| 状态管理 | **Zustand**（轻量级，适合中型应用）或 `useReducer` |
| 数据存储 | **`localStorage`**（主存储）+ JSON 文件导出（备份） |
| 备选存储 | IndexedDB（用于大量历史数据） |
| 样式库 | Tailwind CSS |
| 图表库 | Recharts |
| 图标库 | lucide-react |
| 日期处理 | **date-fns**（推荐）或原生 `Date` |

> **修正注**：v1.0 提及的 `window.storage` 不存在，已更正为 `localStorage`。

<details>
<summary><b>已废弃：v1.0 的原始技术栈（保留作为设计沿革）</b></summary>

- 前端框架：React（推荐使用函数组件 + Hooks）
- 状态管理：React `useState` / `useReducer`
- 数据存储：首选 `window.storage` API（**不存在的 API，v1.1 已修正为 `localStorage`**）；备选 IndexedDB
- 样式：Tailwind CSS（未变）
- 图表库：Recharts（未变）
- 日期处理：原生 JavaScript `Date` 对象
- 图标：lucide-react（未变）

</details>

### 3.2 数据模型（TypeScript Interfaces）

#### 3.2.1 时间表（TimeSlot）

> v1.1 删掉了这个接口，但它的 §2.1.1 仍要求对时间段做增删改查、§4 又说 `ptlt_settings`
> "包含时间表配置"却没定义结构。这里按 v1.0 补回，填上那个洞。

```ts
interface TimeSlot {
  id: string;
  timeRange: string;        // 如 "07:00 – 08:30"
  name: string;             // 如 "🟦 个人核心认知时段"
  content: string;          // 该时段的具体活动描述
  antiHumanRules: string;   // 该时段的约束条件
}
```

#### 3.2.2 任务（Task）—— 已修复缺失字段

```ts
type Quadrant = 'Q1' | 'Q2' | 'Q3' | 'Q4';
type EnergyLevel = 'low' | 'medium' | 'high';
type TaskStatus = 'todo' | 'in_progress' | 'completed' | 'archived';

interface Task {
  id: string;
  title: string;
  description?: string;
  quadrant: Quadrant;           // 核心字段：象限
  priority: 'P0' | 'P1' | 'P2' | 'P3';
  energyRequired: EnergyLevel;  // 核心字段：能量预算
  status: TaskStatus;
  estimatedTime: number;        // 分钟
  actualTime?: number;          // 分钟，用于分析
  deadline?: string;            // ISO String
  relatedModule?: string;       // 关联 Bootcamp 模块 ID
  createdAt: string;            // ISO String
  updatedAt: string;
}
```

> v1.0 的 `Task` 缺 `quadrant` 和 `energyRequired`（尽管 §2.3 整章依赖它们），
> 且用 `high | medium | low` 当优先级，和正文的 P0–P3 自相矛盾。以上为修正版。

<details>
<summary><b>已废弃：v1.0 的原始 Task / RoutineRecord 接口（保留作为设计沿革）</b></summary>

```ts
interface Task {
  id: string;
  title: string;
  priority: "high" | "medium" | "low";
  status: "not_started" | "in_progress" | "completed";
  deadline?: string;
  relatedModule?: string;
  notes?: string;
}

interface RoutineRecord {
  completed: boolean[];
  completionRate: number;
}
```

`RoutineRecord.completed` 用位置化的 `boolean[]` 记录例程勾选——v1.1 改成
`RoutineData.completedItems: string[]`，避免例程条目增删后整体错位。

</details>

#### 3.2.3 每日记录（DailyRecord）

```ts
interface RoutineData {
  completedItems: string[]; // 完成的项目ID列表
  completionRate: number;   // 0.0 - 1.0
}

interface DailyRecord {
  date: string; // YYYY-MM-DD (主键)

  // 例程
  morning: RoutineData;
  evening: RoutineData;

  // 学习记录
  bootcampModules: string[];
  contributionTypes: string[];
  notes: string;

  // 核心指标
  cognitiveEnergy: EnergyLevel;
  alignedWithGoals: boolean;
  coreTaskCompleted: boolean;

  // 计算结果 (持久化存储以免重复计算)
  stabilityScore: number;
  dailyStatus: 'green' | 'yellow' | 'gray';
}
```

> `RoutineData` 用 `completedItems: string[]` 取代了 v1.0 的位置化 `completed: boolean[]`——
> 后者在例程条目增删后会整体错位。

### 3.3 核心算法

#### 3.3.1 每日状态计算（加权评分制）

```js
/**
 * 计算每日稳定性评分和状态
 * @param {DailyRecord} record
 * @returns { {score: number, status: string} }
 */
function calculateDailyStatus(record) {
  let score = 0;

  // 1. 例程完成率 (权重 40分)
  // 晨间和夜间各占 20分
  score += (record.morning.completionRate || 0) * 20;
  score += (record.evening.completionRate || 0) * 20;

  // 2. 核心任务 (权重 30分)
  // 必须完成核心任务才能拿分，这是底线
  if (record.coreTaskCompleted) {
    score += 30;
  }

  // 3. 认知能量 (权重 15分)
  const energyScores = { high: 15, medium: 10, low: 0 };
  score += energyScores[record.cognitiveEnergy] || 0;

  // 4. 目标对齐 (权重 15分)
  if (record.alignedWithGoals) {
    score += 15;
  }

  // 状态判定
  let status = 'gray';
  if (score >= 80) status = 'green';
  else if (score >= 50) status = 'yellow';

  return { score, status };
}
```

#### 3.3.2 连续稳定天数计算

```js
function calculateStreak(records) {
  let streak = 0;
  const sortedDates = Object.keys(records).sort().reverse(); // 从最新日期开始

  for (const date of sortedDates) {
    // 只要不是灰色状态，都视为维持了系统运转
    if (records[date].dailyStatus !== 'gray') {
      streak++;
    } else {
      break;
    }
  }
  return streak;
}
```

#### 3.3.3 灰色预警检测

> v1.1 只保留了预警的 UI 文案，删掉了代码。这里按 v1.0 补回。

```js
function checkGrayAlert(records) {
  const recentDays = getLastNDays(records, 2);
  return recentDays.every(r => r.dailyStatus === "gray");
}
```

---

## 4. 数据存储结构设计

为了适应 `localStorage`，采用扁平化 Key-Value 设计。**`ptlt_` 前缀即项目代号的来源。**

| Key | 内容 | 结构 |
|---|---|---|
| `ptlt_settings` | 全局配置：时间表配置、例程条目定义 | `{ schedule: TimeSlot[], routines: {...} }` |
| `ptlt_records` | 每日记录 | `{ "2026-01-20": DailyRecord, ... }` |
| `ptlt_tasks` | 任务列表（所有任务的大数组，前端通过 filter 展示） | `Task[]` |

<details>
<summary><b>已废弃：v1.0 的分散键设计（保留作为设计沿革）</b></summary>

v1.0 每天一个 key，且基于不存在的 `window.storage`：

```js
// 例程数据
"routines:{YYYY-MM-DD}": {
  morning: { completed: boolean[], completionRate: number },
  daytime: { completed: boolean[], completionRate: number },
  evening: { completed: boolean[], completionRate: number }
}

// 每日记录
"daily:{YYYY-MM-DD}": {
  bootcampModules: string[],
  contributionType: string[],
  status: "not_started" | "in_progress" | "completed",
  notes: string,
  cognitiveEnergy: "low" | "medium" | "high",
  alignedWithGoals: boolean,
  coreTaskCompleted: boolean,
  dailyStatus: "green" | "yellow" | "gray"
}

// 任务列表
"tasks:daily": Task[]
"tasks:monthly": Task[]
"tasks:yearly": Task[]

// 时间表配置
"schedule:config": TimeSlot[]

// 统计数据（定期计算并缓存）
"stats:current-streak": number
"stats:monthly-summary:{YYYY-MM}": MonthlySummary
```

注意 v1.0 的 `routines` 里有 `daytime`，而 v1.1 的 `DailyRecord` 只有 `morning` / `evening`——
日间例程（§2.1.2）在新模型里没有落脚点。若要实现日间例程追踪，需给 `DailyRecord` 补一个
`daytime: RoutineData`。

</details>

---

## 5. 用户界面设计

### 5.1 页面结构（主导航）

| 页面 | 说明 |
|---|---|
| 仪表盘（Dashboard） | 默认首页 |
| 今日（Today） | 当日详细视图 |
| 时间表（Schedule） | 工作日时间规划 |
| 任务（Tasks） | 日/月/年任务管理 + 象限看板 |
| Bootcamp | 模块追踪 |
| 分析（Analytics） | 数据可视化 |
| 设置（Settings） | 数据导入/导出 |

> v1.1 只描述了 3 个页面（Dashboard / Today / Tasks），Schedule、Bootcamp、Analytics、Settings
> 四页随之消失。这里按 v1.0 保留 7 页导航。

### 5.2 关键页面设计

#### 5.2.1 仪表盘页面

**上半部分：关键指标卡片**

- 连续稳定天数（大字显示，带趋势图标）
- 本周绿色天数 / 7
- 本月完成率
- 当前状态指示器（绿/黄/灰圆点 + 文字，红绿灯样式）

**中间部分：稳定度折线图**（Recharts）

- X 轴：最近 30 天日期
- Y 轴：稳定性评分（0–100，随 [§2.4.2](#242-每日状态判定规则v11-加权评分制) 改为连续评分；
  v1.0 原为离散三档 绿=3 / 黄=2 / 灰=1，与"评分"实现不兼容，按 v1.1 更新）
- 高亮当前日期

**下半部分：快速操作**

- "记录今日" 按钮
- "查看本周总结" 按钮
- 预警通知（如果连续 2 天灰色）

#### 5.2.2 今日页面

分为 4 个区块：

1. **晨间例程**：7 项任务的勾选框（左侧）
2. **日间提醒**：显示当前时间段及反人性规则
3. **Bootcamp 记录**：模块多选 + 贡献类型 + 能量 + 笔记输入（中间）
4. **夜间例程**：4 项任务的勾选框（右侧）

底部：保存按钮 + 今日状态预览

#### 5.2.3 任务看板页面

- 艾森豪威尔矩阵视图（四个象限的 Grid 布局）
- 支持从 Q1/Q2 **拖拽**任务到"今日核心任务"区域
- 配合 [§2.3.5](#235-任务过滤与视图) 的快速过滤器与智能视图

#### 5.2.4 Bootcamp 页面

模块卡片网格：

- 每个模块一张卡片
- 显示：模块名称、核心内容、本周/本月投入天数
- 点击展开：查看该模块的所有历史记录

#### 5.2.5 分析页面

可视化图表：

- 稳定度趋势图（折线图，最近 3 个月）
- 认知能量分布（饼图）
- Bootcamp 模块时间占比（堆叠柱状图）
- 周度/月度对比（柱状图）

### 5.3 交互细节

- **能量预算提醒**：当用户在"低能量"状态下试图添加 Q1 高难度任务时，弹出 Toast 建议：
  "当前能量较低，建议安排 Q3/Q4 任务"（机制定义见 [§2.5.2](#252-能量预算系统energy-budget)）
- **灰色预警**：连续 2 天状态为 Gray 时，Dashboard 顶部显示显眼横幅：
  "系统由于连续低能已降级，请启用恢复模式。"

---

## 6. 非功能需求

### 6.1 性能要求

- 页面加载时间 **< 1 秒**（v1.1 收紧，v1.0 为 < 2 秒）
- 数据读写响应时间 < 500ms
- 支持存储至少 1 年的历史数据（~365 条记录）

### 6.2 兼容性

- **主要目标**：Chrome 90+、Edge 90+（Desktop 优化）
- **v1.0 更宽的目标**（若要支持则一并测试）：Firefox 88+、Safari 14+
- 响应式设计（支持桌面端和平板端，手机端可选）

### 6.3 数据安全

- 所有数据仅存储在用户本地浏览器
- 提供数据**导出**功能（JSON 格式）——必须，便于备份或做数据分析
- 提供数据**导入**功能（支持恢复备份）
- 数据**清除**功能（设置页面）

> v1.1 只保留了导出，却在测试计划里要求"导入恢复"。导入/清除按 v1.0 保留。

### 6.4 用户体验

- 明确的视觉反馈（加载状态、保存成功提示）
- 友好的错误提示
- 键盘快捷键支持（可选）
- 暗色模式支持（可选）

---

## 7. 实现建议

### 7.1 开发阶段划分

**Phase 1：核心功能（MVP）**

- 基础数据结构设计
- 今日页面（例程记录 + Bootcamp 记录）
- 简单的每日状态计算
- 基本的本地存储

**Phase 2：计划管理**

- 任务管理模块（日/月/年）
- 时间表配置页面

**Phase 3：数据分析**

- 仪表盘页面
- 稳定度折线图
- 连续天数计算
- 灰色预警功能

**Phase 4：高级功能**

- 周/月总结自动生成
- 数据导出/导入
- Bootcamp 详细分析页面
- 响应式优化

### 7.2 关键技术难点

**难点 1：状态管理复杂度**

- 建议使用 `useReducer` 或 Zustand 管理复杂状态
- 将状态逻辑与 UI 组件分离

**难点 2：持久化存储可靠性**

- `localStorage` 读写要做错误处理（配额超限、隐私模式下不可用）
- 实现自动保存机制（防止数据丢失）
- 定期提醒用户导出备份

**难点 3：日期与跨天处理**

- 时区处理统一使用本地时区
- 日期统一用 ISO 8601 的 `YYYY-MM-DD` 字符串作为 Key，避免时间戳的时区计算麻烦
- 每天首次打开 App 时，检查是否存在今日 Key，若无则初始化（重置例程勾选）
- 注意跨年/跨月的边界情况

**难点 4：数据分析性能**

- 随着记录增多，计算"月度趋势"可能变慢
- 建议在 `DailyRecord` 保存时直接计算并存储 `stabilityScore`，读取时直接使用，无需重新计算

### 7.3 代码组织建议

```
src/
├── components/
│   ├── Dashboard/
│   ├── Today/
│   ├── Schedule/
│   ├── Tasks/
│   ├── Bootcamp/
│   ├── Analytics/
│   └── Settings/
├── hooks/
│   ├── useStorage.js       # 封装storage API
│   ├── useDailyRecord.js   # 每日记录逻辑
│   └── useStats.js         # 统计计算逻辑
├── utils/
│   ├── dateUtils.js        # 日期处理
│   ├── statusCalculator.js # 状态计算
│   └── dataExport.js       # 导入导出
├── constants/
│   ├── modules.js          # Bootcamp模块定义
│   └── routines.js         # 例程配置
└── types/
    └── index.ts            # TypeScript类型定义
```

---

## 8. 测试计划

### 8.1 单元测试

- 日期计算函数
- 状态计算逻辑（加权评分）
- 连续天数统计算法

### 8.2 集成测试

- 数据存储和读取
- 跨组件状态同步
- 导入导出功能

### 8.3 用户测试场景

- 新用户首次使用流程
- 连续 7 天记录流程
- 数据备份和恢复流程
- 灰色预警触发流程

### 8.4 具体验收用例（v1.1）

1. **逻辑测试**：模拟输入早晚例程全空、能量低的情况，验证状态是否正确判定为 Gray。
2. **持久化测试**：关闭浏览器再打开，确认今日记录未丢失。
3. **导出测试**：导出 JSON 文件并尝试在一个新浏览器中"导入恢复"，验证数据完整性。

---

## 9. 未来扩展方向

### 9.1 短期优化

- 添加数据分析洞察（如"本月认知能量下降 20%"）
- 自定义例程内容
- 任务优先级自动排序

### 9.2 长期愿景

- 多设备同步（需要后端支持）
- AI 驱动的个性化建议
- 社区分享功能（匿名数据对比）
- 移动端原生应用

---

## 10. 附录

### 附录 A：术语表

| 术语 | 含义 |
|---|---|
| **认知能量** | 用户自我评估的当日深度思考能力 |
| **反人性规则** | 设计用于对抗拖延/分心的行为约束 |
| **稳定性评分** | 基于例程完成度和任务完成情况的综合评分（0–100） |
| **降载** | 降低系统复杂度，减少非必要任务 |

### 附录 B：增强前的原始任务模型

> 这是最早那份 `personal_tracker_doc.md` 里的 §2.3，后来被 v1.0 的
> [§2.3 增强版](#23-任务与计划管理增强版)整章取代。保留它是因为这是唯一记录了
> "增强之前长什么样"的地方——如果哪天觉得增强版过重，这就是那个可以退回的最小版本。

**2.3.1 多维度计划**

- **每日任务**：Todolist，支持添加/删除/标记完成
- **每月计划**：月度目标列表
- **每年计划**：年度目标列表

**2.3.2 任务字段**

- 任务名称
- 优先级（高/中/低）
- 状态（未开始/进行中/已完成）
- 截止日期
- 关联模块（可选，关联到 Bootcamp 模块）
- 备注

### 附录 C：参考资源

- React 官方文档：https://react.dev
- Recharts 文档：https://recharts.org
- Tailwind CSS：https://tailwindcss.com
- Vite：https://vite.dev
- date-fns：https://date-fns.org
- 浏览器存储 API：MDN Web Docs — [Window.localStorage](https://developer.mozilla.org/docs/Web/API/Window/localStorage)

---

## 原始文档版本记录

| | v1.0（`v1.docx` / `personal_tracker_doc.md`） | v1.1（`v2.docx`） |
|---|---|---|
| 版本号 | v1.0 | v1.1 修正版 |
| 最后更新 | 2026-01-20 | 2026-01-20 |
| 作者 | 需求分析团队 | 需求分析团队 |
| 状态 | 待开发团队评审 | 已准备开发 |
