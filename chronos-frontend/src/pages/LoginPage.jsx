import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { loginUser } from '../api/auth';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async ({ email, password }) => {
    setServerError(''); setLoading(true);
    try {
      const data = await loginUser(email, password);
      login(data.token, data.name, data.email);
      navigate('/dashboard');
    } catch (err) {
      setServerError(err.response?.data?.error ?? 'Incorrect email or password.');
    } finally { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-wrap">
        <div className="auth-logo">
          <div className="auth-logo-icon">C</div>
          <h1>Sign in to Chronos</h1>
          <p>Job scheduling made simple</p>
        </div>

        <div className="card">
          <div className="card-body">
            <form onSubmit={handleSubmit(onSubmit)} className="stack gap-16">
              <div className="form-group">
                <label className="form-label">Email</label>
                <input
                  type="email"
                  className={`form-input${errors.email ? ' is-error' : ''}`}
                  placeholder="you@example.com"
                  {...register('email', { required: 'Email is required' })}
                />
                {errors.email && <span className="form-error">{errors.email.message}</span>}
              </div>

              <div className="form-group">
                <label className="form-label">Password</label>
                <input
                  type="password"
                  className={`form-input${errors.password ? ' is-error' : ''}`}
                  placeholder="••••••••"
                  {...register('password', { required: 'Password is required' })}
                />
                {errors.password && <span className="form-error">{errors.password.message}</span>}
              </div>

              {serverError && <div className="alert alert-error">{serverError}</div>}

              <button type="submit" disabled={loading} className="btn btn-primary btn-full btn-lg">
                {loading ? 'Signing in…' : 'Sign in'}
              </button>
            </form>
          </div>
        </div>

        <p className="auth-footer">
          No account? <Link to="/register">Create one</Link>
        </p>
      </div>
    </div>
  );
}
