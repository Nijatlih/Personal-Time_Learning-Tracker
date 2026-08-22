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
