import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import MasonryGrid from '../components/MasonryGrid';
import { Search as SearchIcon, X } from 'lucide-react';

const POPULAR_TAGS = ['Nature', 'Architecture', '3D Renders', 'Cyberpunk', 'Abstract', 'Minimalism', 'Space', 'Art', 'Anime', 'Cars'];

// Shape returned by both Elasticsearch results (PinDocument) and Feed (Pin)
interface PinItem {
  pinId: string;
  title: string;
  description?: string;
  pinUrl: string;
  downloadUrl?: string;
  tags?: string[];
}

const Search = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const query = searchParams.get('q') || '';
  const tagFilter = searchParams.get('tag') || '';

  const [input, setInput] = useState(query);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [pins, setPins] = useState<PinItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setInput(query);
  }, [query]);

  useEffect(() => {
    if (!input.trim()) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    const delayDebounceFn = setTimeout(async () => {
      try {
        const res = await fetch(`/api/search/suggestions?q=${encodeURIComponent(input)}`, { credentials: 'include' });
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
  }, [input]);

  useEffect(() => {
    if (!query && !tagFilter) {
      setPins([]);
      return;
    }

    setLoading(true);
    setError('');

    // Build URL — try Elasticsearch first, then DB fallback
    let esUrl: string;
    if (tagFilter) {
      esUrl = `/api/search/pins?tags=${encodeURIComponent(tagFilter)}&size=40`;
    } else {
      esUrl = `/api/search/pins?q=${encodeURIComponent(query)}&size=40`;
    }

    const dbFallbackUrl = query
      ? `/api/pins/search?q=${encodeURIComponent(query)}`
      : `/api/pins/search?q=`;

    fetch(esUrl, { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error('ES failed');
        return res.json();
      })
      .then(data => {
        // Elasticsearch returns a Spring Page: { content: [...] }
        const items: PinItem[] = (data.content || []);
        if (items.length === 0 && query) {
          // Fall through to DB search
          return fetch(dbFallbackUrl, { credentials: 'include' }).then(r => r.json());
        }
        setPins(items);
        setLoading(false);
        return null;
      })
      .then(fallbackData => {
        if (!fallbackData) return; // Already set from ES
        // DB fallback returns Feed: { pins: [...] }
        const items: PinItem[] = fallbackData.pins || fallbackData.content || [];
        setPins(items);
        setLoading(false);
      })
      .catch(() => {
        // ES unreachable — go straight to DB
        fetch(dbFallbackUrl, { credentials: 'include' })
          .then(r => r.json())
          .then(data => {
            const items: PinItem[] = data.pins || data.content || [];
            setPins(items);
          })
          .catch(() => setError('Search failed. Is the backend running?'))
          .finally(() => setLoading(false));
      });
  }, [query, tagFilter]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (input.trim()) {
      navigate(`/search?q=${encodeURIComponent(input.trim())}`);
    }
  };

  const handleTagClick = (tag: string) => {
    navigate(`/search?tag=${encodeURIComponent(tag)}`);
  };

  const clearSearch = () => {
    setInput('');
    setSuggestions([]);
    setShowSuggestions(false);
    navigate('/search');
  };

  const activeLabel = tagFilter ? `Tag: ${tagFilter}` : query ? `"${query}"` : null;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)', paddingTop: 24 }}>
      {/* Search input bar */}
      <div style={{ maxWidth: 680, margin: '0 auto', padding: '0 24px 32px' }}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <SearchIcon
              size={20}
              style={{ position: 'absolute', left: 16, top: '50%', transform: 'translateY(-50%)', color: '#999', pointerEvents: 'none' }}
            />
            <input
              type="text"
              value={input}
              onChange={e => {
                setInput(e.target.value);
                setShowSuggestions(true);
              }}
              placeholder="Search by title, description…"
              style={{
                width: '100%',
                padding: '14px 48px 14px 48px',
                borderRadius: 32,
                border: '2px solid #eee',
                fontSize: '1rem',
                fontFamily: 'inherit',
                background: '#f9f9f9',
                outline: 'none',
                transition: 'border-color 0.2s',
              }}
              onFocus={e => {
                e.currentTarget.style.borderColor = 'var(--accent-primary)';
                if (suggestions.length > 0) setShowSuggestions(true);
              }}
              onBlur={e => {
                e.currentTarget.style.borderColor = '#eee';
                setTimeout(() => setShowSuggestions(false), 200);
              }}
            />
            {input && (
              <button
                type="button"
                onClick={clearSearch}
                style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', background: '#eee', border: 'none', borderRadius: '50%', width: 28, height: 28, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
              >
                <X size={14} />
              </button>
            )}
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
                      setInput(suggestion);
                      navigate(`/search?q=${encodeURIComponent(suggestion)}`);
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
                    <SearchIcon size={14} color="#666" />
                    <span>{suggestion}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <button
            type="submit"
            style={{ background: 'var(--accent-primary)', color: 'white', border: 'none', borderRadius: 32, padding: '14px 24px', fontWeight: 700, cursor: 'pointer', fontSize: '0.95rem', whiteSpace: 'nowrap' }}
          >
            Search
          </button>
        </form>

        {/* Popular tag chips */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 16 }}>
          {POPULAR_TAGS.map(tag => (
            <button
              key={tag}
              onClick={() => handleTagClick(tag)}
              style={{
                padding: '6px 16px',
                borderRadius: 20,
                border: `2px solid ${tagFilter === tag ? 'var(--accent-primary)' : '#e8e8e8'}`,
                background: tagFilter === tag ? 'var(--accent-primary)' : 'white',
                color: tagFilter === tag ? 'white' : 'var(--text-primary)',
                fontWeight: 500,
                fontSize: '0.85rem',
                cursor: 'pointer',
                transition: 'all 0.15s',
              }}
            >
              {tag}
            </button>
          ))}
        </div>
      </div>

      {/* Results */}
      <div style={{ padding: '0 24px 64px', maxWidth: 1600, margin: '0 auto' }}>
        {activeLabel && !loading && (
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: 24, display: 'flex', alignItems: 'center', gap: 12 }}>
            Results for {activeLabel}
            <span style={{ fontSize: '0.85rem', fontWeight: 400, color: 'var(--text-secondary)' }}>
              {pins.length} pin{pins.length !== 1 ? 's' : ''}
            </span>
          </h2>
        )}

        {loading && (
          <div style={{ textAlign: 'center', padding: 80, color: 'var(--text-secondary)', fontSize: '1.1rem' }}>
            🔍 Searching…
          </div>
        )}

        {error && (
          <div style={{ textAlign: 'center', padding: 40, color: '#e60023', fontWeight: 600 }}>{error}</div>
        )}

        {!loading && !error && activeLabel && pins.length === 0 && (
          <div style={{ textAlign: 'center', padding: 80, color: 'var(--text-secondary)', fontSize: '1.1rem' }}>
            No pins found for {activeLabel}. Try a different keyword or tag!
          </div>
        )}

        {!loading && !error && !activeLabel && (
          <div style={{ textAlign: 'center', padding: 80, color: 'var(--text-secondary)', fontSize: '1.1rem' }}>
            Type something above or click a tag to explore ideas ✨
          </div>
        )}

        {!loading && pins.length > 0 && (
          <MasonryGrid staticPins={pins.map(p => ({
            pinId: p.pinId,
            title: p.title,
            description: p.description || '',
            pinUrl: p.pinUrl,
            downloadUrl: p.downloadUrl || p.pinUrl,
          }))} />
        )}
      </div>
    </div>
  );
};

export default Search;
