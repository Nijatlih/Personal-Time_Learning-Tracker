// frontend/src/components/schedule/ScheduleSlotModal.jsx
import { useState } from 'react';
import { Loader2, X, Trash2 } from 'lucide-react';
import ScheduleSlotService from '../../api/ScheduleSlotService';
import { addMinutesToTime, DAY_ORDER } from '../../utils/schedule';
import { useLanguage } from '../../hooks/useLanguage';

const buildInitialForm = (slot) => {
  if (slot) {
    return {
      dayOfWeek: slot.dayOfWeek,
      startTime: slot.startTime.slice(0, 5),
      endTime: slot.endTime.slice(0, 5),
      title: slot.title || '',
      description: slot.description || '',
    };
  }
  return {
    dayOfWeek: 'MONDAY',
    startTime: '09:00',
    endTime: addMinutesToTime('09:00', 60),
    title: '',
    description: '',
  };
};

// slot: null for create, the full ScheduleSlotResponse (with `version`) for edit
const ScheduleSlotModal = ({ slot, onClose, onSaved }) => {
  const { t } = useLanguage();
  const [form, setForm] = useState(() => buildInitialForm(slot));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const payload = {
        dayOfWeek: form.dayOfWeek,
        startTime: form.startTime,
        endTime: form.endTime,
        title: form.title,
        description: form.description,
      };
      if (slot) {
        await ScheduleSlotService.update(slot.id, { ...payload, version: slot.version });
      } else {
        await ScheduleSlotService.create(payload);
      }
      onSaved();
    } catch (err) {
      setError(err.message || t('schedule.slotSaveFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(t('schedule.slotConfirmDelete'))) return;
    setSubmitting(true);
    setError(null);
    try {
      await ScheduleSlotService.delete(slot.id);
      onSaved();
    } catch (err) {
      setError(err.message || t('schedule.slotDeleteFailed'));
      setSubmitting(false);
    }
  };

  return (
    <div role="presentation" style={{ position: 'fixed', inset: 0, zIndex: 1000, display: 'grid', placeItems: 'center', padding: '1.5rem', background: 'var(--overlay)', backdropFilter: 'blur(8px)' }}>
      <div role="dialog" aria-modal="true" aria-labelledby="slot-dialog-title" className="glass fade-in" style={{ width: '100%', maxWidth: '480px', padding: '2rem', borderRadius: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
          <h2 id="slot-dialog-title" style={{ fontSize: '1.3rem', fontWeight: 750 }}>
            {slot ? t('schedule.slotEdit') : t('schedule.slotNew')}
          </h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label={t('common.closeDialog')}><X size={19} /></button>
        </div>

        {error && (
          <div role="alert" style={{ padding: '0.8rem 1rem', marginBottom: '1.25rem', borderRadius: '0.8rem', border: '1px solid var(--danger)', color: 'var(--danger)', fontSize: '0.85rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1.25rem' }}>
            <label htmlFor="slot-day" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('schedule.dayOfWeek')}</label>
            <select
              id="slot-day"
              className="input-field"
              value={form.dayOfWeek}
              onChange={(event) => setForm((current) => ({ ...current, dayOfWeek: event.target.value }))}
            >
              {DAY_ORDER.map((day) => (
                <option key={day} value={day}>{t(`day.${day}`)}</option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.25rem' }}>
            <div style={{ flex: 1 }}>
              <label htmlFor="slot-start" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('schedule.start')}</label>
              <input
                id="slot-start"
                type="time"
                className="input-field"
                value={form.startTime}
                onChange={(event) => setForm((current) => ({ ...current, startTime: event.target.value }))}
                required
              />
            </div>
            <div style={{ flex: 1 }}>
              <label htmlFor="slot-end" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('schedule.end')}</label>
              <input
                id="slot-end"
                type="time"
                className="input-field"
                value={form.endTime}
                onChange={(event) => setForm((current) => ({ ...current, endTime: event.target.value }))}
                required
              />
            </div>
          </div>

          <div style={{ marginBottom: '1.25rem' }}>
            <label htmlFor="slot-title" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('schedule.fieldTitle')}</label>
            <input
              id="slot-title"
              className="input-field"
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              maxLength={200}
            />
          </div>

          <div style={{ marginBottom: '1.75rem' }}>
            <label htmlFor="slot-description" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('schedule.fieldDescription')}</label>
            <textarea
              id="slot-description"
              className="input-field"
              value={form.description}
              onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
              maxLength={500}
              rows={3}
              style={{ resize: 'vertical' }}
            />
          </div>

          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button type="submit" className="primary-btn" style={{ flex: 1 }} disabled={submitting}>
              {submitting ? <Loader2 className="animate-spin" size={19} /> : (slot ? t('common.saveChanges') : t('schedule.slotCreate'))}
            </button>
            {slot && (
              <button type="button" className="icon-button" onClick={handleDelete} disabled={submitting} aria-label={t('schedule.slotDelete')} title={t('common.delete')}>
                <Trash2 size={18} color="var(--danger)" />
              </button>
            )}
            <button type="button" className="glass" style={{ minHeight: '44px', padding: '0.7rem 1.25rem', borderRadius: '0.8rem', color: 'var(--text-primary)', cursor: 'pointer' }} onClick={onClose}>{t('common.cancel')}</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ScheduleSlotModal;
