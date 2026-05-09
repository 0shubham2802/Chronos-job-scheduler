import api from './axios';

export const getJobs = async (cursor = null, limit = 20) => {
  const params = { limit };
  if (cursor) params.cursor = cursor;
  const res = await api.get('/api/jobs', { params });
  return res.data; // { data: [...], nextCursor, hasMore }
};

export const getJob = async (id) => {
  const res = await api.get(`/api/jobs/${id}`);
  return res.data;
};

export const createJob = async (jobData) => {
  const res = await api.post('/api/jobs', jobData);
  return res.data;
};

export const updateJob = async (id, jobData) => {
  const res = await api.patch(`/api/jobs/${id}`, jobData);
  return res.data;
};

export const deleteJob = async (id) => {
  await api.delete(`/api/jobs/${id}`);
};

export const pauseJob = async (id) => {
  const res = await api.post(`/api/jobs/${id}/pause`);
  return res.data;
};

export const resumeJob = async (id) => {
  const res = await api.post(`/api/jobs/${id}/resume`);
  return res.data;
};

export const triggerJob = async (id) => {
  const res = await api.post(`/api/jobs/${id}/trigger`);
  return res.data;
};

export const getJobLogs = async (id, limit = 20) => {
  const res = await api.get(`/api/jobs/${id}/logs`, { params: { limit } });
  return res.data; // { jobId, logs: [...], count }
};
