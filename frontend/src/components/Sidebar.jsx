import { LayoutDashboard, NotebookPen, CheckSquare, Repeat, CalendarClock, BarChart3, LogOut, User } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const menuItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/' },
  { label: 'Daily Log', icon: NotebookPen, path: '/daily-log' },
  { label: 'Tasks', icon: CheckSquare, path: '/tasks' },
  { label: 'Routines', icon: Repeat, path: '/routines' },
  { label: 'Schedule', icon: CalendarClock, path: '/schedule' },
  { label: 'Analytics', icon: BarChart3, path: '/analytics' },
];

const Sidebar = () => {
  const { user, logout } = useAuth();

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-mark">P</span>
        <div>
          <div>PTLT</div>
          <small>Personal tracker</small>
        </div>
      </div>

      <nav className="sidebar-nav" aria-label="Primary navigation">
        {menuItems.map(({ label, icon: Icon, path }) => (
          <NavLink
            key={path}
            to={path}
            end={path === '/'}
            className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
          >
            <Icon size={19} aria-hidden="true" />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-user">
        <div className="sidebar-avatar"><User size={19} aria-hidden="true" /></div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="sidebar-username">{user?.email?.split('@')[0] || 'Operator'}</div>
          <div className="sidebar-caption">Workspace owner</div>
        </div>
      </div>

      <button type="button" onClick={logout} className="sidebar-logout">
        <LogOut size={19} aria-hidden="true" />
        Sign out
      </button>
    </aside>
  );
};

export default Sidebar;
