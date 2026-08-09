import client from './client';

const DashboardService = {
  getDashboard: () => client.get('/dashboard'),
};

export default DashboardService;
