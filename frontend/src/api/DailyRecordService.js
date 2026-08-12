import client from './client';

const DailyRecordService = {
  /**
   * 创建或更新某一天的记录。
   *
   * 后端这个端点是 createOrUpdate（按 workspace + date 唯一），所以同一个调用既能新建
   * 也能改写，请求体里**不需要** version —— 强制乐观锁只作用于 6 个 PUT 端点。
   */
  save: (record) => client.post('/daily-records', record),

  /** 取某一天的记录。不存在时后端返回 404，调用方需要自己区分"没录"和"出错"。 */
  getByDate: (date) => client.get('/daily-records/by-date', { params: { date } }),

  getRange: (startDate, endDate) =>
    client.get('/daily-records/range', { params: { startDate, endDate } }),
};

export default DailyRecordService;
