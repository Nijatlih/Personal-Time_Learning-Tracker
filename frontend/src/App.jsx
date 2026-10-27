import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './hooks/AuthProvider';
import { ThemeProvider } from './hooks/ThemeProvider';
import { LanguageProvider } from './hooks/LanguageProvider';
import { useAuth } from './hooks/useAuth';
import Sidebar from './components/Sidebar';
import ThemeToggle from './components/ThemeToggle';
import LanguageToggle from './components/LanguageToggle';
import Dashboard from './pages/Dashboard';
import DailyLog from './pages/DailyLog';
import Login from './pages/Login';
import Register from './pages/Register';
import Tasks from './pages/Tasks';
import Routines from './pages/Routines';
import Schedule from './pages/Schedule';
import Analytics from './pages/Analytics';
import './styles/globals.css';
import { useLanguage } from './hooks/useLanguage';

const RequireAuth = ({ children }) => {
  const { token } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  return children;
};

const PublicLayout = ({ children }) => (
  <div className="public-page">
    <div className="public-theme-toggle"><LanguageToggle /><ThemeToggle /></div>
    {children}
  </div>
);

const SystemLayout = ({ children }) => {
  const { t } = useLanguage();
  return (
  <div className="app-layout">
    <Sidebar />
    <main className="main-content" id="main-content">
      <header className="system-header">
        <div className="system-header-inner">
          <div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 700 }}>
              {t('shell.workspace')}
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', marginTop: '0.15rem' }}>
              {t('shell.statusLabel')} <span style={{ color: 'var(--success)', fontWeight: 700 }}>{t('shell.online')}</span>
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <LanguageToggle />
            <ThemeToggle />
          </div>
        </div>
      </header>
      <div className="page-content">{children}</div>
    </main>
  </div>
  );
};

function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<PublicLayout><Login /></PublicLayout>} />
            <Route path="/register" element={<PublicLayout><Register /></PublicLayout>} />
            <Route path="/" element={<RequireAuth><SystemLayout><Dashboard /></SystemLayout></RequireAuth>} />
            <Route path="/daily-log" element={<RequireAuth><SystemLayout><DailyLog /></SystemLayout></RequireAuth>} />
            <Route path="/tasks" element={<RequireAuth><SystemLayout><Tasks /></SystemLayout></RequireAuth>} />
            <Route path="/routines" element={<RequireAuth><SystemLayout><Routines /></SystemLayout></RequireAuth>} />
            <Route path="/schedule" element={<RequireAuth><SystemLayout><Schedule /></SystemLayout></RequireAuth>} />
            <Route path="/analytics" element={<RequireAuth><SystemLayout><Analytics /></SystemLayout></RequireAuth>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
      </LanguageProvider>
    </ThemeProvider>
  );
}

export default App;
