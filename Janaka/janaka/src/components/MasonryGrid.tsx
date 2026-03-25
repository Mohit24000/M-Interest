import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Heart, Download, Loader2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import './MasonryGrid.css';

interface Pin {
  pinId: string;
  title: string;
  description: string;
  pinUrl: string;
  downloadUrl: string;
}

interface Feed {
  pins: Pin[];
  page: number;
  size: number;
  totalPins: number;
}

const SafeImage = ({ src, alt, className }: { src: string; alt: string; className?: string }) => {
  const [error, setError] = useState(false);

  if (error || !src) {
    return (
      <div 
        className={className} 
        style={{ 
          width: '100%', minHeight: '200px', background: '#f0f0f0', 
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#aaa', fontSize: '1.2rem', textAlign: 'center'
        }}
      >
        <div>
          <div style={{ fontSize: '2.5rem', marginBottom: 8 }}>🖼️</div>
          <div>{alt}</div>
        </div>
      </div>
    );
  }

  return <img src={src} alt={alt} className={className} loading="lazy" onError={() => setError(true)} />;
};

const PinItem = ({ pin, user, navigate }: { pin: any, user: any, navigate: any }) => {
  const [isLiked, setIsLiked] = useState(() => {
    return (pin.likesList || []).some((l: any) => (l.user?.userId || l.userId) === user?.userId);
  });

  const handleLike = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!user) return;
    
    // Optimistic UI update
    setIsLiked(!isLiked);

    try {
      const url = isLiked ? `/api/pins/${pin.pinId}/unlike?userId=${user.userId}` : `/api/pins/${pin.pinId}/like?userId=${user.userId}`;
      const res = await fetch(url, { method: 'POST', credentials: 'include' });
      if (!res.ok) {
        // revert on failure
        setIsLiked(isLiked);
      }
    } catch(err) {
      setIsLiked(isLiked);
    }
  };

  return (
    <div
      className="masonry-item"
      onClick={() => navigate(`/pin/${pin.pinId}`)}
      style={{ cursor: 'pointer' }}
    >
      <SafeImage
        src={pin.pinUrl}
        alt={pin.title}
      />
      <div className="masonry-overlay">
        <div className="masonry-actions">
          <button
            className={`btn-icon small-icon heart-btn ${isLiked ? 'liked' : ''}`}
            onClick={handleLike}
          >
            <Heart size={18} fill={isLiked ? 'currentColor' : 'none'} />
          </button>
        </div>
        <div className="bottom-info">
          <span className="image-title">{pin.title}</span>
          <div className="masonry-actions">
            <button
              className="btn-icon small-icon"
              onClick={(e) => {
                e.stopPropagation();
                window.open(pin.downloadUrl || pin.pinUrl, '_blank');
              }}
            >
              <Download size={18} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

interface MasonryGridProps {
  staticPins?: Pin[];
  fetchUrl?: string;
}

const MasonryGrid = ({ staticPins, fetchUrl }: MasonryGridProps) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [pins, setPins] = useState<Pin[]>(staticPins || []);
  const [isLoading, setIsLoading] = useState(!staticPins);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isFetchingMore, setIsFetchingMore] = useState(false);
  const observer = useRef<IntersectionObserver | null>(null);
  
  const lastPinElementRef = useCallback((node: HTMLDivElement | null) => {
    if (isLoading || isFetchingMore) return;
    if (observer.current) observer.current.disconnect();
    
    observer.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasMore) {
        setPage(prevPage => prevPage + 1);
      }
    });
    
    if (node) observer.current.observe(node);
  }, [isLoading, isFetchingMore, hasMore]);

  // Initial fetch or fetchUrl change
  useEffect(() => {
    if (staticPins) {
      setPins(staticPins);
      setIsLoading(false);
      setHasMore(false);
      return;
    }
    
    // Reset when URL changes
    console.log("### FRONTEND_TRACE_V2 ### Resetting grid for URL:", fetchUrl);
    setPins([]);
    setPage(0);
    setHasMore(true);
    setIsLoading(true);
  }, [staticPins, fetchUrl]);

  // Pagination fetch
  useEffect(() => {
    if (staticPins || !hasMore || (page === 0 && pins.length > 0)) return;

    const fetchPins = async () => {
      if (page > 0) setIsFetchingMore(true);
      
      const baseUrl = fetchUrl || '/api/pins/trending';
      const separator = baseUrl.includes('?') ? '&' : '?';
      const url = `${baseUrl}${separator}page=${page}&size=20`;

      try {
        const res = await fetch(url, { credentials: 'include' });
        const data: Feed = await res.json();
        console.log("### FRONTEND_TRACE_V2 ### Received pins:", data.pins.length, "Total available:", data.totalPins, "Page:", data.page);
        
        setPins(prev => {
          // Avoid duplicates if any
          const newPins = data.pins.filter(np => !prev.some(pp => pp.pinId === np.pinId));
          return [...prev, ...newPins];
        });
        
        // If we've reached the total number of pins, stop fetching
        if (data.pins.length < data.size || (pins.length + data.pins.length) >= data.totalPins) {
          setHasMore(false);
        }
      } catch (err) {
        console.error('Failed to fetch pins:', err);
        setHasMore(false);
      } finally {
        setIsLoading(false);
        setIsFetchingMore(false);
      }
    };

    fetchPins();
  }, [page, fetchUrl, staticPins]);

  if (isLoading && pins.length === 0) {
    return <div style={{ textAlign: 'center', padding: '100px', fontSize: '1.2rem', color: 'var(--text-secondary)' }}>[V2] Loading amazing ideas...</div>;
  }

  if (pins.length === 0 && !isLoading) {
    return <div style={{ textAlign: 'center', padding: '100px', fontSize: '1.2rem', color: 'var(--text-secondary)' }}>No pins to show yet. Be the first to upload!</div>;
  }

  return (
    <>
      <div className="masonry-grid-wrapper">
        <div className="masonry-grid">
          {pins.map((pin) => (
            <PinItem key={pin.pinId} pin={pin} user={user} navigate={navigate} />
          ))}
        </div>
        
        {/* Dedicated sentinel for infinite scroll */}
        <div ref={lastPinElementRef} className="load-more-trigger" style={{ height: '20px', margin: '20px 0' }}>
          {isFetchingMore && (
            <div style={{ display: 'flex', justifyContent: 'center' }}>
              <Loader2 className="animate-spin" size={32} color="#e60023" />
            </div>
          )}
        </div>
      </div>
      
      {!hasMore && pins.length > 0 && (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          You've reached the end of the feed! ✨
        </div>
      )}
    </>
  );
};

export default MasonryGrid;
