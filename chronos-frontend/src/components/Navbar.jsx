import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/login'); };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/dashboard" className="navbar-logo">
          <span className="navbar-logo-icon">C</span>
          Chronos
        </Link>
        {auth && (
          <div className="navbar-right">
            <span className="navbar-email">{auth.user.email}</span>
            <Link to="/jobs/new" className="btn btn-primary btn-sm">+ New Job</Link>
            <button onClick={handleLogout} className="btn btn-ghost btn-sm">Log out</button>
          </div>
        )}
      </div>
    </nav>
  );
}
