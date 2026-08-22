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
