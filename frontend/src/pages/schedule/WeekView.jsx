// frontend/src/pages/schedule/WeekView.jsx
import { useEffect, useState } from 'react';
import { Loader2, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react';
import TimeBlockService from '../../api/TimeBlockService';
import TaskService from '../../api/TaskService';
import TimelineGrid from '../../components/schedule/TimelineGrid';
import TimeBlockModal from '../../components/schedule/TimeBlockModal';
import {
  toLocalDateString, addDays, startOfWeek, sumBlockMinutes, formatDuration,
} from '../../utils/schedule';

const DAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

const WeekView = () => {
  const [weekOffset, setWeekOffset] = useState(0);
  const [blocks, setBlocks] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadCount, setReloadCount] = useState(0);
  const [modalState, setModalState] = useState(null);

  const weekStart = addDays(startOfWeek(new Date()), weekOffset * 7);
  const weekStartStr = toLocalDateString(weekStart);
  const weekEndStr = toLocalDateString(addDays(weekStart, 6));

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      TimeBlockService.getByDateRange(weekStartStr, weekEndStr),
      TaskService.list(),
    ])
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
  }, [reloadCount, weekStartStr, weekEndStr]);

  const reload = () => setReloadCount((n) => n + 1);

  const tasksById = new Map(tasks.map((task) => [task.id, task]));

  const days = Array.from({ length: 7 }, (_, index) => {
    const date = toLocalDateString(addDays(weekStart, index));
    const dayBlocks = blocks
      .filter((block) => block.date === date)
      .map((block) => ({
        ...block,
        taskTitle: block.taskId ? tasksById.get(block.taskId)?.title : null,
      }));
    return { date, label: `${DAY_LABELS[index]} ${date.slice(5)}`, blocks: dayBlocks };
  });

  if (loading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
      <Loader2 className="animate-spin" size={36} color="var(--accent-primary)" />
    </div>
  );

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <button type="button" className="icon-button" onClick={() => setWeekOffset((n) => n - 1)} aria-label="Previous week">
            <ChevronLeft size={18} />
          </button>
          <span style={{ fontSize: '0.875rem', fontWeight: 700 }}>{weekStartStr} – {weekEndStr}</span>
          <button type="button" className="icon-button" onClick={() => setWeekOffset((n) => n + 1)} aria-label="Next week">
            <ChevronRight size={18} />
          </button>
        </div>
        <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          本周已排 {formatDuration(sumBlockMinutes(blocks))}
        </div>
      </div>

      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '1rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      <TimelineGrid
        days={days}
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

export default WeekView;
