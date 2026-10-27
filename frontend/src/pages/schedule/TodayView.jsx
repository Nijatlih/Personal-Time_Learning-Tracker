// frontend/src/pages/schedule/TodayView.jsx
import { useEffect, useState } from 'react';
import { Loader2, AlertCircle } from 'lucide-react';
import TimeBlockService from '../../api/TimeBlockService';
import TaskService from '../../api/TaskService';
import TimelineGrid from '../../components/schedule/TimelineGrid';
import TimeBlockModal from '../../components/schedule/TimeBlockModal';
import { toLocalDateString, sumBlockMinutes, formatDuration } from '../../utils/schedule';
import { useLanguage } from '../../hooks/useLanguage';

const TodayView = () => {
  const { t } = useLanguage();
  const [blocks, setBlocks] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadCount, setReloadCount] = useState(0);
  const [modalState, setModalState] = useState(null);

  const today = toLocalDateString(new Date());

  useEffect(() => {
    let cancelled = false;

    Promise.all([TimeBlockService.getByDate(today), TaskService.list()])
      .then(([blockData, taskData]) => {
        if (cancelled) return;
        setBlocks(blockData || []);
        setTasks(taskData || []);
        setError(null);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, [reloadCount, today]);

  const reload = () => setReloadCount((n) => n + 1);

  const tasksById = new Map(tasks.map((task) => [task.id, task]));
  const enrichedBlocks = blocks.map((block) => ({
    ...block,
    taskTitle: block.taskId ? tasksById.get(block.taskId)?.title : null,
  }));

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
      <Loader2 className="animate-spin" size={36} color="var(--accent-primary)" />
    </div>
  );

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
        <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          {t('schedule.todayScheduled', { duration: formatDuration(sumBlockMinutes(blocks)) })}
        </div>
      </div>

      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '1rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      <TimelineGrid
        days={[{ date: today, label: t('schedule.today'), blocks: enrichedBlocks }]}
        onSlotClick={(date, time) => setModalState({ mode: 'create', date, startTime: time })}
        onBlockClick={(block) => setModalState({ mode: 'edit', block })}
      />

      {modalState && (
        <TimeBlockModal
          mode={modalState.mode}
          date={modalState.date}
          startTime={modalState.startTime}
          block={modalState.block}
          tasks={tasks}
          onClose={() => setModalState(null)}
          onSaved={() => {
            setModalState(null);
            reload();
          }}
        />
      )}
    </div>
  );
};

export default TodayView;
