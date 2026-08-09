import { useEffect, useState } from 'react';
import AnalyticsService from '../api/AnalyticsService';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';
import { TrendingUp, Target, RefreshCcw, Info } from 'lucide-react';

const COLORS = {
  GREEN: '#10b981',
  YELLOW: '#f59e0b',
  RED: '#ef4444',
  GRAY: '#64748b'
};

const Analytics = () => {
  const [stabilityData, setStabilityData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const end = new Date().toISOString().split('T')[0];
    const start = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    
    AnalyticsService.getStabilitySummary(start, end)
      .then(res => {
        setStabilityData(res);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setLoading(false);
      });
  }, []);

  const preparePieData = () => {
    if (!stabilityData || !stabilityData.statusCounts) return [];
    return Object.entries(stabilityData.statusCounts).map(([status, count]) => ({
      name: status,
      value: count
    }));
  };

  if (loading) return <div style={{ color: 'var(--text-secondary)' }}>Calculating analytics...</div>;

  return (
    <div className="fade-in">
      <header style={{ marginBottom: '2.5rem' }}>
        <h1 style={{ fontSize: '1.875rem', fontWeight: '700' }}>Analytics</h1>
        <p style={{ color: 'var(--text-secondary)' }}>Deep dive into your system performance over the last 30 days.</p>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1.5rem', marginBottom: '2rem' }}>
        <div className="glass" style={{ padding: '1.5rem', borderRadius: '1.25rem' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '600', marginBottom: '0.5rem' }}>AVG STABILITY</div>
          <div style={{ fontSize: '2rem', fontWeight: '700', color: 'var(--accent-primary)' }}>
            {stabilityData?.averageScore?.toFixed(1) || '0.0'}
          </div>
        </div>
        <div className="glass" style={{ padding: '1.5rem', borderRadius: '1.25rem' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '600', marginBottom: '0.5rem' }}>MAX GREEN STREAK</div>
          <div style={{ fontSize: '2rem', fontWeight: '700', color: 'var(--success)' }}>
            {stabilityData?.maxGreenStreak || 0} Days
          </div>
        </div>
        <div className="glass" style={{ padding: '1.5rem', borderRadius: '1.25rem' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '600', marginBottom: '0.5rem' }}>MAX UPTIME</div>
          <div style={{ fontSize: '2rem', fontWeight: '700', color: 'var(--accent-secondary)' }}>
            {stabilityData?.maxUptimeStreak || 0} Days
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '1.5rem' }}>
        <div className="glass" style={{ padding: '2rem', borderRadius: '1.5rem' }}>
          <h2 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <TrendingUp size={18} /> Stability Distribution
          </h2>
          <div style={{ height: '300px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={preparePieData()}
                  innerRadius={80}
                  outerRadius={100}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {preparePieData().map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[entry.name] || COLORS.GRAY} />
                  ))}
                </Pie>
                <RechartsTooltip 
                  contentStyle={{ backgroundColor: 'var(--bg-sidebar)', border: '1px solid var(--border-color)', borderRadius: '0.75rem' }}
                />
                <Legend verticalAlign="bottom" height={36}/>
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="glass" style={{ padding: '2rem', borderRadius: '1.5rem' }}>
          <h2 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Info size={18} /> Performance Insights
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ padding: '1rem', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Target size={16} color="var(--accent-primary)" /> Task Velocity
              </div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                Your task completion speed is optimal. Focus on maintaining High Priority tasks.
              </p>
            </div>
            <div style={{ padding: '1rem', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <RefreshCcw size={16} color="var(--success)" /> Habit Consistency
              </div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                Your morning routines have a 85% consistency rate. Keep it up!
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
