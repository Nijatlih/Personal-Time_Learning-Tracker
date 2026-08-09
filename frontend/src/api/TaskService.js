import client from './client';

const TaskService = {
  // 获取任务列表，支持状态过滤
  list: (status) => client.get('/tasks', { params: { status } }),
  
  // 创建新任务
  create: (taskData) => client.post('/tasks', taskData),
  
  // 完成任务
  complete: (id) => client.post(`/tasks/${id}/complete`),
  
  // 归档任务
  archive: (id) => client.post(`/tasks/${id}/archive`),
  
  // 更新任务
  update: (id, taskData) => client.put(`/tasks/${id}`, taskData)
};

export default TaskService;
