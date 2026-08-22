// frontend/src/components/schedule/TimeBlockModal.jsx
import { useState } from 'react';
import { Loader2, X, Trash2 } from 'lucide-react';
import TimeBlockService from '../../api/TimeBlockService';
import { addMinutesToTime } from '../../utils/schedule';

const buildInitialForm = (mode, startTime, block) => {
  if (mode === 'edit' && block) {
    return {
      startTime: block.startTime.slice(0, 5),
      endTime: block.endTime.slice(0, 5),
      title: block.title || '',
      description: block.description || '',
      taskId: block.taskId || '',
    };
  }
  return {
    startTime,
    endTime: addMinutesToTime(startTime, 60),
    title: '',
    description: '',
    taskId: '',
  };
};

// mode: 'create' | 'edit'. Create needs `date` + `startTime` (from the clicked slot).
// Edit needs `block` (the full TimeBlockResponse, including `version`).
const TimeBlockModal = ({ mode, date, startTime, block, tasks, onClose, onSaved }) => {
  const [form, setForm] = useState(() => buildInitialForm(mode, startTime, block));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const availableTasks = tasks.filter(
    (task) => task.status !== 'COMPLETED' && task.status !== 'ARCHIVED'
      || task.id === block?.taskId,
  );

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const payload = {
        taskId: form.taskId || null,
        startTime: form.startTime,
        endTime: form.endTime,
        title: form.title,
        description: form.description,
      };
      if (mode === 'create') {
        await TimeBlockService.create({ ...payload, date });
      } else {
        await TimeBlockService.update(block.id, { ...payload, version: block.version });
      }
      onSaved();
    } catch (err) {
      setError(err.message || 'Unable to save time block');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete this time block?')) return;
    setSubmitting(true);
    setError(null);
    try {
      await TimeBlockService.delete(block.id);
      onSaved();
    } catch (err) {
      setError(err.message || 'Unable to delete time block');
      setSubmitting(false);
    }
  };

  return (
    <div role="presentation" style={{ position: 'fixed', inset: 0, zIndex: 1000, display: 'grid', placeItems: 'center', padding: '1.5rem', background: 'var(--overlay)', backdropFilter: 'blur(8px)' }}>
      <div role="dialog" aria-modal="true" aria-labelledby="time-block-dialog-title" className="glass fade-in" style={{ width: '100%', maxWidth: '480px', padding: '2rem', borderRadius: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
          <h2 id="time-block-dialog-title" style={{ fontSize: '1.3rem', fontWeight: 750 }}>
            {mode === 'create' ? 'New time block' : 'Edit time block'}
          </h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close dialog"><X size={19} /></button>
        </div>

        {error && (
          <div role="alert" style={{ padding: '0.8rem 1rem', marginBottom: '1.25rem', borderRadius: '0.8rem', border: '1px solid var(--danger)', color: 'var(--danger)', fontSize: '0.85rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.25rem' }}>
            <div style={{ flex: 1 }}>
              <label htmlFor="block-start" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Start</label>
              <input
                id="block-start"
                type="time"
                className="input-field"
                value={form.startTime}
                onChange={(event) => setForm((current) => ({ ...current, startTime: event.target.value }))}
                required
              />
            </div>
            <div style={{ flex: 1 }}>
              <label htmlFor="block-end" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>End</label>
              <input
                id="block-end"
                type="time"
                className="input-field"
                value={form.endTime}
                onChange={(event) => setForm((current) => ({ ...current, endTime: event.target.value }))}
                required
              />
            </div>
          </div>

          <div style={{ marginBottom: '1.25rem' }}>
            <label htmlFor="block-title" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Title</label>
            <input
              id="block-title"
              className="input-field"
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              maxLength={200}
              placeholder="What are you doing?"
            />
          </div>

          <div style={{ marginBottom: '1.25rem' }}>
            <label htmlFor="block-description" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Description</label>
            <textarea
              id="block-description"
              className="input-field"
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
              maxLength={500}
              rows={3}
              style={{ resize: 'vertical' }}
            />
          </div>

          <div style={{ marginBottom: '1.75rem' }}>
            <label htmlFor="block-task" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Linked task (optional)</label>
            <select
              id="block-task"
              className="input-field"
              value={form.taskId}
              onChange={(event) => setForm((current) => ({ ...current, taskId: event.target.value }))}
            >
              <option value="">None</option>
              {availableTasks.map((task) => (
                <option key={task.id} value={task.id}>{task.title}</option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button type="submit" className="primary-btn" style={{ flex: 1 }} disabled={submitting}>
              {submitting ? <Loader2 className="animate-spin" size={19} /> : (mode === 'create' ? 'Create block' : 'Save changes')}
            </button>
            {mode === 'edit' && (
              <button type="button" className="icon-button" onClick={handleDelete} disabled={submitting} aria-label="Delete time block" title="Delete">
                <Trash2 size={18} color="var(--danger)" />
              </button>
            )}
            <button type="button" className="glass" style={{ minHeight: '44px', padding: '0.7rem 1.25rem', borderRadius: '0.8rem', color: 'var(--text-primary)', cursor: 'pointer' }} onClick={onClose}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default TimeBlockModal;
