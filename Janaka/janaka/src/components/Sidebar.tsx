import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Home, Search, Bell, Plus, Settings, X } from 'lucide-react';
import UploadModal from './UploadModal';
import './Sidebar.css';

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [showCreateMenu, setShowCreateMenu] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<{ id: number; message: string; time: Date }[]>([]);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [toast, setToast] = useState<{ message: string; visible: boolean }>({ message: '', visible: false });

  useEffect(() => {
    const sse = new EventSource('/api/stream', { withCredentials: true });
    
    const handleEvent = (msg: string) => {
      setToast({ message: msg, visible: true });
      setTimeout(() => setToast(prev => ({ ...prev, visible: false })), 4000);
      setNotifications(prev => [{ id: Date.now(), message: msg, time: new Date() }, ...prev]);
      setUnreadCount(prev => prev + 1);
    };

    sse.addEventListener('pin_upload', () => handleEvent('📌 Someone uploaded a new pin!'));

    return () => sse.close();
  }, []);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    const delayDebounceFn = setTimeout(async () => {
      try {
        const res = await fetch(`/api/search/suggestions?q=${encodeURIComponent(searchQuery)}`, { credentials: 'include' });
        if (res.ok) {
          const data = await res.json();
          const uniqueSuggestions = Array.from(new Set(data)).slice(0, 5) as string[];
          setSuggestions(uniqueSuggestions);
          setShowSuggestions(uniqueSuggestions.length > 0);
        }
      } catch (err) {
        console.error("Failed to fetch suggestions", err);
      }
    }, 300);

    return () => clearTimeout(delayDebounceFn);
  }, [searchQuery]);

  const handleSearch = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
      setSearchOpen(false);
      setSearchQuery('');
    }
  };

  const navItems = [
    { icon: <Home size={24} />, label: 'Home', path: '/' },
    { icon: <Search size={24} />, label: 'Search', path: null, action: () => setSearchOpen(s => !s) },
    { 
      icon: (
        <div style={{ position: 'relative', display: 'flex' }}>
          <Bell size={24} />
          {unreadCount > 0 && (
            <span style={{
              position: 'absolute', top: -6, right: -6, background: '#ef4444', color: 'white',
              fontSize: '10px', fontWeight: 'bold', width: 18, height: 18, borderRadius: '50%',
              display: 'flex', alignItems: 'center', justifyContent: 'center', outline: '2px solid white'
            }}>
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </div>
      ), 
      label: 'Notifications', 
      path: null, 
      action: () => {
        setNotificationsOpen(s => !s);
        if (!notificationsOpen) setUnreadCount(0);
      } 
    },
  ];

  return (
    <>
      <aside className="sidebar">
        {/* Profile picture at top — click to go to profile */}
        <button className="sidebar-logo" onClick={() => navigate('/profile')} title="My Profile">
          {user?.profileImageUrl
            ? <img src={user.profileImageUrl} alt="profile" style={{ width: 44, height: 44, borderRadius: '50%', objectFit: 'cover' }} />
            : <div style={{
                width: 44, height: 44, borderRadius: '50%',
                background: 'linear-gradient(135deg, #0ea5e9, #0284c7)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: 'white', fontWeight: 700, fontSize: '1.2rem',
              }}>
                {(user?.username || user?.email || '?')[0].toUpperCase()}
              </div>
          }
        </button>

        {/* Nav items */}
        <nav className="sidebar-nav">
          {navItems.map(item => (
            <button
              key={item.label}
              className={`sidebar-btn ${item.path && location.pathname === item.path ? 'active' : ''}`}
              onClick={() => { if (item.action) item.action(); else if (item.path) navigate(item.path); }}
              title={item.label}
            >
              {item.icon}
            </button>
          ))}

          {/* Create/Upload button */}
          <div className="sidebar-create-wrapper">
            <button
              className="sidebar-btn create-btn"
              onClick={() => setShowCreateMenu(!showCreateMenu)}
              title="Create"
            >
              <Plus size={24} />
            </button>
            {showCreateMenu && (
              <div className="create-menu animate-fade-in">
                <div className="create-menu-item" onClick={() => { setIsUploadOpen(true); setShowCreateMenu(false); }}>
                  <div className="create-menu-icon">📌</div>
                  <div>
                    <div style={{fontWeight: 700}}>Pin</div>
                    <div style={{fontSize: '0.8rem', color: 'var(--text-secondary)'}}>Post your photos or videos</div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </nav>

        {/* Bottom: Settings only */}
        <div className="sidebar-bottom">
          <button
            className={`sidebar-btn ${location.pathname === '/settings' ? 'active' : ''}`}
            onClick={() => navigate('/settings')}
            title="Settings"
          >
            <Settings size={22} />
          </button>
        </div>
      </aside>

      <UploadModal isOpen={isUploadOpen} onClose={() => setIsUploadOpen(false)} />

      {/* Overlay to close create menu */}
      {showCreateMenu && <div style={{position:'fixed',inset:0,zIndex:90}} onClick={() => setShowCreateMenu(false)} />}

      {/* Search flyout */}
      {searchOpen && (
        <div style={{
          position: 'fixed', left: 80, top: 0, bottom: 0,
          width: 320, background: 'white',
          boxShadow: '4px 0 24px rgba(0,0,0,0.12)',
          zIndex: 99, display: 'flex', flexDirection: 'column', padding: 24,
        }} className="animate-fade-in">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
            <h3 style={{ fontWeight: 700, fontSize: '1.2rem' }}>Search</h3>
            <button onClick={() => setSearchOpen(false)} style={{ background: '#f0f0f0', border: 'none', borderRadius: '50%', width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <X size={18} />
            </button>
          </div>
          <div style={{ position: 'relative' }}>
            <Search size={18} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: '#999' }} />
            <input
              autoFocus
              type="text"
              value={searchQuery}
              onChange={e => {
                setSearchQuery(e.target.value);
                setShowSuggestions(true);
              }}
              onFocus={() => {
                if (suggestions.length > 0) setShowSuggestions(true);
              }}
              onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
              onKeyDown={handleSearch}
              placeholder="Search amazing ideas…"
              style={{
                width: '100%', padding: '12px 12px 12px 44px',
                borderRadius: 24, border: '1px solid #eee',
                background: '#f9f9f9', fontSize: '0.95rem',
                fontFamily: 'inherit',
              }}
            />
            {showSuggestions && suggestions.length > 0 && (
              <ul style={{
                position: 'absolute', top: '100%', left: 0, right: 0,
                marginTop: 8, background: 'white', border: '1px solid #eee',
                borderRadius: 16, boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
                listStyle: 'none', padding: '8px 0', zIndex: 100
              }}>
                {suggestions.map((suggestion, idx) => (
                  <li
                    key={idx}
                    onMouseDown={(e) => {
                      e.preventDefault();
                      setSearchQuery(suggestion);
                      navigate(`/search?q=${encodeURIComponent(suggestion)}`);
                      setSearchOpen(false);
                      setSearchQuery('');
                      setShowSuggestions(false);
                    }}
                    style={{
                      padding: '10px 20px', cursor: 'pointer',
                      display: 'flex', alignItems: 'center', gap: 12,
                      fontSize: '0.95rem', color: '#333'
                    }}
                    onMouseEnter={(e) => e.currentTarget.style.background = '#f0f0f0'}
                    onMouseLeave={(e) => e.currentTarget.style.background = 'white'}
                  >
                    <Search size={14} color="#666" />
                    <span>{suggestion}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <p style={{ color: '#999', fontSize: '0.85rem', marginTop: 16 }}>Press Enter to search</p>
        </div>
      )}

      {/* Notifications flyout */}
      {notificationsOpen && (
        <div style={{
          position: 'fixed', left: 80, top: 0, bottom: 0,
          width: 320, background: 'white',
          boxShadow: '4px 0 24px rgba(0,0,0,0.12)',
          zIndex: 99, display: 'flex', flexDirection: 'column', padding: 24,
        }} className="animate-fade-in">
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
            <h3 style={{ fontWeight: 700, fontSize: '1.2rem' }}>Notifications</h3>
            <button onClick={() => setNotificationsOpen(false)} style={{ background: '#f0f0f0', border: 'none', borderRadius: '50%', width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <X size={18} />
            </button>
          </div>
          
          <div style={{ overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
            {notifications.length === 0 ? (
              <p style={{ color: '#999', fontSize: '0.9rem', textAlign: 'center', marginTop: 40 }}>No new notifications</p>
            ) : (
              notifications.map(n => (
                <div key={n.id} style={{ padding: '12px 16px', background: '#f9f9f9', borderRadius: 12, fontSize: '0.95rem' }}>
                  {n.message}
                  <div style={{ fontSize: '0.75rem', color: '#999', marginTop: 4 }}>
                    {n.time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Toast Overlay */}
      {toast.visible && (
        <div style={{
          position: 'fixed', top: '24px', right: '24px',
          background: '#111', color: 'white',
          padding: '14px 20px', borderRadius: '12px',
          zIndex: 10000, boxShadow: '0 10px 40px rgba(0,0,0,0.25)',
          fontWeight: 600, fontSize: '0.95rem',
        }} className="animate-fade-in">
          {toast.message}
        </div>
      )}
    </>
  );
};

export default Sidebar;
