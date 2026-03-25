import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, useParams } from 'react-router-dom';
import { Settings } from 'lucide-react';
import MasonryGrid from '../components/MasonryGrid';

const Profile = () => {
  const { user: authUser, logout } = useAuth();
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const [profileUser, setProfileUser] = useState<any>(null);
  const [tab, setTab] = useState<'created' | 'saved'>('created');
  const [isFollowing, setIsFollowing] = useState(false);

  useEffect(() => {
    if (userId) {
      fetch(`/api/user/${userId}`, { credentials: 'include' })
        .then(res => res.json())
        .then(data => setProfileUser(data))
        .catch(err => console.error(err));
    } else {
      setProfileUser(authUser);
    }
  }, [userId, authUser]);

  const toggleFollow = async () => {
    if (!authUser || !profileUser) return;
    const method = isFollowing ? 'DELETE' : 'POST';
    try {
      await fetch(`/api/follow/${profileUser.username}?followerUsername=${authUser.username}`, { method, credentials: 'include' });
      setIsFollowing(!isFollowing);
    } catch (err) {
      console.error(err);
    }
  };

  if (!profileUser) return (
    <div style={{ paddingTop: '100px', textAlign: 'center', color: 'var(--text-secondary)' }}>
      Loading profile…
    </div>
  );

  const isOwnProfile = !userId || userId === authUser?.userId;
  const displayName = profileUser.username || profileUser.name || profileUser.email?.split('@')[0] || 'User';
  const initial = displayName[0].toUpperCase();

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)' }}>
      {/* Header */}
      <div style={{ padding: '60px 0 32px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
        {/* Avatar */}
        <div style={{
          width: 120, height: 120, borderRadius: '50%',
          background: '#e60023',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '3rem', color: 'white', fontWeight: 700,
          overflow: 'hidden',
        }}>
          {profileUser.profileImageUrl ? (
            <img 
              src={profileUser.profileImageUrl} 
              alt="avatar" 
              style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = 'none';
                (e.target as HTMLImageElement).parentElement!.innerText = initial;
              }}
            />
          ) : initial}
        </div>

        {/* Name */}
        <h1 style={{ fontSize: '2rem', fontWeight: 700 }}>{displayName}</h1>

        {/* Handle & follower count */}
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          @{profileUser.username || profileUser.email?.split('@')[0]} · {profileUser.followerCount || 0} followers
        </p>

        {/* Action buttons */}
        <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
          {isOwnProfile ? (
            <>
              <button
                onClick={() => navigate('/settings')}
                style={{
                  border: 'none', background: '#efefef', borderRadius: 24,
                  padding: '10px 20px', fontWeight: 600, cursor: 'pointer',
                  fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: 8,
                }}
              >
                <Settings size={16} /> Edit profile
              </button>
              <button
                onClick={logout}
                style={{
                  border: 'none', background: '#ffe0e0', borderRadius: 24,
                  padding: '10px 20px', fontWeight: 600, cursor: 'pointer',
                  fontSize: '0.9rem', color: '#e60023',
                }}
              >
                Log out
              </button>
            </>
          ) : (
            <button
              onClick={toggleFollow}
              style={{
                border: 'none', background: isFollowing ? '#efefef' : '#e60023', color: isFollowing ? 'black' : 'white', borderRadius: 24,
                padding: '10px 32px', fontWeight: 700, cursor: 'pointer',
                fontSize: '0.9rem',
              }}
            >
              {isFollowing ? 'Following' : 'Follow'}
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 32, borderBottom: '2px solid #eee', paddingBottom: 0 }}>
        {(['created', 'saved'] as const).map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            style={{
              border: 'none', background: 'none', fontWeight: 700,
              fontSize: '1rem', cursor: 'pointer',
              paddingBottom: 16,
              color: tab === t ? 'var(--text-primary)' : 'var(--text-secondary)',
              borderBottom: tab === t ? '3px solid var(--text-primary)' : '3px solid transparent',
              textTransform: 'capitalize',
            }}
          >
            {t === 'created' ? 'Created' : 'Saved'}
          </button>
        ))}
      </div>

      {/* Pin grid */}
      <div style={{ padding: '24px 16px', maxWidth: 1400, margin: '0 auto' }}>
        {tab === 'created'
          ? <MasonryGrid fetchUrl={`/api/pins/user/${profileUser.userId}`} />
          : <MasonryGrid fetchUrl={`/api/pins/saved/${profileUser.userId}`} />
        }
      </div>
    </div>
  );
};

export default Profile;
