import { useState } from 'react';
import TodayView from './schedule/TodayView';
import WeekView from './schedule/WeekView';
import TemplatesView from './schedule/TemplatesView';
import { useLanguage } from '../hooks/useLanguage';

const TABS = [
  { key: 'today', labelKey: 'schedule.tab.today' },
  { key: 'week', labelKey: 'schedule.tab.week' },
  { key: 'templates', labelKey: 'schedule.tab.templates' },
];

const Schedule = () => {
  const { t } = useLanguage();
  const [activeTab, setActiveTab] = useState('today');

  return (
    <div className="fade-in">
      <div className="page-heading" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: '800', marginBottom: '0.5rem' }}>{t('schedule.title')}</h1>
          <p style={{ color: 'var(--text-secondary)' }}>{t('schedule.subtitle')}</p>
        </div>
        <div className="segmented-control" role="tablist" aria-label={t('schedule.viewLabel')}>
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              role="tab"
              aria-selected={activeTab === tab.key}
              className={`segmented-btn${activeTab === tab.key ? ' active' : ''}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {t(tab.labelKey)}
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
