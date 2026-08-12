package com.nijat.ptlt.workspace.entity;

/**
 * Workspace成员角色。
 *
 * 只剩 OWNER。EDITOR / VIEWER 随 invite 流程一起删除了 —— 详见
 * adr/0003-drop-invite-and-role-based-access.md：PTLT 是自用工具（adr/0001），
 * 不做多人协作，而这两个角色除了 invite 之外没有任何写入路径。
 *
 * 枚举本身保留（而不是把 role 列一起删掉），是因为 workspace_members.role 是
 * NOT NULL 列，删列要动 schema 和实体，收益不足以抵消风险。
 */
public enum WorkspaceRole {
    OWNER   // 所有者：全部权限
}
