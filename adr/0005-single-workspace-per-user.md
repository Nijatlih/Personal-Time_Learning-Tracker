# ADR 0005 — 单用户单 workspace，删除 workspace 创建端点

- **日期**：2026-08-12
- **状态**：已接受
- **相关**：[ADR 0001（自用工具，不做成产品）](0001-personal-tool-not-a-product.md)、
  [ADR 0003（删除 invite 与 RBAC）](0003-drop-invite-and-role-based-access.md)、
  [`AUDIT_2026_08_10.md`](../docs/AUDIT_2026_08_10.md)（F1/F4）

## 背景

`POST /api/v1/workspaces` 能创建 workspace，但创建出来的东西**此后永远访问不到**。

原因在认证层：登录和刷新时写进 token 的 `workspaceId` 只来自
`findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc` —— 字面意思是"最早创建的那个"。
而 44 个端点里没有任何一个接受显式的 workspaceId，也没有任何端点能切换当前 workspace。
于是第二个及以后创建的 workspace 一建出来就是死的。

`WorkspaceService.list()` 让这件事更不自洽：它返回用户名下的**全部** workspace，
所以界面可以列出它们、可以拿到它们的 id，但所有数据端点都只认 token 里那一个。
API 主动展示了一批点不进去的东西。

这是第二轮审计 F1/F4 未修的那一半。当时按"单用户单 workspace"记录为已接受的限制，
但没有动代码 —— 结果限制只存在于文档里，API 表面仍然在承诺一个它做不到的能力。

## 决定

**删掉 create 端点，并把 `list()` 收敛为只返回 token 指向的那一个 workspace。**

workspace 只在注册时创建一个（`AuthService.register` 里直接建，不经过
`WorkspaceService`），此后不再有第二个。

理由与 ADR 0003（删 invite/RBAC）、ADR 0004（删 sync）一致：**不做的功能整块删掉，
不留半个。** 一个能创建不可访问资源的端点，比没有这个端点糟得多 ——
它看起来能用，实际制造垃圾数据，而且要到用户困惑"为什么我建的 workspace 打不开"
才会暴露。

`list()` 返回类型保持 `List` 而不是改成单个对象，是为了不改动 API 形状：
客户端拿到的仍是数组，只是长度恒为 1。前端目前完全没有调用 workspace 端点，
所以这个选择只影响将来。

## 删除面

- `POST /api/v1/workspaces` 端点
- `WorkspaceService.create()`
- `workspace/dto/WorkspaceCreateRequest.java`
- `WorkspaceService.list()` 的签名从 `(userId)` 变成 `(workspaceId, userId)`，
  并加上成员身份校验

端点数 44 → 43。保留 `GET`（列表与详情）、`PUT`、`DELETE`：
改名和删除自己那一个 workspace 都是合理操作。

## 存量数据

如果某个库里已经通过这个端点创建过多余的 workspace，它们仍然留在表里，
只是不再出现在 `list()` 的结果里，也从来无法被访问。**没有为它们写清理迁移**：
自用工具、单用户，多余的行是无害的死数据；写一个删业务数据的迁移风险高于收益。

## 将来如果真的需要多 workspace

要做的不是把这个端点加回来，而是先解决"如何切换"：登录/刷新接受可选的 workspaceId、
校验成员身份后写进 token，前端加一个选择器。**切换能力必须先于创建能力存在**，
否则又会回到今天这个状态。
