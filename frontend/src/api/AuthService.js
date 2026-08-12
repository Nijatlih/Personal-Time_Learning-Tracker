import client, { AUTH_KEYS, clearAuthStorage } from './client';

const AuthService = {
  login: async (email, password) => {
    const data = await client.post('/auth/login', { email, password });
    return data;
  },

  register: async (email, password, workspaceName) => {
    const data = await client.post('/auth/register', { email, password, workspaceName });
    return data;
  },

  /**
   * 登出。
   *
   * 此前这里只清 localStorage，从不调用后端 —— 结果是 refresh token 在 DB 里
   * 一直有效到 7 天后自然过期，谁拿到它都还能换出新的 access token。
   * 后端的撤销逻辑本身是好的（RefreshTokenRepository 的 UPDATE 带 @Modifying），
   * 只是从来没人调它。
   *
   * 后端失败不阻塞本地清理：用户点了登出，本地凭据无论如何都要清掉。
   */
  logout: async () => {
    try {
      await client.post('/auth/logout');
    } catch (err) {
      console.warn('[Auth] Server-side logout failed, clearing local session anyway:', err.message);
    } finally {
      clearAuthStorage();
    }
  },
};

export { AUTH_KEYS };
export default AuthService;
