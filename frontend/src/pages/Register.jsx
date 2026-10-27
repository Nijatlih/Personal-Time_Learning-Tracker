import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import AuthService from '../api/AuthService';
import { useAuth } from '../hooks/useAuth';
import { Mail, Lock, Building, Loader2, UserPlus } from 'lucide-react';
import { useLanguage } from '../hooks/useLanguage';

const Register = () => {
  const { t } = useLanguage();
  const { login } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    workspaceName: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const data = await AuthService.register(formData.email, formData.password, formData.workspaceName);
      login({ email: formData.email, userId: data.userId }, data.accessToken, data.refreshToken);
      navigate('/');
    } catch (err) {
      setError(err.message || t('auth.registerFailed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ width: '100%', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
      <div className="glass fade-in" style={{ 
        width: '100%', 
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
          <h2 style={{ fontSize: '1.25rem', fontWeight: '700', marginBottom: '0.25rem' }}>{t('auth.createAccount')}</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{t('auth.createAccountSub')}</p>
        </div>

        {error && (
          <div role="alert" style={{
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
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.4rem' }}>{t('auth.email')}</label>
            <div style={{ position: 'relative' }}>
              <Mail size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
              <input 
                type="email"
                autoComplete="email"
                className="input-field"
                value={formData.email}
                onChange={(e) => setFormData({...formData, email: e.target.value})}
                required
                style={{ width: '100%', backgroundColor: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)', borderRadius: '0.75rem', padding: '0.75rem 1rem 0.75rem 2.5rem', color: 'white', outline: 'none' }} 
              />
            </div>
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.4rem' }}>{t('auth.password')}</label>
            <div style={{ position: 'relative' }}>
              <Lock size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
              <input 
                type="password"
                autoComplete="new-password"
                className="input-field"
                value={formData.password}
                onChange={(e) => setFormData({...formData, password: e.target.value})}
                required
                minLength={8}
                style={{ width: '100%', backgroundColor: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)', borderRadius: '0.75rem', padding: '0.75rem 1rem 0.75rem 2.5rem', color: 'white', outline: 'none' }} 
              />
            </div>
          </div>

          <div style={{ marginBottom: '2rem' }}>
            <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.4rem' }}>{t('auth.workspaceName')}</label>
            <div style={{ position: 'relative' }}>
              <Building size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
              <input 
                type="text"
                className="input-field"
                value={formData.workspaceName}
                onChange={(e) => setFormData({...formData, workspaceName: e.target.value})}
                required
                placeholder={t('auth.workspacePlaceholder')}
                style={{ width: '100%', backgroundColor: 'rgba(0,0,0,0.2)', border: '1px solid var(--border-color)', borderRadius: '0.75rem', padding: '0.75rem 1rem 0.75rem 2.5rem', color: 'white', outline: 'none' }} 
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
              cursor: 'pointer'
            }}
          >
            {loading ? <Loader2 className="animate-spin" size={20} /> : (
              <>
                {t('auth.createAccount')} <UserPlus size={18} />
              </>
            )}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          {t('auth.haveAccount')} <Link to="/login" style={{ color: 'var(--accent-primary)', textDecoration: 'none' }}>{t('auth.signIn')}</Link>
        </p>
      </div>
    </div>
  );
};

export default Register;
