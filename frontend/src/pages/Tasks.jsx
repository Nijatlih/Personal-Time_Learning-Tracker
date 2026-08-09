import React, { useEffect, useState, useCallback } from 'react';
import TaskService from '../api/TaskService';
import { Plus, CheckCircle2, Circle, Tag, AlertCircle, Loader2, X } from 'lucide-react';

const Tasks = () => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Modal & Form State
  const [showModal, setShowModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [newTask, setNewTask] = useState({ title: '', description: '', isCoreTask: false });

  const fetchTasks = useCallback(async () => {
    try {
      setError(null);
      const data = await TaskService.list();
      setTasks(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const handleCreateTask = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await TaskService.create(newTask);
      setNewTask({ title: '', description: '', isCoreTask: false });
      setShowModal(false);
      fetchTasks();
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
      alert('Sync failed: ' + err.message);
      fetchTasks(); // 失败时回滚
    }
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '5rem' }}>
      <Loader2 className="animate-spin" size={40} color="var(--accent-primary)" />
    </div>
  );

  return (
    <div className="fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2.5rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: '800', marginBottom: '0.5rem' }}>System Tasks</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Manage your core objectives and daily operations.</p>
        </div>
        <button onClick={() => setShowModal(true)} className="primary-btn">
          <Plus size={20} /> New Task
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
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🎯</div>
            <h3 style={{ marginBottom: '0.5rem' }}>No Active Tasks</h3>
            <p style={{ color: 'var(--text-secondary)' }}>Your system is clear. Add a task to start tracking.</p>
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
              <div 
                onClick={() => handleToggleComplete(task.id, task.status)}
                style={{ cursor: 'pointer', color: task.status === 'COMPLETED' ? 'var(--success)' : 'var(--text-secondary)' }}
              >
                {task.status === 'COMPLETED' ? <CheckCircle2 size={26} /> : <Circle size={26} />}
              </div>
              
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
                  <Tag size={12} /> CORE
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
          backgroundColor: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(8px)',
          display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000
        }}>
          <div className="glass fade-in" style={{ width: '100%', maxWidth: '500px', padding: '2.5rem', borderRadius: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
              <h2 style={{ fontSize: '1.5rem', fontWeight: '700' }}>New Objective</h2>
              <X size={24} style={{ cursor: 'pointer', color: 'var(--text-secondary)' }} onClick={() => setShowModal(false)} />
            </div>
            
            <form onSubmit={handleCreateTask}>
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Title</label>
                <input 
                  type="text" 
                  className="input-field"
                  value={newTask.title}
                  onChange={e => setNewTask({...newTask, title: e.target.value})}
                  required
                  placeholder="What is the objective?"
                />
              </div>
              
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Description</label>
                <textarea 
                  className="input-field"
                  style={{ minHeight: '100px', resize: 'vertical' }}
                  value={newTask.description}
                  onChange={e => setNewTask({...newTask, description: e.target.value})}
                  placeholder="Additional context (optional)..."
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
                <label htmlFor="core-check" style={{ fontSize: '0.875rem', fontWeight: '600', cursor: 'pointer' }}>Mark as Core System Task</label>
              </div>

              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="submit" className="primary-btn" style={{ flex: 1 }} disabled={submitting}>
                  {submitting ? <Loader2 className="animate-spin" size={20} /> : 'Initialize Task'}
                </button>
                <button type="button" className="glass" style={{ flex: 1, borderRadius: '0.75rem', border: 'none', cursor: 'pointer' }} onClick={() => setShowModal(false)}>
                  Cancel
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
