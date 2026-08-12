import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import AuthService from '../api/AuthService';
import { useAuth } from '../hooks/useAuth';
import { Mail, Lock, ArrowRight, Loader2 } from 'lucide-react';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const data = await AuthService.login(email, password);
      login({ email, userId: data.userId }, data.accessToken, data.refreshToken);
      navigate('/');
    } catch (err) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      height: '100vh', 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center',
      background: 'var(--bg-main)',
      backgroundImage: 'radial-gradient(at 0% 0%, rgba(56, 189, 248, 0.1) 0px, transparent 50%), radial-gradient(at 100% 0%, rgba(129, 140, 248, 0.1) 0px, transparent 50%)'
    }}>
      <div className="glass fade-in" style={{ 
        width: '100%', 
        maxSize: '400px', 
        padding: '2.5rem', 
        borderRadius: '1.5rem',
        maxWidth: '420px'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{ 
            fontSize: '2rem', 
            fontWeight: '800', 
            background: 'linear-gradient(to right, var(--accent-primary), var(--accent-secondary))',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            marginBottom: '0.5rem'
          }}>PTLT</div>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Personal Time & Learning Tracker</p>
        </div>

        {error && (
          <div style={{ 
            backgroundColor: 'rgba(239, 68, 68, 0.1)', 
            color: 'var(--danger)', 
            padding: '0.75rem', 
            borderRadius: '0.75rem', 
            fontSize: '0.875rem',
            marginBottom: '1.5rem',
            border: '1px solid rgba(239, 68, 68, 0.2)'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Email Address</label>
            <div style={{ position: 'relative' }}>
              <Mail size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
              <input 
                type="email" 
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                style={{ 
                  width: '100%', 
                  backgroundColor: 'rgba(0,0,0,0.2)', 
                  border: '1px solid var(--border-color)', 
                  borderRadius: '0.75rem', 
                  padding: '0.75rem 1rem 0.75rem 2.5rem',
                  color: 'white',
                  outline: 'none'
                }} 
              />
            </div>
          </div>

          <div style={{ marginBottom: '2rem' }}>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Password</label>
            <div style={{ position: 'relative' }}>
              <Lock size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
              <input 
                type="password" 
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                style={{ 
                  width: '100%', 
                  backgroundColor: 'rgba(0,0,0,0.2)', 
                  border: '1px solid var(--border-color)', 
                  borderRadius: '0.75rem', 
                  padding: '0.75rem 1rem 0.75rem 2.5rem',
                  color: 'white',
                  outline: 'none'
                }} 
              />
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            style={{ 
              width: '100%', 
              background: 'linear-gradient(to right, var(--accent-primary), var(--accent-secondary))',
              color: 'white',
              border: 'none',
              padding: '0.875rem',
              borderRadius: '0.75rem',
              fontWeight: '600',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              gap: '0.5rem',
              cursor: 'pointer',
              transition: 'opacity 0.2s'
            }}
          >
            {loading ? <Loader2 className="animate-spin" size={20} /> : (
              <>
                Sign In <ArrowRight size={18} />
              </>
            )}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          New here? <Link to="/register" style={{ color: 'var(--accent-primary)', textDecoration: 'none' }}>Create an account</Link>
        </p>
      </div>
    </div>
  );
};

export default Login;
