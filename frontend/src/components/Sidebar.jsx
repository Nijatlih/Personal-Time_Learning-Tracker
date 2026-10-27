import { LayoutDashboard, NotebookPen, CheckSquare, Repeat, CalendarClock, BarChart3, LogOut, User } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useLanguage } from '../hooks/useLanguage';

const menuItems = [
  { labelKey: 'nav.dashboard', icon: LayoutDashboard, path: '/' },
  { labelKey: 'nav.dailyLog', icon: NotebookPen, path: '/daily-log' },
  { labelKey: 'nav.tasks', icon: CheckSquare, path: '/tasks' },
  { labelKey: 'nav.routines', icon: Repeat, path: '/routines' },
  { labelKey: 'nav.schedule', icon: CalendarClock, path: '/schedule' },
  { labelKey: 'nav.analytics', icon: BarChart3, path: '/analytics' },
];

const Sidebar = () => {
  const { user, logout } = useAuth();
  const { t } = useLanguage();

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <span className="sidebar-mark">P</span>
        <div>
          <div>PTLT</div>
          <small>{t('sidebar.tagline')}</small>
        </div>
      </div>

      <nav className="sidebar-nav" aria-label={t('nav.primary')}>
        {menuItems.map(({ labelKey, icon: Icon, path }) => (
          <NavLink
            key={path}
            to={path}
            end={path === '/'}
            className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
          >
            <Icon size={19} aria-hidden="true" />
            <span>{t(labelKey)}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-user">
        <div className="sidebar-avatar"><User size={19} aria-hidden="true" /></div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="sidebar-username">{user?.email?.split('@')[0] || t('sidebar.operator')}</div>
          <div className="sidebar-caption">{t('sidebar.owner')}</div>
        </div>
      </div>

      <button type="button" onClick={logout} className="sidebar-logout">
        <LogOut size={19} aria-hidden="true" />
        {t('sidebar.signOut')}
      </button>
    </aside>
  );
};

export default Sidebar;
