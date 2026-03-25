import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { User, Bell, Shield, HelpCircle, LogOut } from 'lucide-react';

const Settings = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState(user?.username || user?.name || '');
  const [bio, setBio] = useState(user?.bio || '');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setSaving(true);
    try {
      const res = await fetch(
        `/api/user/${user.userId}/profile?username=${encodeURIComponent(name)}&bio=${encodeURIComponent(bio)}`,
        { method: 'PATCH', credentials: 'include' }
      );
      if (res.ok) {
        setSaved(true);
        setTimeout(() => setSaved(false), 2000);
      } else {
        alert('Failed to save: ' + await res.text());
      }
    } catch {
      alert('Network error – is the backend running?');
    } finally {
      setSaving(false);
    }
  };

  const menuItems = [
    { icon: <User size={20} />, label: 'Public profile', active: true },
    { icon: <Bell size={20} />, label: 'Notifications', active: false },
    { icon: <Shield size={20} />, label: 'Privacy & data', active: false },
    { icon: <HelpCircle size={20} />, label: 'Help', active: false },
  ];

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)', display: 'flex' }}>
      {/* Left sidebar */}
      <div style={{ width: 260, borderRight: '1px solid #eee', padding: '40px 0', flexShrink: 0 }}>
        <h2 style={{ fontSize: '1.4rem', fontWeight: 700, padding: '0 24px 24px' }}>Settings</h2>
        {menuItems.map(item => (
          <div key={item.label} style={{
            display: 'flex', alignItems: 'center', gap: 12,
            padding: '12px 24px',
            fontWeight: item.active ? 700 : 400,
            background: item.active ? '#f0f0f0' : 'transparent',
            borderRadius: item.active ? '0 24px 24px 0' : 0,
            cursor: 'pointer',
            marginRight: item.active ? 24 : 0,
            color: item.active ? 'var(--text-primary)' : 'var(--text-secondary)',
          }}>
            {item.icon} {item.label}
          </div>
        ))}
        <div style={{ borderTop: '1px solid #eee', marginTop: 16, paddingTop: 16 }}>
          <div
            onClick={logout}
            style={{
              display: 'flex', alignItems: 'center', gap: 12,
              padding: '12px 24px', cursor: 'pointer',
              color: '#e60023', fontWeight: 500,
            }}
          >
            <LogOut size={20} /> Log out
          </div>
        </div>
      </div>

      {/* Right: form */}
      <div style={{ flex: 1, padding: '40px 60px', maxWidth: 700 }}>
        <h2 style={{ fontSize: '1.8rem', fontWeight: 700, marginBottom: 8 }}>Edit profile</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: 40 }}>
          Keep your personal details private. Information you add here is visible to anyone who can view your profile.
        </p>

        {/* Avatar */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 24, marginBottom: 36 }}>
          <div style={{
            width: 80, height: 80, borderRadius: '50%', background: '#e60023',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            overflow: 'hidden', fontSize: '2rem', color: 'white', fontWeight: 700,
          }}>
            {user?.profileImageUrl
              ? <img src={user.profileImageUrl} alt="avatar" style={{ width:'100%', height:'100%', objectFit:'cover' }} />
              : (user?.username || user?.email || '?')[0].toUpperCase()
            }
          </div>
          <div>
            <div style={{ fontWeight: 600 }}>{user?.username || user?.email}</div>
            <button style={{ color: 'var(--accent-primary)', fontWeight: 600, fontSize: '0.9rem', marginTop: 4, background: 'none', border: 'none', cursor: 'pointer' }}>
              Change
            </button>
          </div>
        </div>

        <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div>
            <label style={{ display: 'block', fontWeight: 700, marginBottom: 8 }}>Name</label>
            <input
              type="text"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Your display name"
              style={{
                width: '100%', padding: '14px 16px',
                border: '1px solid #ddd', borderRadius: 16,
                fontSize: '1rem', fontFamily: 'inherit',
                background: '#f9f9f9',
              }}
            />
            <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: 6 }}>
              This appears on your profile and in search results.
            </p>
          </div>

          <div>
            <label style={{ display: 'block', fontWeight: 700, marginBottom: 8 }}>About</label>
            <textarea
              value={bio}
              onChange={e => setBio(e.target.value)}
              placeholder="Tell your story"
              rows={3}
              style={{
                width: '100%', padding: '14px 16px',
                border: '1px solid #ddd', borderRadius: 16,
                fontSize: '1rem', fontFamily: 'inherit',
                background: '#f9f9f9', resize: 'vertical',
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontWeight: 700, marginBottom: 8 }}>Email</label>
            <input
              type="email"
              value={user?.email || ''}
              disabled
              style={{
                width: '100%', padding: '14px 16px',
                border: '1px solid #eee', borderRadius: 16,
                fontSize: '1rem', fontFamily: 'inherit',
                background: '#f0f0f0', color: '#999',
              }}
            />
            <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: 6 }}>
              Email is managed by your OAuth provider and cannot be changed here.
            </p>
          </div>

          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <button
              type="submit"
              disabled={saving}
              style={{
                background: '#e60023', color: 'white', border: 'none',
                borderRadius: 24, padding: '14px 32px',
                fontWeight: 700, fontSize: '1rem', cursor: 'pointer',
              }}
            >
              {saving ? 'Saving...' : saved ? '✓ Saved!' : 'Save'}
            </button>
            <button
              type="button"
              onClick={() => navigate('/profile')}
              style={{ border: 'none', background: 'none', fontWeight: 600, cursor: 'pointer', color: '#555' }}
            >
              Done
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Settings;
