import { createContext, useContext, useState, useEffect } from 'react';
import AuthService, { AUTH_KEYS } from '../api/AuthService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem(AUTH_KEYS.accessToken));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 初始化检查
    const storedUser = localStorage.getItem(AUTH_KEYS.user);
    if (token && storedUser) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, [token]);

  const login = (userData, accessToken, refreshToken) => {
    localStorage.setItem(AUTH_KEYS.accessToken, accessToken);
    localStorage.setItem(AUTH_KEYS.user, JSON.stringify(userData));
    // 存下 refresh token —— 此前从不保存，所以 access token 一到 15 分钟过期
    // 就只能重新登录，client.js 里的刷新流程也无从谈起。
    if (refreshToken) {
      localStorage.setItem(AUTH_KEYS.refreshToken, refreshToken);
    }
    setToken(accessToken);
    setUser(userData);
  };

  const logout = async () => {
    // 真的去撤销后端的 refresh token，而不只是清本地。AuthService.logout
    // 内部保证无论后端成败都会清干净 localStorage。
    await AuthService.logout();
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
