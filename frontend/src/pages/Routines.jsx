import React, { useEffect, useState, useCallback } from 'react';
import RoutineService from '../api/RoutineService';
import { Sun, Moon, CheckCircle2, Zap, Loader2, AlertCircle, TrendingUp } from 'lucide-react';

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
        <div>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '700', marginBottom: '0.25rem' }}>{routine.name}</h3>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', color: 'var(--accent-primary)', fontWeight: '600' }}>
              <Zap size={14} /> +{routine.stabilityImpact} Stability
            </div>
          </div>
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
  const [completingId, setCompletingId] = useState(null);

  const fetchDefinitions = useCallback(async () => {
    try {
      setError(null);
      const data = await RoutineService.listDefinitions();
      setDefinitions(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDefinitions();
  }, [fetchDefinitions]);

  const handleComplete = async (id) => {
    setCompletingId(id);
    try {
      await RoutineService.recordCompletion({
        routineDefinitionId: id,
        completedAt: new Date().toISOString()
      });
      // 可以增加一个成功的微反馈，或者重新拉取数据（如果后端支持完成状态显示）
      alert('Habit tracked! System stability increased.');
    } catch (err) {
      alert('Failed to record habit: ' + err.message);
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
