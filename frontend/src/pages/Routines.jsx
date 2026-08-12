import { useEffect, useState } from 'react';
import RoutineService from '../api/RoutineService';
import { Sun, Moon, CheckCircle2, Loader2, AlertCircle, TrendingUp } from 'lucide-react';

/**
 * 取本地时区的 YYYY-MM-DD。
 *
 * 不能用 toISOString().split('T')[0] —— 那个先转 UTC。在 UTC+2 的时区里
 * 凌晨 1 点打卡会被记成前一天，打卡日期和用户看到的"今天"对不上。
 */
const toLocalDateString = (d) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

const RoutineCard = ({ routine, onComplete, completing }) => {
  const isMorning = routine.type === 'MORNING';
  
  return (
    <div className="glass fade-in" style={{ 
      padding: '1.5rem', 
      borderRadius: '1.25rem', 
      display: 'flex', 
      justifyContent: 'space-between', 
      alignItems: 'center',
      marginBottom: '1rem',
      borderLeft: `4px solid ${isMorning ? 'var(--warning)' : 'var(--accent-secondary)'}`
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
        <div style={{ 
          padding: '0.875rem', 
          backgroundColor: isMorning ? 'rgba(245, 158, 11, 0.1)' : 'rgba(129, 140, 248, 0.1)', 
          color: isMorning ? 'var(--warning)' : 'var(--accent-secondary)',
          borderRadius: '1rem'
        }}>
          {isMorning ? <Sun size={24} /> : <Moon size={24} />}
        </div>
        {/*
          此处原本渲染 `+{routine.stabilityImpact} Stability`，但 stabilityImpact
          在整个 routine 领域里都不存在（实体、DTO、service 都没有这个字段），
          所以它永远显示的是字面量 "+undefined Stability"。
          稳定度加成是后端按 RoutineType 定的固定值（morning/evening 各 20 分），
          不是每条 routine 自带的属性，改用 description 展示真实数据。
        */}
        <div>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '700', marginBottom: '0.25rem' }}>{routine.name}</h3>
          {routine.description && (
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
              {routine.description}
            </div>
          )}
        </div>
      </div>
      
      <button 
        onClick={() => onComplete(routine.id)}
        disabled={completing}
        className="primary-btn"
        style={{ 
          padding: '0.6rem 1.25rem', 
          fontSize: '0.875rem',
          minWidth: '120px'
        }}
      >
        {completing ? <Loader2 className="animate-spin" size={18} /> : (
          <>
            <CheckCircle2 size={18} /> Check In
          </>
        )}
      </button>
    </div>
  );
};

const Routines = () => {
  const [definitions, setDefinitions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [completingId, setCompletingId] = useState(null);

  // 取数只有这一条路径。setState 放在 promise 回调里而不是在 effect 体里调一个
  // async 取数函数 —— 后者会被 react-hooks/set-state-in-effect 标记。
  // 参见 Analytics.jsx / DailyLog.jsx / Tasks.jsx 的同一写法。
  useEffect(() => {
    let cancelled = false;

    RoutineService.listDefinitions()
      .then((data) => {
        if (cancelled) return;
        setDefinitions(data || []);
        setError(null);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const handleComplete = async (id) => {
    setCompletingId(id);
    setError(null);
    setNotice(null);
    try {
      // RoutineCompletionRequest 要的是 { routineDefinitionId, date, completed }，
      // 三个字段都是 @NotNull。此前这里发的是 { routineDefinitionId, completedAt } ——
      // completedAt 后端根本不存在，而两个必填字段一个都没发，于是控制器上的 @Valid
      // 在进 service 之前就把每一次点击挡成 400。这个按钮从未成功过一次。
      //
      // 影响比看起来大：routine 完成是稳定度分数里 40 分（morning 20 + evening 20）
      // 的唯一来源，而 StabilityService 是从 routine_completions 表反查的 ——
      // 这张表因此一直是空的。
      await RoutineService.recordCompletion({
        routineDefinitionId: id,
        date: toLocalDateString(new Date()),
        completed: true,
      });
      setNotice('Habit tracked! System stability increased.');
    } catch (err) {
      setError('Failed to record habit: ' + err.message);
    } finally {
      setCompletingId(null);
    }
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '5rem' }}>
      <Loader2 className="animate-spin" size={40} color="var(--accent-primary)" />
    </div>
  );

  const morningRoutines = definitions.filter(d => d.type === 'MORNING');
  const eveningRoutines = definitions.filter(d => d.type === 'EVENING');

  return (
    <div className="fade-in">
      <header style={{ marginBottom: '3rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: '800', marginBottom: '0.5rem' }}>Routines & Habits</h1>
        <p style={{ color: 'var(--text-secondary)' }}>Standardize your daily operations to maximize system performance.</p>
      </header>

      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {notice && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--success)', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <CheckCircle2 size={20} /> {notice}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(450px, 1fr))', gap: '2.5rem' }}>
        {/* Morning Section */}
        <section>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <Sun size={24} style={{ color: 'var(--warning)' }} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>Morning Flow</h2>
            <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--border-color)' }}></div>
          </div>
          
          {morningRoutines.length === 0 ? (
            <div className="glass" style={{ padding: '2rem', textAlign: 'center', borderRadius: '1.25rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              No morning protocols active.
            </div>
          ) : (
            morningRoutines.map(d => (
              <RoutineCard 
                key={d.id} 
                routine={d} 
                onComplete={handleComplete} 
                completing={completingId === d.id} 
              />
            ))
          )}
        </section>

        {/* Evening Section */}
        <section>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <Moon size={24} style={{ color: 'var(--accent-secondary)' }} />
            <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>Evening Reflection</h2>
            <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--border-color)' }}></div>
          </div>

          {eveningRoutines.length === 0 ? (
            <div className="glass" style={{ padding: '2rem', textAlign: 'center', borderRadius: '1.25rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              No evening protocols active.
            </div>
          ) : (
            eveningRoutines.map(d => (
              <RoutineCard 
                key={d.id} 
                routine={d} 
                onComplete={handleComplete} 
                completing={completingId === d.id} 
              />
            ))
          )}
        </section>
      </div>

      <div className="glass" style={{ marginTop: '3rem', padding: '2rem', borderRadius: '1.5rem', background: 'linear-gradient(135deg, rgba(56, 189, 248, 0.05) 0%, rgba(129, 140, 248, 0.05) 100%)', display: 'flex', alignItems: 'center', gap: '2rem' }}>
        <div style={{ padding: '1rem', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '1rem' }}>
          <TrendingUp size={32} color="var(--accent-primary)" />
        </div>
        <div>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '700', marginBottom: '0.5rem' }}>Habit Consistency</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '600px' }}>
            Daily routines are the backbone of your system stability. Completing these tasks consistently 
            provides a passive boost to your overall productivity index.
          </p>
        </div>
      </div>
    </div>
  );
};

export default Routines;
