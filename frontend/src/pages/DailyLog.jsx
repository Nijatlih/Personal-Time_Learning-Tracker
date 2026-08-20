import { useEffect, useState } from 'react';
import DailyRecordService from '../api/DailyRecordService';
import {
  AlertCircle, CalendarDays, CheckCircle2, Flame, Loader2, Moon, Save, Sun, Target, Zap,
} from 'lucide-react';

/** 取本地时区的 YYYY-MM-DD；toISOString() 先转 UTC，跨时区会差一天。 */
const toLocalDateString = (d) => {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

const STATUS_COLOR = {
  GREEN: 'var(--success)',
  YELLOW: 'var(--warning)',
  GRAY: 'var(--text-secondary)',
};

const ENERGY_OPTIONS = [
  { value: 'LOW', label: 'Low', points: 0 },
  { value: 'MEDIUM', label: 'Medium', points: 10 },
  { value: 'HIGH', label: 'High', points: 15 },
];

const EMPTY_FORM = { coreTaskCompleted: false, cognitiveEnergy: 'MEDIUM', alignedWithGoals: false };

/** 一个只读的派生指标卡。 */
const Metric = ({ icon: Icon, label, value, color }) => (
  <div className="glass" style={{ padding: '1.25rem', borderRadius: '1.25rem' }}>
    <div style={{
      display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem',
      color: 'var(--text-secondary)', fontSize: '0.75rem', fontWeight: '600',
      textTransform: 'uppercase', letterSpacing: '0.03em',
    }}>
      <Icon size={14} /> {label}
    </div>
    <div style={{ fontSize: '1.75rem', fontWeight: '700', color: color || 'var(--text-primary)' }}>
      {value}
    </div>
  </div>
);

/**
 * Daily Log —— DailyRecord 的录入页。
 *
 * 这一页此前不存在，而稳定度分数、streak、Dashboard 图表和整个 Analytics 页全部读
 * DailyRecord：也就是说这个应用的核心数据没有任何入口可以产生，四个后端端点
 * （DailyRecordController）一直是就绪但无人调用的状态。
 *
 * 两点设计上的取舍：
 * - 日期可选而不是固定今天。补录是真实需求，而且后端的 streak 与 Gray Alert 现在都按
 *   记录自身的日期锚定，补录能得到正确结果。
 * - morning/evening routine 两项是**只读**展示。它们由后端从 routine_completions 派生
 *   （见 StabilityService），不是用户能直接填的东西 —— 给一个假的输入框会让人以为
 *   在这里勾选有用。要拿这 40 分得去 Routines 页打卡。
 */
const DailyLog = () => {
  const [date, setDate] = useState(() => toLocalDateString(new Date()));
  const [form, setForm] = useState(EMPTY_FORM);
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  const applyRecord = (data) => {
    setRecord(data);
    setForm({
      coreTaskCompleted: data.coreTaskCompleted ?? false,
      cognitiveEnergy: data.cognitiveEnergy ?? 'MEDIUM',
      alignedWithGoals: data.alignedWithGoals ?? false,
    });
  };

  // 取数写成 .then/.catch 而不是在 effect 里调一个 async 函数，是为了满足
  // react-hooks/set-state-in-effect：该规则会静态地标记 effect 体里任何（间接）
  // 触发 setState 的调用，与 await 出现在哪一句无关。setState 放在 promise 回调里
  // 才是它认可的形态。Analytics.jsx 用的是同一个写法，也是本仓库里唯一不被这条规则
  // 标记的页面 —— 另外 4 处告警都是 useEffect(() => { fetchX(); }, [fetchX]) 的形状。
  //
  // cancelled 标志解决的是一个真实的竞态：快速切换日期时，先发出的请求可能后返回，
  // 把已经不是当前选中日期的数据写进表单。其余页面没有这个保护，但它们也没有
  // 会变的查询参数。
  useEffect(() => {
    let cancelled = false;

    DailyRecordService.getByDate(date)
      .then((data) => {
        if (cancelled) return;
        applyRecord(data);
        setError(null);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err.status === 404) {
          // 这一天还没录 —— 是正常状态，不是错误
          setRecord(null);
          setForm(EMPTY_FORM);
          setError(null);
        } else {
          setError(err.message);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [date]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setNotice(null);
    try {
      const data = await DailyRecordService.save({ date, ...form });
      applyRecord(data);
      setError(null);
      setNotice(`Saved. Stability for ${date}: ${data.stabilityScore} (${data.stabilityStatus}).`);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const energyPoints =
    ENERGY_OPTIONS.find((o) => o.value === form.cognitiveEnergy)?.points ?? 0;
  // 与后端 StabilityService 的规则一致：core 30 / energy 0-15 / aligned 15 /
  // morning 20 / evening 20。这里只是给用户一个即时预估，落库的分数由后端计算。
  const projected =
    (form.coreTaskCompleted ? 30 : 0) + energyPoints + (form.alignedWithGoals ? 15 : 0) +
    (record?.morningRoutineCompleted ? 20 : 0) + (record?.eveningRoutineCompleted ? 20 : 0);

  const labelStyle = {
    display: 'block', fontSize: '0.75rem', fontWeight: '700',
    color: 'var(--text-secondary)', textTransform: 'uppercase',
    letterSpacing: '0.03em', marginBottom: '0.75rem',
  };

  const toggleRow = (checked, onChange, id, title, subtitle, points) => (
    <label
      htmlFor={id}
      style={{
        display: 'flex', alignItems: 'center', gap: '1rem', cursor: 'pointer',
        padding: '1rem 1.25rem', borderRadius: '1rem',
        border: `1px solid ${checked ? 'var(--accent-primary)' : 'var(--border-color)'}`,
        backgroundColor: checked ? 'var(--accent-soft)' : 'transparent',
        transition: 'all 0.2s',
      }}
    >
      <input
        type="checkbox"
        id={id}
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        style={{ width: '20px', height: '20px', accentColor: 'var(--accent-primary)' }}
      />
      <div style={{ flex: 1 }}>
        <div style={{ fontWeight: '600', fontSize: '0.9375rem' }}>{title}</div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{subtitle}</div>
      </div>
      <div style={{
        fontSize: '0.75rem', fontWeight: '700',
        color: checked ? 'var(--accent-primary)' : 'var(--text-secondary)',
      }}>
        +{points}
      </div>
    </label>
  );

  return (
    <div className="fade-in">
      <header style={{ marginBottom: '2.5rem' }}>
        <h1 style={{ fontSize: '2rem', fontWeight: '800', marginBottom: '0.5rem' }}>Daily Log</h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          Record the day. Everything on the Dashboard and Analytics pages is derived from these entries.
        </p>
      </header>

      {error && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--danger)', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <AlertCircle size={20} /> {error}
        </div>
      )}

      {notice && (
        <div className="glass" style={{ padding: '1rem', marginBottom: '2rem', borderColor: 'var(--success)', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <CheckCircle2 size={20} /> {notice}
        </div>
      )}

      <div className="two-column-grid" style={{ display: 'grid', gap: '2rem', alignItems: 'start' }}>
        <form onSubmit={handleSubmit} className="glass" style={{ padding: '2rem', borderRadius: '1.5rem' }}>
          <div style={{ marginBottom: '2rem' }}>
            <label htmlFor="log-date" style={labelStyle}>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                <CalendarDays size={14} /> Date
              </span>
            </label>
            <input
              type="date"
              id="log-date"
              className="input-field"
              value={date}
              max={toLocalDateString(new Date())}
              onChange={(e) => {
                if (!e.target.value) return;
                setNotice(null);
                setDate(e.target.value);
              }}
            />
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
              {loading
                ? 'Loading…'
                : record
                  ? 'An entry already exists for this date — saving will update it.'
                  : 'No entry for this date yet.'}
            </div>
          </div>

          <div style={{ marginBottom: '2rem' }}>
            <span style={labelStyle}>Cognitive Energy</span>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              {ENERGY_OPTIONS.map((option) => {
                const selected = form.cognitiveEnergy === option.value;
                return (
                  <button
                    type="button"
                    key={option.value}
                    onClick={() => setForm((f) => ({ ...f, cognitiveEnergy: option.value }))}
                    style={{
                      flex: 1, padding: '0.875rem 0.5rem', borderRadius: '1rem', cursor: 'pointer',
                      border: `1px solid ${selected ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                      backgroundColor: selected ? 'var(--accent-soft)' : 'transparent',
                      color: selected ? 'var(--accent-primary)' : 'var(--text-secondary)',
                      fontWeight: selected ? '700' : '500', fontSize: '0.875rem',
                      transition: 'all 0.2s',
                    }}
                  >
                    {option.label}
                    <div style={{ fontSize: '0.7rem', opacity: 0.8, marginTop: '0.2rem' }}>
                      +{option.points}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          <div style={{ display: 'grid', gap: '0.75rem', marginBottom: '2rem' }}>
            {toggleRow(
              form.coreTaskCompleted,
              (v) => setForm((f) => ({ ...f, coreTaskCompleted: v })),
              'core-task', 'Core task completed', 'The one thing that had to happen today', 30,
            )}
            {toggleRow(
              form.alignedWithGoals,
              (v) => setForm((f) => ({ ...f, alignedWithGoals: v })),
              'aligned', 'Aligned with goals', 'The day moved the long-term direction forward', 15,
            )}
          </div>

          <button type="submit" className="primary-btn" style={{ width: '100%' }} disabled={saving || loading}>
            {saving ? <Loader2 className="animate-spin" size={20} /> : (<><Save size={18} /> Save Entry</>)}
          </button>
        </form>

        <div style={{ display: 'grid', gap: '1rem' }}>
          <Metric
            icon={Zap}
            label={record ? 'Stability (saved)' : 'Stability (projected)'}
            value={`${record ? record.stabilityScore : projected} / 100`}
            color={record ? STATUS_COLOR[record.stabilityStatus] : 'var(--accent-primary)'}
          />

          {record && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <Metric icon={Flame} label="Green streak" value={`${record.greenStreak ?? 0}d`} />
              <Metric icon={Target} label="Uptime streak" value={`${record.uptimeStreak ?? 0}d`} />
            </div>
          )}

          {/*
            只读。这两项由后端从 routine_completions 派生，不是这一页能填的东西 ——
            要拿这 40 分得去 Routines 页打卡。给个假输入框会误导人。
          */}
          <div className="glass" style={{ padding: '1.5rem', borderRadius: '1.25rem' }}>
            <div style={{ fontSize: '0.75rem', fontWeight: '700', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.03em', marginBottom: '1rem' }}>
              Routines (derived)
            </div>
            {[
              { icon: Sun, label: 'Morning', done: record?.morningRoutineCompleted },
              { icon: Moon, label: 'Evening', done: record?.eveningRoutineCompleted },
            ].map(({ icon: Icon, label, done }) => (
              <div key={label} style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem', fontSize: '0.875rem' }}>
                <Icon size={16} style={{ color: done ? 'var(--success)' : 'var(--text-secondary)' }} />
                <span style={{ flex: 1 }}>{label}</span>
                <span style={{ fontWeight: '700', color: done ? 'var(--success)' : 'var(--text-secondary)' }}>
                  {done ? '+20' : '—'}
                </span>
              </div>
            ))}
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.75rem', lineHeight: 1.5 }}>
              Computed from routine check-ins, not editable here. Check in on the Routines page to earn these 40 points.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DailyLog;
