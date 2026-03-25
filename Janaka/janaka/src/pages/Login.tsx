import { useAuth } from '../context/AuthContext';
import { Camera, Github } from 'lucide-react';
import './Login.css';

const Login = () => {
  const { login } = useAuth();

  return (
    <div className="login-container animate-fade-in">
      <div className="login-card glass-card">
        <Camera className="login-logo-icon" size={64} />
        <h1 className="login-title">Welcome to Janaka</h1>
        <p className="login-subtitle">Discover ideas and inspiration in Sky Blue.</p>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '16px' }}>
          <button className="btn btn-primary login-btn" onClick={() => login('google')}>
            Continue with Google
          </button>
          
          <button className="btn login-btn" style={{ background: '#24292e', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }} onClick={() => login('github')}>
            <Github size={20} />
            Continue with GitHub
          </button>
        </div>
      </div>
    </div>
  );
};

export default Login;
