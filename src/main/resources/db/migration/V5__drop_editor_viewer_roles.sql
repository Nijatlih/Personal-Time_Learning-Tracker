-- 把 workspace_members.role 收敛到只剩 OWNER。
--
-- 背景见 adr/0003-drop-invite-and-role-based-access.md：invite 流程与
-- EDITOR/VIEWER 角色在本次提交中一并删除，PTLT 是单人自用工具（adr/0001），
-- 不做多人协作。
--
-- 为什么这是安全的：POST /workspaces/{id}/invite 是**唯一**会写入
-- EDITOR/VIEWER 的路径（其他两处写 role 的地方 —— AuthService.register 和
-- WorkspaceService.create —— 都写死 OWNER）。而按 F1/F4，被邀请者根本登不进来：
-- 登录时 token 里的 workspaceId 只取自 findFirstByOwnerId，不拥有任何 workspace
-- 的用户会直接被抛 401 "No workspace found for user"。所以这个功能从未真正可用。
--
-- 即便如此，下面仍先做一次防御性 UPDATE 再收紧约束：万一某个库里真有这样的行，
-- 收紧 CHECK 会让迁移直接失败、应用起不来，那比默默把它们改成 OWNER 更糟
-- （这是自用工具，唯一的真实用户就是 workspace 的 owner 本人）。
UPDATE workspace_members SET role = 'OWNER' WHERE role <> 'OWNER';

ALTER TABLE workspace_members DROP CONSTRAINT workspace_members_role_check;

ALTER TABLE workspace_members ADD CONSTRAINT workspace_members_role_check
    CHECK (role IN ('OWNER'));
