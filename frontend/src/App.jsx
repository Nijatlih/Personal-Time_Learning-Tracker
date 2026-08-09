import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import Register from './pages/Register';
import Tasks from './pages/Tasks';
import Routines from './pages/Routines';
import Analytics from './pages/Analytics';
import './styles/globals.css';

// 认证守卫组件
const RequireAuth = ({ children }) => {
  const { token, loading } = useAuth();
  if (loading) return <div className="loading-screen">Loading System...</div>;
  if (!token) return <Navigate to="/login" replace />;
  return children;
};

// 系统布局组件
const SystemLayout = ({ children }) => {
  return (
    <div className="app-layout" style={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar />
      <main className="main-content" style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        <header className="system-header" style={{ 
          padding: '1.5rem 3rem', 
          borderBottom: '1px solid var(--border-color)',
          backgroundColor: 'rgba(2, 6, 23, 0.5)',
          backdropFilter: 'blur(10px)',
          position: 'sticky',
          top: 0,
          zIndex: 10
        }}>
          {/* 这里可以放置动态标题或搜索框 */}
          <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            System Status: <span style={{ color: 'var(--success)', fontWeight: '600' }}>ONLINE</span>
          </div>
        </header>
        <div style={{ flex: 1, padding: '2rem 3rem' }}>
          {children}
        </div>
      </main>
    </div>
  );
};

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* 受保护的私有路由 */}
          <Route path="/" element={<RequireAuth><SystemLayout><Dashboard /></SystemLayout></RequireAuth>} />
          <Route path="/tasks" element={<RequireAuth><SystemLayout><Tasks /></SystemLayout></RequireAuth>} />
          <Route path="/routines" element={<RequireAuth><SystemLayout><Routines /></SystemLayout></RequireAuth>} />
          <Route path="/analytics" element={<RequireAuth><SystemLayout><Analytics /></SystemLayout></RequireAuth>} />

          {/* 404 兜底 */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
