// frontend/src/pages/schedule/TemplatesView.jsx
import { useEffect, useState } from 'react';
import { Loader2, AlertCircle, Plus, Pencil } from 'lucide-react';
import ScheduleSlotService from '../../api/ScheduleSlotService';
import ScheduleSlotModal from '../../components/schedule/ScheduleSlotModal';

const DAY_ORDER = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const DAY_LABELS = {
  MONDAY: '周一', TUESDAY: '周二', WEDNESDAY: '周三', THURSDAY: '周四',
  FRIDAY: '周五', SATURDAY: '周六', SUNDAY: '周日',
};

const TemplatesView = () => {
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadCount, setReloadCount] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [editingSlot, setEditingSlot] = useState(null);

  useEffect(() => {
    let cancelled = false;

    ScheduleSlotService.list()
      .then((data) => {
        if (cancelled) return;
        setSlots(data || []);
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

  const reload = () => setReloadCount((n) => n + 1);

  const openCreate = () => {
    setEditingSlot(null);
    setShowModal(true);
  };

  const openEdit = (slot) => {
    setEditingSlot(slot);
    setShowModal(true);
  };

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
      <Loader2 className="animate-spin" size={36} color="var(--accent-primary)" />
    </div>
  );

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '0.75rem' }}>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          每周固定安排的参考列表，不会自动生成时间块。
        </p>
        <button type="button" className="primary-btn" onClick={openCreate}>
          <Plus size={18} /> New template
        </button>
      </div>

      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '1rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {DAY_ORDER.map((day) => {
        const daySlots = slots
          .filter((slot) => slot.dayOfWeek === day)
          .sort((a, b) => a.startTime.localeCompare(b.startTime));

        return (
          <section key={day} style={{ marginBottom: '1.5rem' }}>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem' }}>
              {DAY_LABELS[day]}
            </div>
            {daySlots.length === 0 ? (
              <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>没有安排</div>
            ) : (
              daySlots.map((slot) => (
                <div
                  key={slot.id}
                  className="glass"
                  style={{ padding: '0.75rem 1rem', marginBottom: '0.5rem', borderRadius: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                >
                  <div>
                    <div style={{ fontWeight: 600 }}>{slot.title || 'Untitled'}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      {slot.startTime.slice(0, 5)} – {slot.endTime.slice(0, 5)}
                    </div>
                  </div>
                  <button type="button" className="icon-button" onClick={() => openEdit(slot)} aria-label={`Edit ${slot.title || 'template'}`}>
                    <Pencil size={17} />
                  </button>
                </div>
              ))
            )}
          </section>
        );
      })}

      {showModal && (
        <ScheduleSlotModal
          slot={editingSlot}
          onClose={() => setShowModal(false)}
          onSaved={() => {
            setShowModal(false);
            reload();
          }}
        />
      )}
    </div>
  );
};

export default TemplatesView;
