import React, { createContext, useContext, useState, useEffect } from 'react';

export interface User {
  userId: string;
  username?: string;
  name?: string;      // fallback alias
  email?: string;
  bio?: string;
  profileImageUrl?: string;
  followerCount?: number;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (provider: 'google' | 'github') => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
  login: () => {},
  logout: () => {},
});

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Attempt to fetch current authenticated user from session
    const fetchUser = async () => {
      try {
        const res = await fetch('/api/user/me', { credentials: 'include' });
        if (res.ok) {
          const userData = await res.json();
          setUser(userData);
        } else {
          setUser(null);
        }
      } catch (error) {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    fetchUser();
  }, []);

  const login = (provider: 'google' | 'github') => {
    // Redirect browser to backend OAuth initiation
    window.location.href = `/oauth2/authorization/${provider}`;
  };

  const logout = async () => {
    await fetch('/api/user/logout', { method: 'GET', credentials: 'include' });
    setUser(null);
    window.location.href = '/login';
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
