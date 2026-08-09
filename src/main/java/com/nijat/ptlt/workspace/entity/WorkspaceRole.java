package com.nijat.ptlt.workspace.entity;

/**
 * Workspace成员角色
 */
public enum WorkspaceRole {
    OWNER,   // 所有者：全部权限
    EDITOR,  // 编辑者：读写权限
    VIEWER   // 查看者：只读权限
}
