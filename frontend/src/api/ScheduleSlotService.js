// frontend/src/api/ScheduleSlotService.js
import client from './client';

const ScheduleSlotService = {
  list: () => client.get('/schedule-slots'),
  create: (data) => client.post('/schedule-slots', data),
  update: (id, data) => client.put(`/schedule-slots/${id}`, data),
  delete: (id) => client.delete(`/schedule-slots/${id}`),
};

export default ScheduleSlotService;
