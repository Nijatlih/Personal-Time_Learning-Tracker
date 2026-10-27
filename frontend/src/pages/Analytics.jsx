import { useEffect, useState } from 'react';
import AnalyticsService from '../api/AnalyticsService';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';
import { TrendingUp, Target, RefreshCcw, Info, AlertCircle } from 'lucide-react';
import { useLanguage } from '../hooks/useLanguage';

const COLORS = {
  GREEN: '#10b981',
  YELLOW: '#f59e0b',
  RED: '#ef4444',
  GRAY: '#64748b'
};

/** 取本地时区的 YYYY-MM-DD；toISOString() 先转 UTC，跨时区会差一天。 */
const toLocalDateString = (d) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

/** 百分比格式化。null / undefined 一律显示为无数据，绝不当成 0。 */
const formatPercent = (value) =>
  typeof value === 'number' ? `${value.toFixed(0)}%` : '—';

/** Performance Insights 里的一行：指标名 + 百分比 + 分子分母。 */
// 加载失败且后端没给出错误文本时用这个键，渲染时再按当前语言翻译。
const LOAD_FAILED = 'analytics.loadFailed';

const Insight = ({ label, value, detail }) => (
  <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem', marginTop: '0.5rem' }}>
    <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', flex: 1 }}>{label}</span>
    <span style={{ fontSize: '0.9375rem', fontWeight: '700' }}>{value}</span>
    {/* 同时给出分子分母：光有百分比看不出样本量，"100%" 可能只是 1/1 */}
    <span style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>({detail})</span>
  </div>
);

const Analytics = () => {
  const { t } = useLanguage();
  const [stabilityData, setStabilityData] = useState(null);
  const [taskStats, setTaskStats] = useState(null);
  const [routineStats, setRoutineStats] = useState(null);
  const [loading, setLoading] = useState(true);
  // 此前本页完全没有 error state：.catch 里只 console.error 然后 setLoading(false)，
  // 于是请求失败时页面照常渲染「AVG STABILITY 0.0 / MAX GREEN STREAK 0 Days」和一个空饼图。
  // 用户无从区分"后端坏了"和"这段时间真的是 0"。
  // Tasks.jsx / Routines.jsx 都有错误横幅，只有这一页没有。
  const [error, setError] = useState(null);

  useEffect(() => {
    const end = toLocalDateString(new Date());
    const start = toLocalDateString(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000));

    // 三个统计一起拉。Performance Insights 此前是两段硬编码文案，
    // 而这两个真正提供数据的 service 方法定义好了却零调用点。
    Promise.all([
      AnalyticsService.getStabilitySummary(start, end),
      AnalyticsService.getTaskStats(start, end),
      AnalyticsService.getRoutineStats(start, end),
    ])
      .then(([stability, tasks, routines]) => {
        setStabilityData(stability);
        setTaskStats(tasks);
        setRoutineStats(routines);
        setLoading(false);
      })
      .catch(err => {
        console.error(err);
        setError(err.message || LOAD_FAILED);
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

  if (loading) return <div style={{ color: 'var(--text-secondary)' }}>{t('analytics.loading')}</div>;

  const header = (
    <header style={{ marginBottom: '2.5rem' }}>
      <h1 style={{ fontSize: '1.875rem', fontWeight: '700' }}>{t('analytics.title')}</h1>
      <p style={{ color: 'var(--text-secondary)' }}>{t('analytics.subtitle')}</p>
    </header>
  );

  // 出错时只渲染横幅，不渲染统计卡片和饼图 —— 拿不到数据时展示一堆 0
  // 正是这条缺陷本身，横幅底下再放一排 "0.0" 依然是在报假数。
  // 横幅写法与 Tasks.jsx / Routines.jsx 已有的保持一致。
  if (error) {
    return (
      <div className="fade-in">
        {header}
        <div className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error === LOAD_FAILED ? t(LOAD_FAILED) : error}
        </div>
      </div>
    );
  }

  return (
    <div className="fade-in">
      {header}

      <div className="analytics-stats" style={{ gap: '1.5rem', marginBottom: '2rem' }}>
        <div className="glass" style={{ padding: '1.5rem', borderRadius: '1.25rem' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '600', marginBottom: '0.5rem' }}>{t('analytics.avgStability')}</div>
          <div style={{ fontSize: '2rem', fontWeight: '700', color: 'var(--accent-primary)' }}>
            {stabilityData?.averageScore?.toFixed(1) || '0.0'}
          </div>
        </div>
        <div className="glass" style={{ padding: '1.5rem', borderRadius: '1.25rem' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '600', marginBottom: '0.5rem' }}>{t('analytics.maxGreen')}</div>
          <div style={{ fontSize: '2rem', fontWeight: '700', color: 'var(--success)' }}>
            {t('common.days', { count: stabilityData?.maxGreenStreak || 0 })}
          </div>
        </div>
        <div className="glass" style={{ padding: '1.5rem', borderRadius: '1.25rem' }}>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '600', marginBottom: '0.5rem' }}>{t('analytics.maxUptime')}</div>
          <div style={{ fontSize: '2rem', fontWeight: '700', color: 'var(--accent-secondary)' }}>
            {t('common.days', { count: stabilityData?.maxUptimeStreak || 0 })}
          </div>
        </div>
      </div>

      <div className="two-column-grid" style={{ display: 'grid', gap: '1.5rem' }}>
        <div className="glass" style={{ padding: '2rem', borderRadius: '1.5rem' }}>
          <h2 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <TrendingUp size={18} /> {t('analytics.distribution')}
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
            <Info size={18} /> {t('analytics.insights')}
          </h2>
          {/*
            这两块此前是硬编码文案："Your morning routines have a 85% consistency rate"
            —— 那个 85% 是凭空写死的数字，不来自任何接口；另一句还提到 "High Priority tasks"，
            而 priority 是已删除的陈旧 SPA 的幻影字段，后端从来没有这个概念。

            比显示 "+undefined" 更糟：那种一眼就知道坏了，而 "85%" 看起来完全合理，
            因此会被相信。现在两块都读真实接口，且**没有数据时显示"没有数据"**，
            不回退到任何听起来正确的话。
          */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ padding: '1rem', backgroundColor: 'var(--surface-subtle)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Target size={16} color="var(--accent-primary)" /> {t('analytics.taskVelocity')}
              </div>
              {taskStats?.totalTasks > 0 ? (
                <>
                  <Insight
                    label={t('analytics.scheduledDone')}
                    value={formatPercent(taskStats.completionRate)}
                    detail={t('analytics.ofTotal', { done: taskStats.completedTasks, total: taskStats.totalTasks })}
                  />
                  <Insight
                    label={t('analytics.coreDone')}
                    value={formatPercent(taskStats.coreTaskCompletionRate)}
                    detail={t('analytics.ofTotal', { done: taskStats.coreTasksCompleted, total: taskStats.coreTasksTotal })}
                  />
                </>
              ) : (
                <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                  {t('analytics.noTasks')}
                </p>
              )}
            </div>
            <div style={{ padding: '1rem', backgroundColor: 'var(--surface-subtle)', borderRadius: '1rem', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.875rem', fontWeight: '600', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <RefreshCcw size={16} color="var(--success)" /> {t('analytics.habitConsistency')}
              </div>
              {routineStats && (routineStats.morningTotalDays > 0 || routineStats.eveningTotalDays > 0) ? (
                <>
                  <Insight
                    label={t('analytics.morningRoutine')}
                    value={formatPercent(routineStats.morningCompletionRate)}
                    detail={t('analytics.eligibleDays', { done: routineStats.morningCompletedDays, total: routineStats.morningTotalDays })}
                  />
                  <Insight
                    label={t('analytics.eveningRoutine')}
                    value={formatPercent(routineStats.eveningCompletionRate)}
                    detail={t('analytics.eligibleDays', { done: routineStats.eveningCompletedDays, total: routineStats.eveningTotalDays })}
                  />
                </>
              ) : (
                <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                  {t('analytics.noRoutines')}
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
