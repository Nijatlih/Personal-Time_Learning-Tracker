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
