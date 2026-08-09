import axios from 'axios';

// 建立一个更稳健的 API 实例
const client = axios.create({
  baseURL: '/api/v1',
  timeout: 10000, // 10秒超时
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：注入 Token
client.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // 调试日志：确保我们知道请求发往了哪里
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：系统性处理错误
client.interceptors.response.use(
  (response) => {
    console.log(`[API Success] ${response.config.url}`, response.data);
    return response.data.data; // 直接返回业务数据
  },
  (error) => {
    const originalRequest = error.config;
    
    // 处理网络错误（如后端没开）
    if (!error.response) {
      console.error('[API Network Error] Backend might be down:', error.message);
      return Promise.reject({ message: '无法连接到服务器，请检查后端是否启动。' });
    }

    // 处理 401 认证失效
    if (error.response.status === 401 && !originalRequest._retry) {
      console.warn('[API Auth Error] Token expired or invalid.');
      localStorage.removeItem('accessToken');
      window.location.href = '/login';
    }

    // 处理其他业务错误
    const errorMessage = error.response.data?.error?.message || '未知系统错误';
    console.error(`[API Business Error] ${originalRequest.url}:`, errorMessage);
    return Promise.reject({ message: errorMessage, status: error.response.status });
  }
);

export default client;
