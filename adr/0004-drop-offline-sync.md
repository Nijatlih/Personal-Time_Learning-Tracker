# ADR 0004 — 删除离线同步（sync）

- **日期**：2026-08-12
- **状态**：已接受
- **相关**：[ADR 0001（自用工具，不做成产品）](0001-personal-tool-not-a-product.md)、
  [ADR 0003（删除 invite 与 RBAC）](0003-drop-invite-and-role-based-access.md)

## 背景

`sync` 包提供两个端点，意图是支撑离线优先的客户端：

- `POST /api/v1/sync/pull` —— 返回 `updatedAt > since` 的实体
- `POST /api/v1/sync/push` —— 接收客户端的离线变更

两者都没有真正完成：

**`push` 从未写入过任何数据。** 它此前忽略整个 request，直接返回 `pull()` 的结果并包在
`ApiResponse.success()` 里 —— 离线客户端会收到 200 和一个新游标，据此认为队列已被接受
并清空发件箱，而服务端什么都没写。那是**伪装成成功的数据丢失**。首轮审计把它改成抛
501，让失败至少变得可见，但功能本身仍然不存在。

**`pull` 只覆盖 14 张表中的 2 张**（`workspaces`、`workspace_members`）。也就是说它同步的
恰好是这个应用里唯一不产生日常数据的两张表 —— `daily_records`、`tasks`、`task_instances`、
`routine_completions`、`time_blocks` 等全部不在其中。一个只能同步 workspace 元数据的
"离线同步"，对离线使用毫无帮助。

而且 `pull` 的实现方式不具备扩展性：两个实体各有一个手写的 `xxxToMap` 方法，
把字段一个一个塞进 `Map<String, Object>`。照这个模式补齐剩下 12 张表，意味着 12 个
手写映射器，且每次实体加字段都要记得同步改动 —— 没有任何机制保证这件事。

## 决定

**整块删除，不补齐。**

理由有三层：

1. **需求本身不存在。** 按 ADR 0001，PTLT 不做移动端 App。离线同步存在的唯一理由是
   支撑一个不联网的客户端，而唯一的客户端是浏览器里的 React 应用，它需要网络才能加载。
2. **正确的离线同步是一个独立的工程项目**，不是一个包。它需要冲突解决策略、
   墓碑与删除传播、幂等的客户端变更 ID、单调游标（当前实现用
   `OffsetDateTime.now()` 当游标，在时钟回拨或并发写入时会漏数据）。
   在没人需要它的时候把这些做对，是纯粹的浪费。
3. **留着有真实成本。** 它是 46 个端点里的 2 个，出现在 API 表面上，任何读这个仓库的人
   都会以为离线同步是一项功能。首轮审计已经在它上面花过一次时间（发现静默丢数据），
   第二轮又花了一次（确认 pull 覆盖面）。

## 删除面

- `sync/controller/SyncController.java`
- `sync/service/SyncService.java`
- `sync/dto/SyncPullRequest.java`、`SyncPushRequest.java`、`SyncResponse.java`
- `common/exception/NotImplementedException.java` 及 `GlobalExceptionHandler` 里对应的
  501 处理器 —— `SyncService.push` 是它唯一的使用者，删掉 sync 之后它就是死代码

零外部引用：前端从来没有调用过这两个端点（`frontend/src` 里对 `sync` 的匹配全部是
`async` 关键字和无关的日志文本）。端点数从 46 降到 44。

## 如果将来真的需要

从头设计，并且先确定客户端形态。**不要**从 git 历史里恢复这个包 ——
它的映射方式（手写 `entityToMap`）和游标语义（`OffsetDateTime.now()`）都不该被沿用。
