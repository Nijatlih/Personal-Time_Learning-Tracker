import client from './client';

const RoutineService = {
  // 获取例行事项定义列表
  listDefinitions: () => client.get('/routines/definitions'),
  
  // 创建例行事项定义
  createDefinition: (data) => client.post('/routines/definitions', data),
  
  // 记录一次例行事项完成
  recordCompletion: (data) => client.post('/routines/completions', data),
  
  // 更新例行事项定义
  updateDefinition: (id, data) => client.put(`/routines/definitions/${id}`, data)
};

export default RoutineService;
