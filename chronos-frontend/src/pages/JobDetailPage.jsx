import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { getJob, getJobLogs, pauseJob, resumeJob, triggerJob, deleteJob } from '../api/jobs';
import JobStatusBadge from '../components/JobStatusBadge';

const fmt = dt => dt ? format(new Date(dt), 'dd MMM yyyy, HH:mm:ss') : '—';

function InfoRow({ label, children }) {
  return (
    <div className="info-row">
      <span className="info-label">{label}</span>
      <span className="info-value">{children}</span>
    </div>
  );
}

export default function JobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const [jobData, logsData] = await Promise.all([getJob(id), getJobLogs(id)]);
        setJob(jobData); setLogs(logsData.logs ?? []);
      } catch { setError('Job not found or access denied.'); }
      finally { setLoading(false); }
    };
    load();
  }, [id]);

  const handleAction = async (action) => {
    setActionLoading(true);
    try {
      if (action === 'delete') {
        if (!window.confirm('Delete this job permanently?')) return;
        await deleteJob(id); navigate('/dashboard'); return;
      }
      let updated;
      if (action === 'pause')   updated = await pauseJob(id);
      if (action === 'resume')  updated = await resumeJob(id);
      if (action === 'trigger') { await triggerJob(id); updated = await getJob(id); }
      setJob(updated);
    } catch (err) { alert(err.response?.data?.error ?? 'Action failed.'); }
    finally { setActionLoading(false); }
  };

  if (loading) return <div className="spinner-page"><div className="spinner" /></div>;

  if (error) return (
    <div className="page-wrap" style={{ paddingTop: 32 }}>
      <div className="alert alert-error">{error}</div>
      <button className="back-link" style={{ marginTop: 12 }} onClick={() => navigate('/dashboard')}>← Back</button>
    </div>
  );

  const logStatusClass = s => ({ SUCCESS: 'log-SUCCESS', FAILED: 'log-FAILED', RUNNING: 'log-RUNNING' }[s] ?? '');

  return (
    <div className="page-wrap" style={{ paddingTop: 32, paddingBottom: 48 }}>
      <button className="back-link" onClick={() => navigate('/dashboard')}>← Back to jobs</button>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, marginBottom: 24, flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 600, letterSpacing: '-0.025em' }}>{job.name}</h1>
          {job.description && <p style={{ fontSize: 13, color: 'var(--text-2)', marginTop: 4 }}>{job.description}</p>}
          <div style={{ marginTop: 8 }}><JobStatusBadge status={job.status} /></div>
        </div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button disabled={actionLoading} onClick={() => handleAction('trigger')} className="btn btn-primary btn-sm">▶ Run now</button>
          {job.status === 'PAUSED'
            ? <button disabled={actionLoading} onClick={() => handleAction('resume')} className="btn btn-sm btn-success-ghost">Resume</button>
            : <button disabled={actionLoading || job.status === 'COMPLETED' || job.status === 'FAILED'} onClick={() => handleAction('pause')} className="btn btn-sm btn-warn-ghost">Pause</button>
          }
          <button disabled={actionLoading} onClick={() => handleAction('delete')} className="btn btn-sm btn-danger-ghost">Delete</button>
        </div>
      </div>

      {/* Details card */}
      <div className="card" style={{ marginBottom: 20 }}>
        <div className="card-body">
          <div className="section-label">Job details</div>
          <div className="info-rows">
            <InfoRow label="ID"><code className="info-mono">{job.id}</code></InfoRow>
            <InfoRow label="Type">{job.type}</InfoRow>
            <InfoRow label="Timezone">{job.timezone}</InfoRow>
            <InfoRow label="Retries">{job.retryCount} of {job.maxRetries} used</InfoRow>
            {job.type === 'ONE_TIME' && <InfoRow label="Scheduled at">{fmt(job.scheduledAt)}</InfoRow>}
            {job.type === 'RECURRING' && <InfoRow label="Cron"><code className="info-mono">{job.cronExpression}</code></InfoRow>}
            <InfoRow label="Created">{fmt(job.createdAt)}</InfoRow>
            <InfoRow label="Updated">{fmt(job.updatedAt)}</InfoRow>
          </div>
          {job.payload && Object.keys(job.payload).length > 0 && (
            <div style={{ marginTop: 16 }}>
              <div className="section-label">Payload</div>
              <pre className="payload-pre">{JSON.stringify(job.payload, null, 2)}</pre>
            </div>
          )}
        </div>
      </div>

      {/* Logs card */}
      <div className="card" style={{ overflow: 'hidden' }}>
        <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontWeight: 500, fontSize: 13 }}>Execution logs</span>
          <span style={{ fontSize: 12, color: 'var(--text-3)' }}>{logs.length} run{logs.length !== 1 ? 's' : ''}</span>
        </div>
        {logs.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 24px', color: 'var(--text-3)', fontSize: 13 }}>No executions yet</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Status</th>
                  <th>Started</th>
                  <th>Duration</th>
                  <th>Error</th>
                </tr>
              </thead>
              <tbody>
                {logs.map(log => (
                  <tr key={log.id}>
                    <td className="td-muted">#{log.attempt}</td>
                    <td><span className={`log-badge ${logStatusClass(log.status)}`}>{log.status}</span></td>
                    <td className="td-muted">{fmt(log.startedAt)}</td>
                    <td className="td-muted">{log.durationMs ? `${log.durationMs}ms` : '—'}</td>
                    <td style={{ color: 'var(--red)', fontSize: 12, maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {log.errorMessage || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
