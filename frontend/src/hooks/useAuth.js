import { createContext, useContext } from 'react';

/**
 * 认证上下文与它的 hook。
 *
 * 刻意与 AuthProvider 分成两个文件：react-refresh/only-export-components 要求一个模块
 * 不要同时导出组件和非组件。之前 useAuth.jsx 同时导出 AuthProvider（组件）和
 * useAuth（hook），因此被标记 —— 而这不只是 lint 洁癖：混着导出会让该模块无法参与
 * Fast Refresh，改动它会整页重载并丢掉状态。
 *
 * 本文件只有非组件导出，所以是 .js 而不是 .jsx。
 */
export const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);
