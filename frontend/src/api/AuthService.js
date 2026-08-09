import client from './client';

const AuthService = {
  login: async (email, password) => {
    const data = await client.post('/auth/login', { email, password });
    return data;
  },
  
  register: async (email, password, workspaceName) => {
    const data = await client.post('/auth/register', { email, password, workspaceName });
    return data;
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
  }
};

export default AuthService;
