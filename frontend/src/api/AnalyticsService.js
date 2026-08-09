import client from './client';

const AnalyticsService = {
  getStabilitySummary: (startDate, endDate) => 
    client.get('/analytics/stability/summary', { params: { startDate, endDate } }),
  
  getTaskStats: (startDate, endDate) => 
    client.get('/analytics/tasks/completion-stats', { params: { startDate, endDate } }),
  
  getRoutineStats: (startDate, endDate) => 
    client.get('/analytics/routines/completion-stats', { params: { startDate, endDate } }),
};

export default AnalyticsService;
