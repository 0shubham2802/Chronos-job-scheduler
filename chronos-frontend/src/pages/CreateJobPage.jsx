import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { createJob } from '../api/jobs';

const TIMEZONES = ['UTC','Asia/Kolkata','Asia/Mumbai','America/New_York','America/Chicago','America/Los_Angeles','Europe/London','Europe/Paris','Asia/Tokyo','Asia/Singapore','Australia/Sydney'];
const CRON_EXAMPLES = [
  { label: 'Every minute', value: '* * * * *' },
  { label: 'Every hour',   value: '0 * * * *' },
  { label: 'Daily 9am',    value: '0 9 * * *' },
  { label: 'Mon 9am',      value: '0 9 * * MON' },
  { label: 'Sun midnight', value: '0 0 * * SUN' },
];

export default function CreateJobPage() {
  const navigate = useNavigate();
  const [jobType, setJobType] = useState('ONE_TIME');
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState('');
  const [payloadError, setPayloadError] = useState('');
  const { register, handleSubmit, setValue, formState: { errors } } = useForm({
    defaultValues: { timezone: 'Asia/Kolkata', maxRetries: 3 },
  });

  const onSubmit = async (formData) => {
    setServerError(''); setPayloadError('');

    // Parse payload JSON if provided
    let payload = null;
    if (formData.payload?.trim()) {
      try { payload = JSON.parse(formData.payload); }
      catch { setPayloadError('Invalid JSON — check your payload'); return; }
    }

    const jobData = {
      name: formData.name,
      description: formData.description || null,
      type: jobType,                          // "ONE_TIME" or "RECURRING" exactly
      timezone: formData.timezone,
      maxRetries: parseInt(formData.maxRetries, 10),
      payload: payload,
    };

    // scheduledAt must be "2026-05-09T14:30:00" — LocalDateTime format
    // The browser gives "2026-05-09T14:30" (no seconds) — we append :00
    if (jobType === 'ONE_TIME') {
      if (!formData.scheduledAt) {
        setServerError('Please pick a scheduled date and time.');
        return;
      }
      // Ensure seconds are included: "2026-05-09T14:30" → "2026-05-09T14:30:00"
      jobData.scheduledAt = formData.scheduledAt.length === 16
        ? formData.scheduledAt + ':00'
        : formData.scheduledAt;
    } else {
      if (!formData.cronExpression?.trim()) {
        setServerError('Please enter a cron expression.');
        return;
      }
      jobData.cronExpression = formData.cronExpression.trim();
    }

    console.log('Sending job:', jobData); // helpful for debugging

    setLoading(true);
    try {
      await createJob(jobData);
      navigate('/dashboard');
    } catch (err) {
      const msg = err.response?.data?.error
        ?? err.response?.data?.message
        ?? JSON.stringify(err.response?.data)
        ?? 'Failed to create job. Check console for details.';
      setServerError(msg);
      console.error('Create job error:', err.response?.data);
    } finally { setLoading(false); }
  };

  return (
    <div className="page-wrap" style={{ paddingTop: 32, paddingBottom: 48, maxWidth: 640 }}>
      <button className="back-link" onClick={() => navigate('/dashboard')}>← Back</button>

      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 20, fontWeight: 600, letterSpacing: '-0.025em' }}>Create new job</h1>
        <p style={{ fontSize: 13, color: 'var(--text-2)', marginTop: 4 }}>Schedule a one-time or recurring task</p>
      </div>

      <div className="card">
        <div className="card-body">
          <form onSubmit={handleSubmit(onSubmit)} className="stack gap-20">

            {/* Job type toggle */}
            <div className="form-group">
              <label className="form-label">Job type</label>
              <div className="type-toggle">
                {['ONE_TIME', 'RECURRING'].map(t => (
                  <button key={t} type="button" onClick={() => setJobType(t)}
                    className={`type-toggle-btn${jobType === t ? ' active' : ''}`}>
                    {t === 'ONE_TIME' ? '→ One-time' : '↻ Recurring'}
                  </button>
                ))}
              </div>
            </div>

            {/* Name */}
            <div className="form-group">
              <label className="form-label">Job name <span style={{ color: 'var(--red)' }}>*</span></label>
              <input
                type="text"
                className={`form-input${errors.name ? ' is-error' : ''}`}
                placeholder="e.g. Send weekly report"
                {...register('name', { required: 'Job name is required' })}
              />
              {errors.name && <span className="form-error">{errors.name.message}</span>}
            </div>

            {/* Description */}
            <div className="form-group">
              <label className="form-label">Description <span style={{ color: 'var(--text-3)' }}>(optional)</span></label>
              <textarea rows={2} className="form-input form-textarea"
                placeholder="What does this job do?" {...register('description')} />
            </div>

            {/* Schedule — ONE_TIME or RECURRING */}
            {jobType === 'ONE_TIME' ? (
              <div className="form-group">
                <label className="form-label">Run at <span style={{ color: 'var(--red)' }}>*</span></label>
                <input
                  type="datetime-local"
                  className={`form-input${errors.scheduledAt ? ' is-error' : ''}`}
                  {...register('scheduledAt', { required: 'Please pick a date and time' })}
                />
                {errors.scheduledAt && <span className="form-error">{errors.scheduledAt.message}</span>}
                <span className="form-hint">Time is interpreted in the timezone selected below</span>
              </div>
            ) : (
              <div className="form-group">
                <label className="form-label">Cron expression <span style={{ color: 'var(--red)' }}>*</span></label>
                <input
                  type="text"
                  className={`form-input form-mono${errors.cronExpression ? ' is-error' : ''}`}
                  placeholder="0 9 * * MON"
                  {...register('cronExpression')}
                />
                <div className="cron-pills">
                  {CRON_EXAMPLES.map(ex => (
                    <button key={ex.value} type="button" className="cron-pill"
                      title={ex.label} onClick={() => setValue('cronExpression', ex.value)}>
                      {ex.value}
                    </button>
                  ))}
                </div>
                <span className="form-hint">Format: minute · hour · day · month · weekday</span>
              </div>
            )}

            {/* Timezone + Max retries side by side */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 100px', gap: 12 }}>
              <div className="form-group">
                <label className="form-label">Timezone</label>
                <select className="form-input form-select" {...register('timezone')}>
                  {TIMEZONES.map(tz => <option key={tz} value={tz}>{tz}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Max retries</label>
                <input type="number" min={0} max={10} className="form-input" {...register('maxRetries')} />
              </div>
            </div>

            {/* Payload */}
            <div className="form-group">
              <label className="form-label">Payload <span style={{ color: 'var(--text-3)' }}>(optional JSON)</span></label>
              <textarea rows={4} className={`form-input form-textarea form-mono${payloadError ? ' is-error' : ''}`}
                placeholder={'{\n  "url": "https://api.example.com",\n  "fail": false\n}'}
                {...register('payload')} />
              {payloadError && <span className="form-error">{payloadError}</span>}
              <span className="form-hint">Tip: set <code style={{ fontFamily: 'monospace' }}>"fail": true</code> to test retry behaviour</span>
            </div>

            {serverError && <div className="alert alert-error">{serverError}</div>}

            <div style={{ display: 'flex', gap: 10, paddingTop: 4 }}>
              <button type="button" onClick={() => navigate('/dashboard')} className="btn btn-secondary btn-full btn-lg">Cancel</button>
              <button type="submit" disabled={loading} className="btn btn-primary btn-full btn-lg">
                {loading ? 'Creating…' : 'Create job'}
              </button>
            </div>

          </form>
        </div>
      </div>
    </div>
  );
}
