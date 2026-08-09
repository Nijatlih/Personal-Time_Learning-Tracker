import { LayoutDashboard, CheckSquare, Repeat, BarChart3, LogOut, User } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const Sidebar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const menuItems = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, path: '/' },
    { id: 'tasks', label: 'Tasks', icon: CheckSquare, path: '/tasks' },
    { id: 'routines', label: 'Routines', icon: Repeat, path: '/routines' },
    { id: 'analytics', label: 'Analytics', icon: BarChart3, path: '/analytics' },
  ];

  return (
    <aside style={{
      width: '280px',
      backgroundColor: 'var(--bg-sidebar)',
      borderRight: '1px solid var(--border-color)',
      height: '100vh',
      display: 'flex',
      flexDirection: 'column',
      padding: '2.5rem 1.5rem',
      position: 'sticky',
      top: 0
    }}>
      <div style={{
        fontSize: '1.75rem',
        fontWeight: '900',
        marginBottom: '3.5rem',
        padding: '0 0.5rem',
        background: 'linear-gradient(to right, var(--accent-primary), var(--accent-secondary))',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        letterSpacing: '-0.02em'
      }}>
        PTLT.OS
      </div>

      <nav style={{ flex: 1 }}>
        {menuItems.map((item) => {
          const isActive = location.pathname === item.path;
          return (
            <div
              key={item.id}
              onClick={() => navigate(item.path)}
              style={{
                display: 'flex',
                alignItems: 'center',
                padding: '0.875rem 1.25rem',
                color: isActive ? 'var(--accent-primary)' : 'var(--text-secondary)',
                backgroundColor: isActive ? 'rgba(56, 189, 248, 0.08)' : 'transparent',
                borderRadius: '0.75rem',
                marginBottom: '0.5rem',
                cursor: 'pointer',
                transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                fontWeight: isActive ? '700' : '500',
                border: isActive ? '1px solid rgba(56, 189, 248, 0.1)' : '1px solid transparent'
              }}
            >
              <item.icon size={20} style={{ marginRight: '1rem', opacity: isActive ? 1 : 0.7 }} />
              {item.label}
            </div>
          );
        })}
      </nav>

      {/* User Info Section */}
      <div style={{
        padding: '1.25rem',
        backgroundColor: 'rgba(255,255,255,0.03)',
        borderRadius: '1.25rem',
        marginBottom: '1rem',
        border: '1px solid var(--border-color)',
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem'
      }}>
        <div style={{ 
          width: '40px', height: '40px', borderRadius: '50%', 
          backgroundColor: 'var(--accent-primary)', display: 'flex', 
          justifyContent: 'center', alignItems: 'center', color: 'var(--bg-main)' 
        }}>
          <User size={20} />
        </div>
        <div style={{ flex: 1, overflow: 'hidden' }}>
          <div style={{ fontSize: '0.875rem', fontWeight: '700', color: 'var(--text-primary)', whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>
            {user?.email?.split('@')[0] || 'Operator'}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>System Active</div>
        </div>
      </div>

      <button
        onClick={logout}
        style={{
          display: 'flex',
          alignItems: 'center',
          padding: '0.875rem 1.25rem',
          color: 'var(--danger)',
          cursor: 'pointer',
          borderRadius: '0.75rem',
          transition: 'all 0.2s',
          border: 'none',
          backgroundColor: 'transparent',
          width: '100%',
          fontWeight: '600',
          fontSize: '0.9375rem'
        }}
      >
        <LogOut size={20} style={{ marginRight: '1rem' }} />
        Disconnect
      </button>
    </aside>
  );
};

export default Sidebar;
