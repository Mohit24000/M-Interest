import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Home from './pages/Home';
import Login from './pages/Login';
import Profile from './pages/Profile';
import PinDetail from './pages/PinDetail';
import Search from './pages/Search';
import Settings from './pages/Settings';
import { AuthProvider, useAuth } from './context/AuthContext';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" />;
  return children;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        {/* Global Branding Overlay */}
        <div className="global-branding-logo animate-fade-in">
          ॥ जनक ॥
        </div>

        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/*" element={
            <ProtectedRoute>
              <div style={{ display: 'flex', minHeight: '100vh' }}>
                <Sidebar />
                <main style={{ marginLeft: '72px', flex: 1, minHeight: '100vh' }}>
                  <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/search" element={<Search />} />
                    <Route path="/profile" element={<Profile />} />
                    <Route path="/profile/:userId" element={<Profile />} />
                    <Route path="/settings" element={<Settings />} />
                    <Route path="/pin/:id" element={<PinDetail />} />
                  </Routes>
                </main>
              </div>
            </ProtectedRoute>
          } />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
