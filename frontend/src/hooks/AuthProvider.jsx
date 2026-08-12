import { useMemo, useState } from 'react';
import AuthService, { AUTH_KEYS } from '../api/AuthService';
import { AuthContext } from './useAuth';

/**
 * 从 localStorage 读回上次的登录状态。
 *
 * JSON.parse 加了保护：之前是裸调用，localStorage 里的 user 被写坏（手工改过、
 * 或者被别的脚本覆盖）就会在渲染期间抛异常，把整个 Provider 连带整个应用带下去，
 * 而且用户没有任何办法自己恢复 —— 页面根本渲染不出登出按钮。
 * 现在读坏了就当没登录，并把坏数据清掉。
 */
const readStoredUser = () => {
  const raw = localStorage.getItem(AUTH_KEYS.user);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    console.warn('[Auth] Stored user is not valid JSON; clearing it.');
    localStorage.removeItem(AUTH_KEYS.user);
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  // 用惰性初始值直接读 localStorage，而不是在 useEffect 里读完再 setState。
  //
  // 后者是 react-hooks/set-state-in-effect 标记的那个形状，而且它本身也说不通：
  // localStorage 是同步的，没有任何要等的东西。此前那个 effect 还依赖 [token]，
  // 于是每次 token 变化都会重新解析一次 user，纯属多余。
  const [user, setUser] = useState(readStoredUser);
  const [token, setToken] = useState(() => localStorage.getItem(AUTH_KEYS.accessToken));

  // 没有 loading 字段：初始化是同步的，没有需要等待的阶段。
  // 此前那个 loading 只是"在 effect 里读 localStorage"这个实现方式的副产物，
  // 一个恒为 false 的字段会让 App.jsx 里那个分支永远不执行 —— 留着就是死代码。
  const value = useMemo(() => ({
    user,
    token,

    login: (userData, accessToken, refreshToken) => {
      localStorage.setItem(AUTH_KEYS.accessToken, accessToken);
      localStorage.setItem(AUTH_KEYS.user, JSON.stringify(userData));
      // 存下 refresh token —— 此前从不保存，所以 access token 一到 15 分钟过期
      // 就只能重新登录，client.js 里的刷新流程也无从谈起。
      if (refreshToken) {
        localStorage.setItem(AUTH_KEYS.refreshToken, refreshToken);
      }
      setToken(accessToken);
      setUser(userData);
    },

    logout: async () => {
      // 真的去撤销后端的 refresh token，而不只是清本地。AuthService.logout
      // 内部保证无论后端成败都会清干净 localStorage。
      await AuthService.logout();
      setToken(null);
      setUser(null);
    },
  }), [user, token]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthProvider;
