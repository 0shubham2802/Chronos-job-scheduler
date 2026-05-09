import api from './axios';

export const loginUser = async (email, password) => {
  const res = await api.post('/api/auth/login', { email, password });
  return res.data; // { token, tokenType, name, email }
};

export const registerUser = async (name, email, password) => {
  const res = await api.post('/api/auth/register', { name, email, password });
  return res.data; // { token, tokenType, name, email }
};

export const refreshToken = async () => {
  const token = localStorage.getItem('chronos_token');
  const res = await api.post('/api/auth/refresh', null, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return res.data;
};
