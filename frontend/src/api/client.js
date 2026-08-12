import axios from 'axios';

// 建立一个更稳健的 API 实例
const client = axios.create({
  baseURL: '/api/v1',
  timeout: 10000, // 10秒超时
  headers: {
    'Content-Type': 'application/json',
  },
});

// 认证相关的 localStorage key 集中在这里，避免各处散着写字符串。
export const AUTH_KEYS = {
  accessToken: 'accessToken',
  refreshToken: 'refreshToken',
  user: 'user',
};

export const clearAuthStorage = () => {
  Object.values(AUTH_KEYS).forEach((k) => localStorage.removeItem(k));
};

// 请求拦截器：注入 Token
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(AUTH_KEYS.accessToken);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // 只打方法和路径。此前响应拦截器里还打了整个 response.data，
    // 而登录/刷新的响应体里就是 access token 和 refresh token 本身 ——
    // 等于把凭据写进浏览器控制台，任何能看到这台机器屏幕或装了扩展的人都能读到。
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => Promise.reject(error)
);

const redirectToLogin = () => {
  clearAuthStorage();
  window.location.href = '/login';
};

// 用 access token 过期时刷新一次。
//
// 同一时刻可能有多个请求一起收到 401，用一个共享的 promise 保证只发一次
// POST /auth/refresh，其余请求等这一次的结果。
let refreshPromise = null;

const refreshAccessToken = () => {
  if (!refreshPromise) {
    const refreshToken = localStorage.getItem(AUTH_KEYS.refreshToken);
    if (!refreshToken) {
      return Promise.reject(new Error('no refresh token'));
    }
    // 用裸 axios 而不是 client：避免被本文件的拦截器再套一层 401 处理递归下去。
    refreshPromise = axios
      .post('/api/v1/auth/refresh', { refreshToken }, { baseURL: '' })
      .then((res) => {
        const data = res.data?.data;
        if (!data?.accessToken) {
          throw new Error('refresh response missing accessToken');
        }
        localStorage.setItem(AUTH_KEYS.accessToken, data.accessToken);
        if (data.refreshToken) {
          // 后端会轮换 refresh token 并把旧的置为 revoked，必须存下新的
          localStorage.setItem(AUTH_KEYS.refreshToken, data.refreshToken);
        }
        return data.accessToken;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

// 响应拦截器：系统性处理错误
client.interceptors.response.use(
  (response) => response.data.data, // 直接返回业务数据
  async (error) => {
    const originalRequest = error.config;

    // 处理网络错误（如后端没开）
    if (!error.response) {
      console.error('[API Network Error] Backend might be down:', error.message);
      return Promise.reject({ message: '无法连接到服务器，请检查后端是否启动。' });
    }

    // 处理 401 认证失效：先试着刷新一次，失败才踢回登录页。
    //
    // 此前这里留了 _retry 标记却从未实现刷新，也从不设置这个标记 —— 结果是
    // access token 一到 15 分钟就把用户踢回登录页，refresh token 完全没用上。
    // （在后端把未认证从 403 改成 401 之前，这段代码其实连触发都触发不了。）
    if (error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const accessToken = await refreshAccessToken();
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return client(originalRequest);
      } catch {
        console.warn('[API Auth Error] Refresh failed, signing out.');
        redirectToLogin();
        return Promise.reject({ message: '登录已过期，请重新登录。', status: 401 });
      }
    }

    // 刷新之后仍然 401（或刷新接口自己 401）：只能重新登录
    if (error.response.status === 401) {
      redirectToLogin();
      return Promise.reject({ message: '登录已过期，请重新登录。', status: 401 });
    }

    // 处理其他业务错误
    const errorMessage = error.response.data?.error?.message || '未知系统错误';
    console.error(`[API Business Error] ${originalRequest.url}:`, errorMessage);
    return Promise.reject({ message: errorMessage, status: error.response.status });
  }
);

export default client;
