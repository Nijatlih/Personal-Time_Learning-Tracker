# Schedule Phase A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the existing, already-tested `TimeBlock` and `ScheduleSlot` backend APIs into a new frontend `/schedule` page with three views (today timeline, week timeline, weekly templates), per `docs/superpowers/specs/2026-08-22-schedule-phase-a-design.md`.

**Architecture:** Pure frontend addition. Two new API clients following `TaskService.js`'s shape, a shared `TimelineGrid` presentational component reused by both the today and week views (today is the one-day special case of week), two modal forms following `Routines.jsx`'s modal pattern, three view components, one container page, and small edits to `App.jsx`/`Sidebar.jsx` for routing/nav. Zero backend changes.

**Tech Stack:** React 19 + Vite, no new dependencies. This repo has no JS test runner (`frontend/package.json` has no `vitest`/`jest`; none of the 7 existing pages have test files) — adding one is out of scope for this feature. Verification per task is `npm run lint` (CI-blocking, per `CLAUDE.md`); the final task adds `npm run build` plus manual click-through, matching how every prior frontend change in this repo has been verified.

---

## Before you start

Read these once, they explain conventions every task below relies on:
- `frontend/src/pages/Tasks.jsx` — the `reloadCount` + `.then/.catch` data-fetch shape every view in this plan copies (required by `CLAUDE.md`'s `react-hooks/set-state-in-effect` rule)
- `frontend/src/pages/Routines.jsx` — the modal-with-`version`-based-update shape `TimeBlockModal`/`ScheduleSlotModal` copy
- `frontend/src/api/client.js` — response interceptor unwraps to `response.data.data`; errors reject as `{ message, status }` (already-handled shape, nothing new to build)

Two backend facts this plan depends on (already true, verify only if something behaves unexpectedly):
- `TimeBlockResponse`/`ScheduleSlotResponse` serialize `LocalDate` as `"YYYY-MM-DD"` and `LocalTime` as `"HH:mm:ss"` (Spring Boot's default Jackson JSR-310 config, no override in `application.yml`)
- The JSON body deserializers for `LocalTime` accept `"HH:mm"` (no seconds) too — so a native `<input type="time">`'s value can be sent straight to the backend without reformatting

---

### Task 1: Date and duration utilities

**Files:**
- Create: `frontend/src/utils/schedule.js`

- [ ] **Step 1: Write the utility module**

```js
// frontend/src/utils/schedule.js

export const toLocalDateString = (date) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

export const addDays = (date, amount) => {
  const next = new Date(date);
  next.setDate(next.getDate() + amount);
  return next;
};

// 周一为一周的第一天，和后端 ScheduleSlot 用的 java.time.DayOfWeek 一致
export const startOfWeek = (date) => {
  const day = date.getDay(); // 0=Sunday..6=Saturday
  const diffToMonday = day === 0 ? -6 : 1 - day;
  return addDays(date, diffToMonday);
};

const parseTimeToMinutes = (time) => {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
};

export const blockDurationMinutes = (block) =>
  parseTimeToMinutes(block.endTime) - parseTimeToMinutes(block.startTime);

export const sumBlockMinutes = (blocks) =>
  blocks.reduce((total, block) => total + blockDurationMinutes(block), 0);

export const formatDuration = (totalMinutes) => {
  if (totalMinutes <= 0) return '0m';
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) return `${minutes}m`;
  if (minutes === 0) return `${hours}h`;
  return `${hours}h ${minutes}m`;
};

export const minutesToTimeString = (totalMinutes) => {
  const clamped = Math.max(0, Math.min(24 * 60 - 1, Math.round(totalMinutes)));
  const hours = Math.floor(clamped / 60);
  const minutes = clamped % 60;
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(hours)}:${pad(minutes)}`;
};

// 点击时间轴空白处时，把像素坐标换算成吸附到最近 15 分钟的时间字符串
export const pixelsToSnappedTime = (offsetY, pixelsPerHour) => {
  const rawMinutes = (offsetY / pixelsPerHour) * 60;
  const snapped = Math.round(rawMinutes / 15) * 15;
  return minutesToTimeString(snapped);
};

export const addMinutesToTime = (time, amount) =>
  minutesToTimeString(parseTimeToMinutes(time) + amount);
```

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors (this file has no JSX, only pure functions — nothing here can trip `react-hooks/*` or `react-refresh/only-export-components`)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/utils/schedule.js
git commit -m "feat: add schedule date and duration utilities"
```

---

### Task 2: API clients

**Files:**
- Create: `frontend/src/api/TimeBlockService.js`
- Create: `frontend/src/api/ScheduleSlotService.js`

- [ ] **Step 1: Write `TimeBlockService.js`**

```js
// frontend/src/api/TimeBlockService.js
import client from './client';

const TimeBlockService = {
  getByDate: (date) => client.get('/time-blocks/by-date', { params: { date } }),
  getByDateRange: (startDate, endDate) =>
    client.get('/time-blocks/range', { params: { startDate, endDate } }),
  create: (data) => client.post('/time-blocks', data),
  update: (id, data) => client.put(`/time-blocks/${id}`, data),
  delete: (id) => client.delete(`/time-blocks/${id}`),
};

export default TimeBlockService;
```

- [ ] **Step 2: Write `ScheduleSlotService.js`**

```js
// frontend/src/api/ScheduleSlotService.js
import client from './client';

const ScheduleSlotService = {
  list: () => client.get('/schedule-slots'),
  create: (data) => client.post('/schedule-slots', data),
  update: (id, data) => client.put(`/schedule-slots/${id}`, data),
  delete: (id) => client.delete(`/schedule-slots/${id}`),
};

export default ScheduleSlotService;
```

- [ ] **Step 3: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/TimeBlockService.js frontend/src/api/ScheduleSlotService.js
git commit -m "feat: add TimeBlock and ScheduleSlot API clients"
```

---

### Task 3: Schedule CSS (tabs + timeline grid)

**Files:**
- Modify: `frontend/src/styles/globals.css` (append at the true end of file — the file is 450 lines; earlier drafting of this plan incorrectly assumed EOF at line 409 based on a partial read. The last rule is actually the `@media (prefers-reduced-motion: reduce) { ... }` block closing at line 450.)

- [ ] **Step 1: Append the new classes**

Anchor on the file's real tail:

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

Append the new block after this (i.e. at the actual end of the file), not after the `::-webkit-scrollbar-thumb` rule.

Add at the end of `frontend/src/styles/globals.css`:

```css

.segmented-control {
  display: inline-flex;
  gap: 0.35rem;
  padding: 0.3rem;
  border-radius: 0.9rem;
  background: var(--surface-subtle);
  border: 1px solid var(--border-color);
}

.segmented-btn {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  padding: 0.55rem 1.1rem;
  border-radius: 0.65rem;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: background-color 180ms ease, color 180ms ease;
}

.segmented-btn.active {
  background: var(--accent-primary);
  color: #fff;
}

.timeline-wrapper {
  border: 1px solid var(--border-color);
  border-radius: 1rem;
  background: var(--bg-elevated);
  overflow: hidden;
}

.timeline-header-row {
  display: flex;
  border-bottom: 1px solid var(--border-color);
}

.timeline-header-spacer {
  flex-shrink: 0;
  width: 48px;
}

.timeline-header-cell {
  flex: 1;
  min-width: 0;
  padding: 0.6rem 0.4rem;
  text-align: center;
  font-size: 0.75rem;
  font-weight: 700;
}

.timeline-scroll {
  display: flex;
  max-height: 620px;
  overflow-y: auto;
}

.timeline-hours {
  flex-shrink: 0;
  width: 48px;
  position: relative;
}

.timeline-hour-label {
  position: absolute;
  right: 8px;
  transform: translateY(-50%);
  font-size: 0.7rem;
  color: var(--text-secondary);
}

.timeline-days {
  flex: 1;
  display: flex;
  position: relative;
}

.timeline-day-col {
  flex: 1;
  min-width: 0;
  position: relative;
  border-left: 1px solid var(--border-color);
  cursor: pointer;
}

.timeline-hour-line {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 1px dashed var(--border-color);
  pointer-events: none;
}

.timeline-block {
  position: absolute;
  left: 4px;
  right: 4px;
  background: var(--accent-soft);
  border: 1px solid var(--accent-primary);
  border-radius: 0.5rem;
  padding: 0.25rem 0.5rem;
  font-size: 0.72rem;
  line-height: 1.3;
  overflow: hidden;
  cursor: pointer;
  z-index: 1;
}

.timeline-block:hover {
  filter: brightness(1.15);
}

.timeline-block-task {
  display: block;
  font-size: 0.65rem;
  color: var(--text-secondary);
  margin-top: 0.15rem;
}
```

Use Edit with this anchor (the file's current last rule):

old_string:
```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

new_string: the same block, followed by the CSS block above (i.e. append, don't replace).

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors (CSS isn't linted by this config, this step just confirms the edit didn't break anything else)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/styles/globals.css
git commit -m "style: add schedule timeline and segmented-control CSS"
```

---

### Task 4: Shared `TimelineGrid` component

**Files:**
- Create: `frontend/src/components/schedule/TimelineGrid.jsx`

- [ ] **Step 1: Write the component**

```jsx
// frontend/src/components/schedule/TimelineGrid.jsx
import { useEffect, useRef } from 'react';
import { pixelsToSnappedTime } from '../../utils/schedule';

const PIXELS_PER_HOUR = 60;
const TOTAL_HOURS = 24;
const DEFAULT_SCROLL_HOUR = 7;

const HOUR_LABELS = Array.from({ length: TOTAL_HOURS }, (_, hour) => hour);

const timeToTop = (time) => {
  const [hours, minutes] = time.split(':').map(Number);
  return (hours * 60 + minutes) * (PIXELS_PER_HOUR / 60);
};

// days: [{ date: 'YYYY-MM-DD', label: string, blocks: TimeBlockResponse[] }]
const TimelineGrid = ({ days, onSlotClick, onBlockClick }) => {
  const scrollRef = useRef(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = DEFAULT_SCROLL_HOUR * PIXELS_PER_HOUR;
    }
  }, []);

  const handleColumnClick = (event, date) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const offsetY = event.clientY - rect.top;
    const time = pixelsToSnappedTime(offsetY, PIXELS_PER_HOUR);
    onSlotClick(date, time);
  };

  return (
    <div className="timeline-wrapper">
      <div className="timeline-header-row">
        <div className="timeline-header-spacer" />
        {days.map((day) => (
          <div key={day.date} className="timeline-header-cell">{day.label}</div>
        ))}
      </div>

      <div className="timeline-scroll" ref={scrollRef}>
        <div className="timeline-hours" style={{ height: TOTAL_HOURS * PIXELS_PER_HOUR }}>
          {HOUR_LABELS.map((hour) => (
            <div key={hour} className="timeline-hour-label" style={{ top: hour * PIXELS_PER_HOUR }}>
              {String(hour).padStart(2, '0')}:00
            </div>
          ))}
        </div>

        <div className="timeline-days">
          {days.map((day) => (
            <div
              key={day.date}
              className="timeline-day-col"
              style={{ height: TOTAL_HOURS * PIXELS_PER_HOUR }}
              onClick={(event) => handleColumnClick(event, day.date)}
            >
              {HOUR_LABELS.map((hour) => (
                <div key={hour} className="timeline-hour-line" style={{ top: hour * PIXELS_PER_HOUR }} />
              ))}

              {day.blocks.map((block) => (
                <div
                  key={block.id}
                  className="timeline-block"
                  style={{
                    top: timeToTop(block.startTime),
                    height: Math.max(20, timeToTop(block.endTime) - timeToTop(block.startTime)),
                  }}
                  onClick={(event) => {
                    event.stopPropagation();
                    onBlockClick(block);
                  }}
                >
                  {block.title || 'Untitled block'}
                  {block.taskTitle && <span className="timeline-block-task">{block.taskTitle}</span>}
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default TimelineGrid;
```

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors. In particular, `useEffect(() => {...}, [])` only touches `scrollRef.current` (a ref, not state) so it doesn't trip `react-hooks/set-state-in-effect`, and refs are exempt from `react-hooks/exhaustive-deps`.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/schedule/TimelineGrid.jsx
git commit -m "feat: add shared TimelineGrid component"
```

---

### Task 5: `TimeBlockModal`

**Files:**
- Create: `frontend/src/components/schedule/TimeBlockModal.jsx`

- [ ] **Step 1: Write the component**

```jsx
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
    (task) => task.status !== 'COMPLETED' && task.status !== 'ARCHIVED',
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
```

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/schedule/TimeBlockModal.jsx
git commit -m "feat: add TimeBlockModal create/edit form"
```

---

### Task 6: `ScheduleSlotModal`

**Files:**
- Create: `frontend/src/components/schedule/ScheduleSlotModal.jsx`

- [ ] **Step 1: Write the component**

```jsx
// frontend/src/components/schedule/ScheduleSlotModal.jsx
import { useState } from 'react';
import { Loader2, X, Trash2 } from 'lucide-react';
import ScheduleSlotService from '../../api/ScheduleSlotService';
import { addMinutesToTime } from '../../utils/schedule';

const DAY_OPTIONS = [
  { value: 'MONDAY', label: '周一' },
  { value: 'TUESDAY', label: '周二' },
  { value: 'WEDNESDAY', label: '周三' },
  { value: 'THURSDAY', label: '周四' },
  { value: 'FRIDAY', label: '周五' },
  { value: 'SATURDAY', label: '周六' },
  { value: 'SUNDAY', label: '周日' },
];

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
      setError(err.message || 'Unable to save template');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete this weekly template?')) return;
    setSubmitting(true);
    setError(null);
    try {
      await ScheduleSlotService.delete(slot.id);
      onSaved();
    } catch (err) {
      setError(err.message || 'Unable to delete template');
      setSubmitting(false);
    }
  };

  return (
    <div role="presentation" style={{ position: 'fixed', inset: 0, zIndex: 1000, display: 'grid', placeItems: 'center', padding: '1.5rem', background: 'var(--overlay)', backdropFilter: 'blur(8px)' }}>
      <div role="dialog" aria-modal="true" aria-labelledby="slot-dialog-title" className="glass fade-in" style={{ width: '100%', maxWidth: '480px', padding: '2rem', borderRadius: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
          <h2 id="slot-dialog-title" style={{ fontSize: '1.3rem', fontWeight: 750 }}>
            {slot ? 'Edit weekly template' : 'New weekly template'}
          </h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close dialog"><X size={19} /></button>
        </div>

        {error && (
          <div role="alert" style={{ padding: '0.8rem 1rem', marginBottom: '1.25rem', borderRadius: '0.8rem', border: '1px solid var(--danger)', color: 'var(--danger)', fontSize: '0.85rem' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1.25rem' }}>
            <label htmlFor="slot-day" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Day of week</label>
            <select
              id="slot-day"
              className="input-field"
              value={form.dayOfWeek}
              onChange={(event) => setForm((current) => ({ ...current, dayOfWeek: event.target.value }))}
            >
              {DAY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.25rem' }}>
            <div style={{ flex: 1 }}>
              <label htmlFor="slot-start" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Start</label>
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
              <label htmlFor="slot-end" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>End</label>
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
            <label htmlFor="slot-title" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Title</label>
            <input
              id="slot-title"
              className="input-field"
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              maxLength={200}
            />
          </div>

          <div style={{ marginBottom: '1.75rem' }}>
            <label htmlFor="slot-description" style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase' }}>Description</label>
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
              {submitting ? <Loader2 className="animate-spin" size={19} /> : (slot ? 'Save changes' : 'Create template')}
            </button>
            {slot && (
              <button type="button" className="icon-button" onClick={handleDelete} disabled={submitting} aria-label="Delete template" title="Delete">
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

export default ScheduleSlotModal;
```

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/schedule/ScheduleSlotModal.jsx
git commit -m "feat: add ScheduleSlotModal create/edit form"
```

---

### Task 7: `TodayView`

**Files:**
- Create: `frontend/src/pages/schedule/TodayView.jsx`

- [ ] **Step 1: Write the component**

```jsx
// frontend/src/pages/schedule/TodayView.jsx
import { useEffect, useState } from 'react';
import { Loader2, AlertCircle } from 'lucide-react';
import TimeBlockService from '../../api/TimeBlockService';
import TaskService from '../../api/TaskService';
import TimelineGrid from '../../components/schedule/TimelineGrid';
import TimeBlockModal from '../../components/schedule/TimeBlockModal';
import { toLocalDateString, sumBlockMinutes, formatDuration } from '../../utils/schedule';

const TodayView = () => {
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
          今天已排 {formatDuration(sumBlockMinutes(blocks))}
        </div>
      </div>

      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '1rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      <TimelineGrid
        days={[{ date: today, label: '今天', blocks: enrichedBlocks }]}
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
```

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors. The data fetch uses the mandatory `.then/.catch` inside the effect with `setState` only in the promise callbacks — same shape as `Tasks.jsx`.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/schedule/TodayView.jsx
git commit -m "feat: add Schedule today timeline view"
```

---

### Task 8: `WeekView`

**Files:**
- Create: `frontend/src/pages/schedule/WeekView.jsx`

- [ ] **Step 1: Write the component**

```jsx
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
```

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/schedule/WeekView.jsx
git commit -m "feat: add Schedule week timeline view"
```

---

### Task 9: `TemplatesView`

**Files:**
- Create: `frontend/src/pages/schedule/TemplatesView.jsx`

- [ ] **Step 1: Write the component**

```jsx
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
```

- [ ] **Step 2: Lint**

Run: `cd frontend && npm run lint`
Expected: no errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/schedule/TemplatesView.jsx
git commit -m "feat: add Schedule weekly templates view"
```

---

### Task 10: `Schedule` container + routing + sidebar

**Files:**
- Create: `frontend/src/pages/Schedule.jsx`
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/components/Sidebar.jsx`

- [ ] **Step 1: Write the container page**

```jsx
// frontend/src/pages/Schedule.jsx
import { useState } from 'react';
import TodayView from './schedule/TodayView';
import WeekView from './schedule/WeekView';
import TemplatesView from './schedule/TemplatesView';

const TABS = [
  { key: 'today', label: '今天' },
  { key: 'week', label: '本周' },
  { key: 'templates', label: '模板' },
];

const Schedule = () => {
  const [activeTab, setActiveTab] = useState('today');

  return (
    <div className="fade-in">
      <div className="page-heading" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: '800', marginBottom: '0.5rem' }}>Schedule</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Plan your day, your week, and your recurring routine.</p>
        </div>
        <div className="segmented-control" role="tablist" aria-label="Schedule view">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              role="tab"
              aria-selected={activeTab === tab.key}
              className={`segmented-btn${activeTab === tab.key ? ' active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {activeTab === 'today' && <TodayView />}
      {activeTab === 'week' && <WeekView />}
      {activeTab === 'templates' && <TemplatesView />}
    </div>
  );
};

export default Schedule;
```

- [ ] **Step 2: Wire the route into `App.jsx`**

Modify `frontend/src/App.jsx`.

old_string:
```jsx
import Tasks from './pages/Tasks';
import Routines from './pages/Routines';
import Analytics from './pages/Analytics';
```

new_string:
```jsx
import Tasks from './pages/Tasks';
import Routines from './pages/Routines';
import Schedule from './pages/Schedule';
import Analytics from './pages/Analytics';
```

old_string:
```jsx
            <Route path="/routines" element={<RequireAuth><SystemLayout><Routines /></SystemLayout></RequireAuth>} />
            <Route path="/analytics" element={<RequireAuth><SystemLayout><Analytics /></SystemLayout></RequireAuth>} />
```

new_string:
```jsx
            <Route path="/routines" element={<RequireAuth><SystemLayout><Routines /></SystemLayout></RequireAuth>} />
            <Route path="/schedule" element={<RequireAuth><SystemLayout><Schedule /></SystemLayout></RequireAuth>} />
            <Route path="/analytics" element={<RequireAuth><SystemLayout><Analytics /></SystemLayout></RequireAuth>} />
```

- [ ] **Step 3: Add the sidebar nav item**

Modify `frontend/src/components/Sidebar.jsx`.

old_string:
```jsx
import { LayoutDashboard, NotebookPen, CheckSquare, Repeat, BarChart3, LogOut, User } from 'lucide-react';
```

new_string:
```jsx
import { LayoutDashboard, NotebookPen, CheckSquare, Repeat, CalendarClock, BarChart3, LogOut, User } from 'lucide-react';
```

old_string:
```jsx
  { label: 'Routines', icon: Repeat, path: '/routines' },
  { label: 'Analytics', icon: BarChart3, path: '/analytics' },
```

new_string:
```jsx
  { label: 'Routines', icon: Repeat, path: '/routines' },
  { label: 'Schedule', icon: CalendarClock, path: '/schedule' },
  { label: 'Analytics', icon: BarChart3, path: '/analytics' },
```

- [ ] **Step 4: Lint and build**

Run: `cd frontend && npm run lint && npm run build`
Expected: both succeed. `build` is the important one here — it's the first point where the whole import graph (App → Schedule → the three views → the shared components → the API clients → utils) actually resolves together, so it catches any path typo the per-file lint steps couldn't see.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/Schedule.jsx frontend/src/App.jsx frontend/src/components/Sidebar.jsx
git commit -m "feat: wire Schedule page into routing and sidebar"
```

---

### Task 11: End-to-end manual verification

No new files — this task confirms the feature actually works against the real backend, per `CLAUDE.md`'s rule that a green `npm run build` isn't sufficient for UI changes.

- [ ] **Step 1: Start Postgres and the backend**

```bash
docker run -d --name ptlt-pg -e POSTGRES_PASSWORD=ptltdev -e POSTGRES_DB=ptlt -p 55432:5432 postgres:16
```

Then start the Spring Boot app with `JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD` set (see `CLAUDE.md`'s "命令" section for the required env vars), pointed at `localhost:55432`.

- [ ] **Step 2: Start the frontend dev server**

```bash
cd frontend && npm run dev
```

- [ ] **Step 3: Click through the today view**

Log in, open **Schedule** in the sidebar. On the **今天** tab: click an empty area of the timeline, confirm the create modal opens pre-filled with a snapped time and a 1-hour default span; create a block; confirm it renders at the right vertical position; click it, edit the title, save; confirm the header's "今天已排 …" total updates; delete it via the trash icon in the edit modal and confirm it disappears and the total goes back down.

- [ ] **Step 4: Click through the week view**

Switch to **本周**. Confirm the block you create here shows up in the correct day column. Click "previous week" / "next week" and confirm the date range and total both update. Create a block linked to an existing task via the "Linked task" dropdown and confirm the task's title renders read-only under the block's title.

- [ ] **Step 5: Click through templates**

Switch to **模板**. Create a weekly template for a day of week, confirm it appears grouped under the right day, and confirm it does **not** show up as a real block in the 今天/本周 views (this is the boundary the design explicitly draws — no auto-apply). Edit and delete it.

- [ ] **Step 6: Confirm this didn't touch the backend**

```bash
git status --short
```

Expected: no changes under `src/main/java` or `src/main/resources/db/migration` — this feature is frontend-only.

No commit for this task (nothing new to add).
