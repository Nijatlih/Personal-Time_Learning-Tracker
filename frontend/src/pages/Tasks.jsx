import { useEffect, useState } from 'react';
import TaskService from '../api/TaskService';
import { Plus, CheckCircle2, Circle, Tag, AlertCircle, Loader2, X } from 'lucide-react';
import { useLanguage } from '../hooks/useLanguage';

const Tasks = () => {
  const { t } = useLanguage();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Modal & Form State
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [newTask, setNewTask] = useState({ title: '', description: '', isCoreTask: false });

  // 取数只有一条路径：这个 effect。需要刷新的地方 bump reloadCount，而不是再调一次
  // 取数函数 —— 后者就是 useEffect(() => { fetchTasks(); }, [fetchTasks]) 那个形状，
  // 会被 react-hooks/set-state-in-effect 标记（规则是静态的，只看 effect 体里有没有
  // 间接触发 setState 的调用，与 await 在第几句无关）。setState 放在 promise 回调里
  // 才是它认可的形态，参见 Analytics.jsx / DailyLog.jsx。
  const [reloadCount, setReloadCount] = useState(0);
  const reload = () => setReloadCount((n) => n + 1);

  useEffect(() => {
    let cancelled = false;

    TaskService.list()
      .then((data) => {
        if (cancelled) return;
        setTasks(data || []);
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
  }, [reloadCount]);

  const handleCreateTask = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await TaskService.create(newTask);
      setNewTask({ title: '', description: '', isCoreTask: false });
      setShowModal(false);
      reload();
    } catch (err) {
      alert(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleComplete = async (id, status) => {
    if (status === 'COMPLETED') return; // 目前不支持取消完成（后端逻辑）
    
    // 乐观 UI 更新
    setTasks(prev => prev.map(t => t.id === id ? { ...t, status: 'COMPLETED' } : t));
    
    try {
      await TaskService.complete(id);
    } catch (err) {
      // 措辞：这里跟 sync 无关（sync 模块已删除，见 adr/0004），失败的就是完成任务本身
      alert(t('tasks.completeFailed', { message: err.message }));
      reload(); // 失败时回滚
    }
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '5rem' }}>
      <Loader2 className="animate-spin" size={40} color="var(--accent-primary)" />
    </div>
  );

  return (
    <div className="fade-in">
      <div className="page-heading" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2.5rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: '800', marginBottom: '0.5rem' }}>{t('tasks.title')}</h1>
          <p style={{ color: 'var(--text-secondary)' }}>{t('tasks.subtitle')}</p>
        </div>
        <button onClick={() => setShowModal(true)} className="primary-btn">
          <Plus size={20} /> {t('tasks.new')}
        </button>
      </div>

      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      <div style={{ display: 'grid', gap: '1rem' }}>
        {tasks.length === 0 ? (
          <div className="glass" style={{ padding: '4rem', textAlign: 'center', borderRadius: '1.5rem' }}>
            <Circle size={44} style={{ marginBottom: '1rem', color: 'var(--accent-primary)' }} aria-hidden="true" />
            <h3 style={{ marginBottom: '0.5rem' }}>{t('tasks.emptyTitle')}</h3>
            <p style={{ color: 'var(--text-secondary)' }}>{t('tasks.emptyBody')}</p>
          </div>
        ) : (
          tasks.map(task => (
            <div key={task.id} className="glass" style={{ 
              padding: '1.25rem 1.5rem', 
              borderRadius: '1.25rem',
              display: 'flex',
              alignItems: 'center',
              gap: '1.5rem',
              opacity: task.status === 'COMPLETED' ? 0.5 : 1
            }}>
              <button
                type="button"
                onClick={() => handleToggleComplete(task.id, task.status)}
                disabled={task.status === 'COMPLETED'}
                aria-label={task.status === 'COMPLETED' ? t('tasks.completed', { title: task.title }) : t('tasks.complete', { title: task.title })}
                style={{ width: '44px', height: '44px', display: 'grid', placeItems: 'center', cursor: 'pointer', color: task.status === 'COMPLETED' ? 'var(--success)' : 'var(--text-secondary)', background: 'transparent', border: 0, borderRadius: '0.75rem' }}
              >
                {task.status === 'COMPLETED' ? <CheckCircle2 size={26} /> : <Circle size={26} />}
              </button>
              
              <div style={{ flex: 1 }}>
                <div style={{ 
                  fontSize: '1.125rem', 
                  fontWeight: '600',
                  textDecoration: task.status === 'COMPLETED' ? 'line-through' : 'none'
                }}>
                  {task.title}
                </div>
                {task.description && (
                  <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                    {task.description}
                  </div>
                )}
              </div>

              {task.isCoreTask && (
                <div style={{ 
                  display: 'flex', alignItems: 'center', gap: '0.4rem', 
                  padding: '0.4rem 0.8rem', borderRadius: '2rem', 
                  backgroundColor: 'rgba(56, 189, 248, 0.1)', 
                  border: '1px solid var(--accent-primary)',
                  color: 'var(--accent-primary)', fontSize: '0.75rem', fontWeight: '700'
                }}>
                  <Tag size={12} /> {t('tasks.core')}
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* --- Create Task Modal --- */}
      {showModal && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
          backgroundColor: 'var(--overlay)', backdropFilter: 'blur(8px)',
          display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
        }}>
          <div className="glass fade-in" style={{ width: '100%', maxWidth: '500px', padding: '2.5rem', borderRadius: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
              <h2 style={{ fontSize: '1.5rem', fontWeight: '700' }}>{t('tasks.modalTitle')}</h2>
              <button type="button" className="icon-button" onClick={() => setShowModal(false)} aria-label={t('common.closeDialog')}>
                <X size={20} />
              </button>
            </div>
            
            <form onSubmit={handleCreateTask}>
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>{t('tasks.fieldTitle')}</label>
                <input 
                  type="text" 
                  className="input-field"
                  value={newTask.title}
                  onChange={e => setNewTask({...newTask, title: e.target.value})}
                  required
                  placeholder={t('tasks.titlePlaceholder')}
                />
              </div>
              
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>{t('tasks.fieldDescription')}</label>
                <textarea 
                  className="input-field"
                  style={{ minHeight: '100px', resize: 'vertical' }}
                  value={newTask.description}
                  onChange={e => setNewTask({...newTask, description: e.target.value})}
                  placeholder={t('tasks.descriptionPlaceholder')}
                />
              </div>

              <div style={{ marginBottom: '2.5rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <input 
                  type="checkbox" 
                  id="core-check"
                  checked={newTask.isCoreTask}
                  onChange={e => setNewTask({...newTask, isCoreTask: e.target.checked})}
                  style={{ width: '20px', height: '20px', accentColor: 'var(--accent-primary)' }}
                />
                <label htmlFor="core-check" style={{ fontSize: '0.875rem', fontWeight: '600', cursor: 'pointer' }}>{t('tasks.markCore')}</label>
              </div>

              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="submit" className="primary-btn" style={{ flex: 1 }} disabled={submitting}>
                  {submitting ? <Loader2 className="animate-spin" size={20} /> : t('tasks.initialize')}
                </button>
                <button type="button" className="glass" style={{ flex: 1, minHeight: '44px', borderRadius: '0.75rem', color: 'var(--text-primary)', cursor: 'pointer' }} onClick={() => setShowModal(false)}>
                  {t('common.cancel')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Tasks;
