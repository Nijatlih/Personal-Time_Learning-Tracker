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
