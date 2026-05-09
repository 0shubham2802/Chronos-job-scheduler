import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { registerUser } from '../api/auth';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async ({ name, email, password }) => {
    setServerError(''); setLoading(true);
    try {
      const data = await registerUser(name, email, password);
      login(data.token, data.name, data.email);
      navigate('/dashboard');
    } catch (err) {
      setServerError(err.response?.data?.error ?? 'Registration failed. Please try again.');
    } finally { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-wrap">
        <div className="auth-logo">
          <div className="auth-logo-icon">C</div>
          <h1>Create your account</h1>
          <p>Start scheduling jobs in seconds</p>
        </div>

        <div className="card">
          <div className="card-body">
            <form onSubmit={handleSubmit(onSubmit)} className="stack gap-16">
              <div className="form-group">
                <label className="form-label">Full name</label>
                <input
                  type="text"
                  className={`form-input${errors.name ? ' is-error' : ''}`}
                  placeholder="Your name"
                  {...register('name', { required: 'Name is required' })}
                />
                {errors.name && <span className="form-error">{errors.name.message}</span>}
              </div>

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
                  placeholder="Min. 6 characters"
                  {...register('password', { required: 'Password is required', minLength: { value: 6, message: 'Minimum 6 characters' } })}
                />
                {errors.password && <span className="form-error">{errors.password.message}</span>}
              </div>

              {serverError && <div className="alert alert-error">{serverError}</div>}

              <button type="submit" disabled={loading} className="btn btn-primary btn-full btn-lg">
                {loading ? 'Creating account…' : 'Create account'}
              </button>
            </form>
          </div>
        </div>

        <p className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
