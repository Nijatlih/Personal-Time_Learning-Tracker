import { useEffect, useState } from 'react';
import RoutineService from '../api/RoutineService';
import {
  Sun, Moon, CheckCircle2, Loader2, AlertCircle, TrendingUp, Plus, Pencil, X,
} from 'lucide-react';
import { useLanguage } from '../hooks/useLanguage';

const toLocalDateString = (d) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

const EMPTY_FORM = { type: 'MORNING', name: '', description: '' };

const RoutineCard = ({ routine, onComplete, onEdit, completing }) => {
  const { t } = useLanguage();
  const isMorning = routine.type === 'MORNING';

  return (
    <div className="glass fade-in" style={{
      padding: '1.5rem',
      borderRadius: '1.25rem',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      gap: '1rem',
      marginBottom: '1rem',
      borderLeft: `4px solid ${isMorning ? 'var(--warning)' : 'var(--accent-secondary)'}`,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', minWidth: 0 }}>
        <div style={{
          padding: '0.875rem',
          backgroundColor: isMorning ? 'rgba(245, 158, 11, 0.1)' : 'rgba(129, 140, 248, 0.1)',
          color: isMorning ? 'var(--warning)' : 'var(--accent-secondary)',
          borderRadius: '1rem',
        }}>
          {isMorning ? <Sun size={24} /> : <Moon size={24} />}
        </div>
        <div style={{ minWidth: 0 }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '700', marginBottom: '0.25rem' }}>{routine.name}</h3>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
            {routine.description || t(`routines.defaultDesc.${routine.type}`)}
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexShrink: 0 }}>
        <button
          type="button"
          className="icon-button"
          onClick={() => onEdit(routine)}
          aria-label={t('routines.edit', { name: routine.name })}
          title={t('routines.editTitle')}
        >
          <Pencil size={17} />
        </button>
        <button
          type="button"
          onClick={() => onComplete(routine.id)}
          disabled={completing}
          className="primary-btn"
          style={{ minWidth: '118px' }}
        >
          {completing ? <Loader2 className="animate-spin" size={18} /> : (
            <><CheckCircle2 size={18} /> {t('routines.checkIn')}</>
          )}
        </button>
      </div>
    </div>
  );
};

const Routines = () => {
  const { t } = useLanguage();
  const [definitions, setDefinitions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [completingId, setCompletingId] = useState(null);
  const [reloadCount, setReloadCount] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);

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

    return () => { cancelled = true; };
  }, [reloadCount]);

  const missingTypes = ['MORNING', 'EVENING'].filter(
    (type) => !definitions.some((definition) => definition.type === type),
  );

  const openCreate = (preferredType) => {
    const type = preferredType && missingTypes.includes(preferredType)
      ? preferredType
      : missingTypes[0];
    if (!type) return;
    setEditing(null);
    setForm({ ...EMPTY_FORM, type });
    setShowModal(true);
    setError(null);
  };

  const openEdit = (routine) => {
    setEditing(routine);
    setForm({ type: routine.type, name: routine.name, description: routine.description || '' });
    setShowModal(true);
    setError(null);
  };

  const closeModal = () => {
    if (submitting) return;
    setShowModal(false);
    setEditing(null);
  };

  const handleSave = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      if (editing) {
        await RoutineService.updateDefinition(editing.id, {
          name: form.name,
          description: form.description,
          version: editing.version,
        });
        setNotice(t('routines.updated', { name: form.name }));
      } else {
        await RoutineService.createDefinition(form);
        setNotice(t('routines.created', { name: form.name }));
      }
      setShowModal(false);
      setEditing(null);
      setLoading(true);
      setReloadCount((count) => count + 1);
    } catch (err) {
      setError(err.message || t('routines.saveFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleComplete = async (id) => {
    setCompletingId(id);
    setError(null);
    setNotice(null);
    try {
      await RoutineService.recordCompletion({
        routineDefinitionId: id,
        date: toLocalDateString(new Date()),
        completed: true,
      });
      setNotice(t('routines.checkedIn'));
    } catch (err) {
      setError(t('routines.recordFailed', { message: err.message }));
    } finally {
      setCompletingId(null);
    }
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '5rem' }}>
      <Loader2 className="animate-spin" size={40} color="var(--accent-primary)" />
    </div>
  );

  const morningRoutines = definitions.filter((definition) => definition.type === 'MORNING');
  const eveningRoutines = definitions.filter((definition) => definition.type === 'EVENING');

  const renderSection = (type, routines) => {
    const isMorning = type === 'MORNING';
    return (
      <section>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
          {isMorning
            ? <Sun size={24} style={{ color: 'var(--warning)' }} />
            : <Moon size={24} style={{ color: 'var(--accent-secondary)' }} />}
          <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>
            {isMorning ? t('routines.morningFlow') : t('routines.eveningReflection')}
          </h2>
          <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--border-color)' }} />
        </div>

        {routines.length === 0 ? (
          <div className="glass" style={{ padding: '2rem', textAlign: 'center', borderRadius: '1.25rem' }}>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1.25rem' }}>
              {isMorning ? t('routines.noMorning') : t('routines.noEvening')}
            </p>
            <button type="button" className="primary-btn" onClick={() => openCreate(type)}>
              <Plus size={18} /> {isMorning ? t('routines.createMorning') : t('routines.createEvening')}
            </button>
          </div>
        ) : routines.map((routine) => (
          <RoutineCard
            key={routine.id}
            routine={routine}
            onComplete={handleComplete}
            onEdit={openEdit}
            completing={completingId === routine.id}
          />
        ))}
      </section>
    );
  };

  return (
    <div className="fade-in">
      <header className="page-heading" style={{ marginBottom: '3rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: '800', marginBottom: '0.5rem' }}>{t('routines.title')}</h1>
          <p style={{ color: 'var(--text-secondary)' }}>{t('routines.subtitle')}</p>
        </div>
        {missingTypes.length > 0 && (
          <button type="button" className="primary-btn" onClick={() => openCreate()}>
            <Plus size={19} /> {t('routines.new')}
          </button>
        )}
      </header>

      {error && !showModal && (
        <div role="alert" className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {notice && (
        <div aria-live="polite" className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--success)', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <CheckCircle2 size={20} /> {notice}
        </div>
      )}

      <div className="routines-grid" style={{ display: 'grid', gap: '2.5rem' }}>
        {renderSection('MORNING', morningRoutines)}
        {renderSection('EVENING', eveningRoutines)}
      </div>

      <div className="glass" style={{ marginTop: '3rem', padding: '2rem', borderRadius: '1.5rem', display: 'flex', alignItems: 'center', gap: '2rem' }}>
        <div style={{ padding: '1rem', backgroundColor: 'var(--accent-soft)', borderRadius: '1rem' }}>
          <TrendingUp size={32} color="var(--accent-primary)" />
        </div>
        <div>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '700', marginBottom: '0.5rem' }}>{t('routines.howTitle')}</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', maxWidth: '680px' }}>
            {t('routines.howBody')}
          </p>
        </div>
      </div>

      {showModal && (
        <div role="presentation" style={{ position: 'fixed', inset: 0, zIndex: 1000, display: 'grid', placeItems: 'center', padding: '1.5rem', background: 'var(--overlay)', backdropFilter: 'blur(8px)' }}>
          <div role="dialog" aria-modal="true" aria-labelledby="routine-dialog-title" className="glass fade-in" style={{ width: '100%', maxWidth: '520px', padding: '2rem', borderRadius: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.75rem' }}>
              <div>
                <h2 id="routine-dialog-title" style={{ fontSize: '1.4rem', fontWeight: 750 }}>{editing ? t('routines.dialogEdit') : t('routines.dialogCreate')}</h2>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.25rem' }}>{t('routines.dialogSub')}</p>
              </div>
              <button type="button" className="icon-button" onClick={closeModal} aria-label={t('common.closeDialog')}><X size={19} /></button>
            </div>

            {error && (
              <div role="alert" style={{ padding: '0.8rem 1rem', marginBottom: '1.25rem', borderRadius: '0.8rem', border: '1px solid var(--danger)', color: 'var(--danger)', background: 'color-mix(in srgb, var(--danger) 8%, transparent)', fontSize: '0.85rem' }}>
                {error}
              </div>
            )}

            <form onSubmit={handleSave}>
              <div style={{ marginBottom: '1.25rem' }}>
                <label htmlFor="routine-type" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('routines.timeOfDay')}</label>
                <select
                  id="routine-type"
                  className="input-field"
                  value={form.type}
                  disabled={Boolean(editing)}
                  onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))}
                >
                  {(editing ? [editing.type] : missingTypes).map((type) => (
                    <option key={type} value={type}>{type === 'MORNING' ? t('routines.morning') : t('routines.evening')}</option>
                  ))}
                </select>
              </div>

              <div style={{ marginBottom: '1.25rem' }}>
                <label htmlFor="routine-name" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('routines.name')}</label>
                <input
                  id="routine-name"
                  className="input-field"
                  value={form.name}
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                  required
                  maxLength={100}
                  autoFocus
                  placeholder={t('routines.namePlaceholder')}
                />
              </div>

              <div style={{ marginBottom: '1.75rem' }}>
                <label htmlFor="routine-description" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>{t('routines.description')}</label>
                <textarea
                  id="routine-description"
                  className="input-field"
                  value={form.description}
                  onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                  maxLength={500}
                  rows={4}
                  style={{ resize: 'vertical' }}
                  placeholder={t('routines.descriptionPlaceholder')}
                />
              </div>

              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <button type="submit" className="primary-btn" style={{ flex: 1 }} disabled={submitting}>
                  {submitting ? <Loader2 className="animate-spin" size={19} /> : (editing ? t('common.saveChanges') : t('routines.dialogCreate'))}
                </button>
                <button type="button" className="glass" style={{ minHeight: '44px', padding: '0.7rem 1.25rem', borderRadius: '0.8rem', color: 'var(--text-primary)', cursor: 'pointer' }} onClick={closeModal}>{t('common.cancel')}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Routines;
