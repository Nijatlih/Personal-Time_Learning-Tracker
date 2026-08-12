import { useEffect, useState } from 'react';
import DashboardService from '../api/DashboardService';
import TaskService from '../api/TaskService';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { TrendingUp, Award, Zap, Activity, CheckCircle2, ArrowRight, AlertCircle } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const StatCard = ({ label, value, icon: Icon, color, delay }) => (
  <div className="glass fade-in" style={{ padding: '1.5rem', borderRadius: '1.25rem', flex: 1, animationDelay: `${delay}s` }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
      <div>
        <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '700', textTransform: 'uppercase', marginBottom: '0.5rem', letterSpacing: '0.05em' }}>{label}</div>
        <div style={{ fontSize: '1.75rem', fontWeight: '800' }}>{value}</div>
      </div>
      <div style={{ padding: '0.75rem', backgroundColor: `${color}15`, borderRadius: '0.75rem', color: color }}>
        <Icon size={22} />
      </div>
    </div>
  </div>
);

const Dashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [miniTasks, setMiniTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 取数只有这一条路径。setState 放在 promise 回调里而不是在 effect 体里调一个
  // async 取数函数 —— 后者会被 react-hooks/set-state-in-effect 标记。
  // 参见 Analytics.jsx / DailyLog.jsx / Tasks.jsx 的同一写法。
  useEffect(() => {
    let cancelled = false;

    Promise.all([
      DashboardService.getDashboard(),
      TaskService.list('TODO'),
    ])
      .then(([dash, tasks]) => {
        if (cancelled) return;
        setData(dash);
        setMiniTasks(tasks.slice(0, 3));
        setError(null);
      })
      .catch((err) => {
        // 此前这里只 console.error，页面照常渲染一堆 0 —— 与 Analytics 那条
        // 缺陷（F7）同形。加上错误横幅，让"拿不到数据"和"数据真的是 0"能区分。
        console.error(err);
        if (!cancelled) setError(err.message || '无法加载 Dashboard 数据');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) return <div className="loading-screen">SYNCING SYSTEM...</div>;

  return (
    <div className="fade-in">
      <header style={{ marginBottom: '3rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <div style={{ color: 'var(--accent-primary)', fontWeight: '700', fontSize: '0.875rem', marginBottom: '0.5rem', textTransform: 'uppercase' }}>Command Center</div>
          <h1 style={{ fontSize: '2.25rem', fontWeight: '800' }}>Welcome, {user?.email?.split('@')[0]}</h1>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>System Uptime</div>
          <div style={{ fontWeight: '700', fontSize: '1.125rem' }}>{data?.uptimeStreak || 0} Days</div>
        </div>
      </header>

      {/* 与 Tasks/Routines/Analytics 已有的错误横幅写法保持一致 */}
      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {/* --- Key Metrics --- */}
      <div style={{ display: 'flex', gap: '1.5rem', marginBottom: '2.5rem' }}>
        <StatCard label="Stability Index" value={data?.currentStabilityScore || 0} icon={Zap} color="#38bdf8" delay={0.1} />
        <StatCard label="System Status" value={data?.currentStabilityStatus || 'UNKNOWN'} icon={Activity} color="#10b981" delay={0.2} />
        <StatCard label="Efficiency Streak" value={`${data?.greenStreak || 0}d`} icon={Award} color="#818cf8" delay={0.3} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
        {/* --- Stability Chart --- */}
        <div className="glass" style={{ padding: '2rem', borderRadius: '1.5rem', height: '450px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>Stability Performance</h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', color: 'var(--success)' }}>
              <TrendingUp size={16} /> +12% from last week
            </div>
          </div>
          
          <ResponsiveContainer width="100%" height="80%">
            <LineChart data={data?.recentRecords || []}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.03)" vertical={false} />
              <XAxis dataKey="date" stroke="var(--text-secondary)" fontSize={12} tickLine={false} axisLine={false} dy={10} />
              <YAxis stroke="var(--text-secondary)" fontSize={12} tickLine={false} axisLine={false} domain={[0, 100]} />
              <Tooltip 
                contentStyle={{ backgroundColor: 'var(--bg-sidebar)', border: '1px solid var(--border-color)', borderRadius: '1rem', boxShadow: 'var(--shadow-lg)' }}
                itemStyle={{ color: 'var(--accent-primary)', fontWeight: '700' }}
              />
              <Line 
                type="monotone" 
                dataKey="stabilityScore" 
                stroke="var(--accent-primary)" 
                strokeWidth={4} 
                dot={{ r: 6, fill: 'var(--accent-primary)', strokeWidth: 3, stroke: 'var(--bg-main)' }}
                activeDot={{ r: 8, strokeWidth: 0 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* --- Quick Tasks --- */}
        <div className="glass" style={{ padding: '2rem', borderRadius: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>Immediate Focus</h2>
            <button onClick={() => navigate('/tasks')} style={{ background: 'none', border: 'none', color: 'var(--accent-primary)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.875rem', fontWeight: '600' }}>
              View All <ArrowRight size={14} />
            </button>
          </div>

          <div style={{ display: 'grid', gap: '1rem' }}>
            {miniTasks.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '2rem' }}>No immediate tasks. System optimized.</p>
            ) : (
              miniTasks.map(t => (
                <div key={t.id} style={{ padding: '1rem', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '1rem', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <CheckCircle2 size={20} color="var(--text-secondary)" />
                  <div style={{ fontSize: '0.9375rem', fontWeight: '600', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
                </div>
              ))
            )}
          </div>

          <div style={{ marginTop: '2.5rem', padding: '1.5rem', backgroundColor: 'rgba(56, 189, 248, 0.05)', borderRadius: '1.25rem', border: '1px dashed var(--accent-primary)' }}>
            <h4 style={{ fontSize: '0.875rem', fontWeight: '700', marginBottom: '0.5rem', color: 'var(--accent-primary)' }}>System Tip</h4>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', lineHeight: '1.4' }}>
              Maintain a 7-day Green streak to unlock advanced system analytics and performance boosters.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
