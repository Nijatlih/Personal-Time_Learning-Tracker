# ADR 0003 — 删除 invite 流程与基于角色的访问控制

- **日期**：2026-08-12
- **状态**：已接受
- **相关**：[ADR 0001（自用工具，不做成产品）](0001-personal-tool-not-a-product.md)、
  [`docs/AUDIT_2026_08_10.md`](../docs/AUDIT_2026_08_10.md)（F1/F4）

## 背景

代码里有一整套多人协作的骨架：`WorkspaceRole` 三个值（OWNER/EDITOR/VIEWER）、
`POST /api/v1/workspaces/{id}/invite`、`WorkspaceService.invite()`、
以及散在 `update()`/`delete()` 里的角色分支。

第二轮审计（F1/F4）确认了一件事：**这套东西从来就不可用。**

登录和刷新时，token 里的 `workspaceId` 只来自
`findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc` —— 按 **ownerId** 查，
而不是按成员身份。10 个数据 controller 全部只从 token 取 workspaceId，没有任何端点
接受显式的 workspaceId。所以一个被 invite 进来的 EDITOR/VIEWER，因为不拥有任何
workspace，登录时会直接撞上 `UnauthorizedException("No workspace found for user")`。
**被邀请者登不进来，invite 写进去的成员行永远没有主体去使用它。**

同时这套死代码还带着两个真实缺陷：

1. `TaskService.validateWorkspaceAccess` 等只校验"是不是成员"，不校验角色 ——
   VIEWER 在所有数据写路径上实际拥有完整写权限（首轮审计已记录）。
2. 提权路径：invite 此前只拦截 VIEWER，然后原样采用 `request.getRole()`，
   于是 EDITOR 可以邀请一个自己控制的账号为 OWNER，用它登录即满足 delete 的
   OWNER 校验 —— 两个 HTTP 调用就能从 EDITOR 提权到删除整个 workspace。
   （这一条在首轮已被修补，但修补的是一段本就无人可达的代码。）

## 决定

**删掉整套 invite 与 RBAC，而不是去补齐它。**

按 ADR 0001，PTLT 是自用工具，明确不做多人协作。为一个不会有第二个用户的系统
维护三级权限模型，代价是持续的正确性负担（上面两条就是明证），收益为零。

具体删除面：

- `POST /api/v1/workspaces/{id}/invite` 端点
- `WorkspaceService.invite()`
- `workspace/dto/WorkspaceInviteRequest.java`
- `WorkspaceMemberRepository.findRoleByWorkspaceIdAndUserId`（本就没有调用点）
- `WorkspaceService.update()` 的 VIEWER 分支、`delete()` 的 OWNER 分支 ——
  成员只可能是 OWNER，"是成员"与"有权限"从此是同一件事
- `WorkspaceRole` 收敛为只剩 `OWNER`

## schema 后果

`workspace_members.role` 的 CHECK 约束需要同步收紧，否则实体枚举与数据库允许的
取值不再一致：`V5__drop_editor_viewer_roles.sql`。

`role` 列本身**保留**（不删列）：它是 `NOT NULL`，删列要同时动 schema 和实体，
而留着一个恒为 `'OWNER'` 的列几乎没有成本。

迁移里先跑一次防御性的 `UPDATE ... SET role='OWNER' WHERE role <> 'OWNER'` 再收紧
约束。按上面的论证不可能存在这样的行，但万一存在，让迁移直接失败、应用起不来，
比默默改成 OWNER 更糟 —— 这是自用工具，唯一的真实用户就是 owner 本人。

## 未修复的另一半

F1/F4 的另一半是：**你自己创建的第二个 workspace 永远访问不到**，
因为 token 里的 workspaceId 固定取"最早创建的那个"，而没有任何端点能切换。

**这一半不修**，作为已接受的限制记录在案：PTLT 按**单用户、单 workspace**运行。
`POST /api/v1/workspaces` 是否还应该存在（它能创建一个此后永远无法访问的 workspace），
留给下一轮决定。

## 代价

如果将来真的要做协作，这部分要重写。考虑到骨架从未运行过一次、
且重写时的需求一定与今天这套猜测不同，这个代价是可以接受的。
