import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import { getJobs, deleteJob, pauseJob, resumeJob, triggerJob } from '../api/jobs';
import JobStatusBadge from '../components/JobStatusBadge';

export default function DashboardPage() {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(null);

  const fetchJobs = useCallback(async () => {
    try {
      const data = await getJobs();
      console.log('Jobs API response:', data); // debug
      // Backend returns PagedResponse with field "items" (not "data")
      if (Array.isArray(data)) {
        setJobs(data);
      } else if (data.items) {
        setJobs(data.items);
      } else if (data.data) {
        setJobs(data.data);
      } else if (data.content) {
        setJobs(data.content);
      } else {
        setJobs([]);
      }
    } catch (err) {
      console.error('Fetch jobs error:', err.response?.data ?? err.message);
      setError('Failed to load jobs. Is your backend running on port 8080?');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchJobs(); }, [fetchJobs]);

  const handleAction = async (e, id, action) => {
    e.stopPropagation();
    setActionLoading(id);
    try {
      if (action === 'delete') {
        if (!window.confirm('Delete this job permanently?')) return;
        await deleteJob(id);
        setJobs(prev => prev.filter(j => j.id !== id));
      } else if (action === 'pause') {
        const u = await pauseJob(id);
        setJobs(prev => prev.map(j => j.id === id ? { ...j, status: u.status } : j));
      } else if (action === 'resume') {
        const u = await resumeJob(id);
        setJobs(prev => prev.map(j => j.id === id ? { ...j, status: u.status } : j));
      } else if (action === 'trigger') {
        await triggerJob(id);
        await fetchJobs();
      }
    } catch (err) {
      const msg = err.response?.data?.error ?? err.response?.data?.message ?? 'Action failed.';
      alert(msg);
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) return (
    <div className="spinner-page"><div className="spinner" /></div>
  );

  return (
    <div className="page-wrap" style={{ paddingTop: 32, paddingBottom: 48 }}>
      <div className="page-header">
        <div>
          <h1>Jobs</h1>
          <p>{jobs.length} job{jobs.length !== 1 ? 's' : ''}</p>
        </div>
        <Link to="/jobs/new" className="btn btn-primary">+ New Job</Link>
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: 16 }}>{error}</div>}

      {jobs.length === 0 && !error ? (
        <div className="empty-state">
          <div className="empty-state-icon">📭</div>
          <h3>No jobs yet</h3>
          <p>Create your first scheduled job to get started</p>
          <Link to="/jobs/new" className="btn btn-primary">Create a job</Link>
        </div>
      ) : (
        <div className="card" style={{ overflow: 'hidden' }}>
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map(job => (
                  <tr key={job.id} className="clickable" onClick={() => navigate(`/jobs/${job.id}`)}>
                    <td>
                      <div style={{ fontWeight: 500 }}>{job.name}</div>
                      {job.description && (
                        <div className="td-muted" style={{ marginTop: 2, maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {job.description}
                        </div>
                      )}
                    </td>
                    <td>
                      {job.type === 'RECURRING'
                        ? <span className="chip chip-purple">↻ Recurring</span>
                        : <span className="chip chip-gray">→ One-time</span>}
                    </td>
                    <td><JobStatusBadge status={job.status} /></td>
                    <td className="td-muted">
                      {job.createdAt ? formatDistanceToNow(new Date(job.createdAt), { addSuffix: true }) : '—'}
                    </td>
                    <td className="td-actions">
                      <div className="action-group">
                        <button
                          disabled={actionLoading === job.id}
                          onClick={e => handleAction(e, job.id, 'trigger')}
                          className="btn btn-sm btn-secondary"
                        >▶ Run</button>

                        {job.status === 'PAUSED' ? (
                          <button disabled={actionLoading === job.id} onClick={e => handleAction(e, job.id, 'resume')} className="btn btn-sm btn-success-ghost">Resume</button>
                        ) : (
                          <button
                            disabled={actionLoading === job.id || job.status === 'COMPLETED' || job.status === 'FAILED' || job.type === 'ONE_TIME'}
                            onClick={e => handleAction(e, job.id, 'pause')}
                            className="btn btn-sm btn-warn-ghost"
                            title={job.type === 'ONE_TIME' ? 'Only recurring jobs can be paused' : ''}
                          >Pause</button>
                        )}

                        <button disabled={actionLoading === job.id} onClick={e => handleAction(e, job.id, 'delete')} className="btn btn-sm btn-danger-ghost">Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
