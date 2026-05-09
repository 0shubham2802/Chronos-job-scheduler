import { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    // Restore session from localStorage on page load
    const token = localStorage.getItem('chronos_token');
    const user = localStorage.getItem('chronos_user');
    return token && user ? { token, user: JSON.parse(user) } : null;
  });

  const login = useCallback((token, name, email) => {
    const user = { name, email };
    localStorage.setItem('chronos_token', token);
    localStorage.setItem('chronos_user', JSON.stringify(user));
    setAuth({ token, user });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('chronos_token');
    localStorage.removeItem('chronos_user');
    setAuth(null);
  }, []);

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
