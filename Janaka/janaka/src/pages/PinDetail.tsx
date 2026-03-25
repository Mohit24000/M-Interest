import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Heart, Download, ChevronLeft, Send, MoreHorizontal, Share2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import './PinDetail.css';

interface Comment {
  id: number;
  content: string;
  user?: {
    username: string;
    profileImageUrl?: string;
    userId: string;
  };
  replies?: Comment[];
}

const SafeAvatar = ({ src, alt, className, size = 40 }: { src?: string; alt?: string; className?: string; size?: number }) => {
  const [error, setError] = React.useState(false);
  const initial = alt ? alt.charAt(0).toUpperCase() : '?';

  if (!src || error) {
    return (
      <div 
        className={className} 
        style={{ 
          width: size, height: size, borderRadius: '50%', 
          background: '#e6e6e6', color: '#555', 
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: size * 0.4, fontWeight: 700,
          flexShrink: 0
        }}
      >
        {initial}
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      className={className}
      style={{ width: size, height: size, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
      onError={() => setError(true)}
    />
  );
};

const SafeImage = ({ src, alt, className }: { src: string; alt: string; className?: string }) => {
  const [error, setError] = React.useState(false);

  if (error || !src) {
    return (
      <div 
        className={className} 
        style={{ 
          width: '100%', aspectRatio: '1/1', background: '#f0f0f0', 
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#aaa', fontSize: '0.9rem', textAlign: 'center', padding: 20
        }}
      >
        <div>
          <div style={{ fontSize: '2rem', marginBottom: 8 }}>🖼️</div>
          Image Not Available
        </div>
      </div>
    );
  }

  return <img src={src} alt={alt} className={className} onError={() => setError(true)} />;
};

const CommentItem = ({ comment, onReply, onDelete, user, pinId, depth = 0 }: { comment: Comment; onReply: (parent: Comment) => void; onDelete: (commentId: number) => void; user: any; pinId: string; depth?: number }) => {
  return (
    <div className="comment-item">
      <div className="comment-main">
        <SafeAvatar 
          src={comment.user?.profileImageUrl} 
          alt={comment.user?.username} 
          className="comment-avatar" 
          size={32} 
        />
        <div className="comment-content-wrapper">
          <div className="comment-text">
            <span style={{ fontWeight: 700, marginRight: 8 }}>{comment.user?.username || 'User'}</span>
            {comment.content}
          </div>
          <div className="comment-actions">
            <span onClick={() => onReply(comment)}>Reply</span>
            {(user?.userId === comment.user?.userId) && (
              <span onClick={() => onDelete(comment.id)} style={{ color: '#ff4444' }}>Delete</span>
            )}
          </div>
        </div>
      </div>
      {comment.replies && comment.replies.length > 0 && (
        <div className={depth < 3 ? "replies-list" : "replies-list-flat"}>
          {comment.replies.map(reply => (
            <CommentItem key={reply.id} comment={reply} onReply={onReply} onDelete={onDelete} user={user} pinId={pinId} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
};

const PinDetail = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [pin, setPin] = useState<any>(null);
  const [commentText, setCommentText] = useState('');
  const [replyTo, setReplyTo] = useState<Comment | null>(null);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isLiked, setIsLiked] = useState(false);
  const [isSaved, setIsSaved] = useState(false);
  const [likesCount, setLikesCount] = useState(0);

  const fetchPinDetails = useCallback(async () => {
    try {
      const res = await fetch(`/api/pins/${id}`, { credentials: 'include' });
      const data = await res.json();
      setPin(data);

      if (user) {
        // Initial like state from pin data
        const liked = (data.likesList || []).some((l: any) => (l.user?.userId || l.userId) === user.userId);
        setIsLiked(liked);

        // Fetch saved status
        const savedRes = await fetch(`/api/pins/${id}/isSaved?userId=${user.userId}`, { credentials: 'include' });
        if (savedRes.ok) {
          const savedData = await savedRes.json();
          setIsSaved(savedData);
        }
      }

      // Fetch latest likes count
      const likesRes = await fetch(`/api/pins/${id}/likes`, { credentials: 'include' });
      if (likesRes.ok) {
        const count = await likesRes.json();
        setLikesCount(count);
      }
    } catch (err) {
      console.error('Failed to fetch pin details', err);
    }
  }, [id, user]);

  useEffect(() => {
    fetchPinDetails();
  }, [fetchPinDetails]);

  const handleComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim() || !user) return;

    const url = replyTo
      ? `/api/comments/${id}/comments/${replyTo.id}/replies?content=${encodeURIComponent(commentText)}&userId=${user.userId}`
      : `/api/comments/${id}/comments?content=${encodeURIComponent(commentText)}&userId=${user.userId}`;

    try {
      const res = await fetch(url, { method: 'POST', credentials: 'include' });
      if (res.ok) {
        setCommentText('');
        setReplyTo(null);
        fetchPinDetails();
      } else {
        const errorText = await res.text();
        alert(`Failed to send comment: ${errorText}`);
      }
    } catch (err) {
      console.error(err);
      alert("Network error while sending comment.");
    }
  };

  const toggleFollow = async () => {
    if (!user || !pin?.user) return;
    const method = isFollowing ? 'DELETE' : 'POST';
    try {
      await fetch(`/api/follow/${pin.user.username}?followerUsername=${user.username}`, { method, credentials: 'include' });
      setIsFollowing(!isFollowing);
    } catch (err) {
      console.error(err);
    }
  };

  const [showMenu, setShowMenu] = useState(false);

  const handleLike = async () => {
    if (!user) return;
    const url = isLiked ? `/api/pins/${id}/unlike?userId=${user.userId}` : `/api/pins/${id}/like?userId=${user.userId}`;
    try {
      const res = await fetch(url, { method: 'POST', credentials: 'include' });
      if (res.ok) {
        setIsLiked(!isLiked);
        setLikesCount(prev => isLiked ? prev - 1 : prev + 1);
      } else {
        const txt = await res.text();
        alert(`Like action failed: ${txt}`);
      }
    } catch (err) {
      console.error(err);
      alert("Network error on Like action.");
    }
  };

  const handleSave = async () => {
    if (!user) return;
    const url = isSaved ? `/api/pins/${id}/unsave?userId=${user.userId}` : `/api/pins/${id}/save?userId=${user.userId}`;
    try {
      const res = await fetch(url, { method: 'POST', credentials: 'include' });
      if (res.ok) {
        setIsSaved(!isSaved);
      } else {
        const txt = await res.text();
        alert(`Save action failed: ${txt}`);
      }
    } catch (err) {
      console.error(err);
      alert("Network error on Save action.");
    }
  };

  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to delete this pin?")) return;
    try {
      const res = await fetch(`/api/pins/${id}`, { method: 'DELETE', credentials: 'include' });
      if (res.ok) {
        navigate('/');
      } else {
        const errorText = await res.text();
        alert(`Failed to delete pin: ${errorText}`);
      }
    } catch (err) {
      console.error(err);
      alert("Error deleting pin.");
    }
  };
  
  const handleDeleteComment = async (commentId: number) => {
    if (!window.confirm("Are you sure you want to delete this comment?")) return;
    try {
      const res = await fetch(`/api/comments/comments/${commentId}`, { 
        method: 'DELETE', 
        credentials: 'include' 
      });
      if (res.ok) {
        fetchPinDetails();
      } else {
        const errorText = await res.text();
        alert(`Failed to delete comment: ${errorText}`);
      }
    } catch (err) {
      console.error(err);
      alert("Error deleting comment.");
    }
  };

  if (!pin) return <div className="pindetail-container">Loading...</div>;

  return (
    <div className="pindetail-container">
      <button className="back-button-prominent" onClick={() => navigate(-1)}>
        <ChevronLeft size={20} />
        <span>Back to Feed</span>
      </button>

      <div className="pin-card">
        <div className="pin-image-section">
          <SafeImage src={pin.pinUrl} alt={pin.title} />
        </div>

        <div className="pin-info-section">
          <div className="pin-actions-top">
            <div className="action-left" style={{ position: 'relative' }}>
              <button className="btn-circle" onClick={() => setShowMenu(!showMenu)}><MoreHorizontal size={20} /></button>
              {showMenu && pin.user?.userId === user?.userId && (
                <div style={{ position: 'absolute', top: '100%', left: 0, background: 'white', border: '1px solid #ddd', borderRadius: '8px', padding: '8px', zIndex: 100, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
                  <button onClick={handleDelete} style={{ color: 'red', border: 'none', background: 'none', cursor: 'pointer', padding: '4px 8px', fontWeight: 600 }}>Delete Pin</button>
                </div>
              )}
              <button className="btn-circle"><Share2 size={20} /></button>
            </div>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                className={`btn-save ${isSaved ? 'saved' : ''}`}
                onClick={handleSave}
              >
                {isSaved ? 'Saved' : 'Save'}
              </button>
            </div>
          </div>

          <h1 className="pin-title">{pin.title}</h1>
          <p className="pin-desc">{pin.description}</p>

          {pin.tags && pin.tags.length > 0 && (
            <div className="tags-container" style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '24px' }}>
              {pin.tags.map((tag: string, idx: number) => (
                <span
                  key={idx}
                  className="tag-pill"
                  style={{ background: '#f0f0f0', padding: '6px 12px', borderRadius: '16px', fontSize: '13px', fontWeight: 600, color: '#333' }}
                  onClick={() => navigate(`/search?q=${tag}`)}
                >
                  #{tag}
                </span>
              ))}
            </div>
          )}

          <div className="user-row">
            <div 
              className="user-info"
              onClick={() => {
                if (pin.user?.userId) navigate(`/profile/${pin.user.userId}`);
              }}
              style={{ cursor: 'pointer' }}
              title="View Profile"
            >
              <SafeAvatar 
                src={pin.user?.profileImageUrl} 
                alt={pin.user?.username} 
                className="user-avatar" 
                size={48} 
              />
              <div>
                <div className="username">{pin.user?.username || 'Unknown'}</div>
                <div className="follower-count">{pin.user?.followerCount || 0} followers</div>
              </div>
            </div>
            {user && pin.user && pin.user.userId !== user.userId && (
              <button className="btn-follow" onClick={toggleFollow}>
                {isFollowing ? 'Following' : 'Follow'}
              </button>
            )}
          </div>

          <div className="comments-container">
            <h3 className="comments-header">Comments</h3>
            {(pin.commentsList || []).map((c: Comment) => (
              <CommentItem 
                key={c.id} 
                comment={c} 
                onReply={setReplyTo} 
                onDelete={handleDeleteComment} 
                user={user}
                pinId={id!} 
              />
            ))}
            {(!pin.commentsList || pin.commentsList.length === 0) && (
              <p style={{ color: 'var(--text-mute)', fontStyle: 'italic' }}>No comments yet. Add one!</p>
            )}
          </div>

          <div className="interaction-bar" style={{ display: 'flex', gap: '12px', borderTop: '1px solid #eee', paddingTop: '16px' }}>
            <button
              className="btn-circle"
              title="Like"
              onClick={handleLike}
              style={{ color: isLiked ? 'var(--accent-red)' : 'inherit', display: 'flex', gap: '4px', width: 'auto', padding: '0 12px' }}
            >
              <Heart size={20} fill={isLiked ? 'currentColor' : 'none'} />
              <span style={{ fontSize: '14px', fontWeight: 600 }}>{likesCount}</span>
            </button>
            <button className="btn-circle" title="Download" onClick={() => window.open(pin.downloadUrl || pin.pinUrl, '_blank')}><Download size={20} /></button>
          </div>

          <form className="comment-input-section" onSubmit={handleComment}>
            <div className="user-avatar small" style={{ width: 44, height: 44, flexShrink: 0 }}>
              {user?.profileImageUrl ? <img src={user.profileImageUrl} alt="me" style={{ width: '100%', borderRadius: '50%' }} /> : <div style={{ width: '100%', height: '100%', borderRadius: '50%', background: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>👤</div>}
            </div>
            <div className="comment-input-wrapper">
              <input
                type="text"
                placeholder={replyTo ? `Replying to @${replyTo.user?.username || 'user'}...` : "Add a comment"}
                value={commentText}
                onChange={e => setCommentText(e.target.value)}
              />
              <button type="submit" className="btn-send" style={{ background: 'none', border: 'none' }}>
                <Send size={20} />
              </button>
            </div>
          </form>
          {replyTo && (
            <div style={{ fontSize: '12px', color: 'var(--accent-red)', marginTop: '4px', display: 'flex', justifyContent: 'space-between' }}>
              <span>Replying to {replyTo.user?.username}</span>
              <span style={{ cursor: 'pointer' }} onClick={() => setReplyTo(null)}>Cancel</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PinDetail;
