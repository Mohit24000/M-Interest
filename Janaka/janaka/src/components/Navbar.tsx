import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Camera, Search, User, Menu, X, Upload, ChevronLeft } from 'lucide-react';
import UploadModal from './UploadModal';
import './Navbar.css';

const Navbar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [scrolled, setScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  useEffect(() => {
    const fetchSuggestions = async () => {
      if (searchQuery.trim().length < 2) {
        setSuggestions([]);
        return;
      }
      try {
        const res = await fetch(`/api/search/suggestions?q=${encodeURIComponent(searchQuery)}`);
        if (res.ok) {
          const data = await res.json();
          setSuggestions(data);
        }
      } catch (err) {
        console.error('Failed to fetch suggestions', err);
      }
    };

    const timeoutId = setTimeout(fetchSuggestions, 300);
    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const handleSearchSubmit = (query?: string | React.KeyboardEvent<HTMLInputElement>) => {
    let q = '';
    if (typeof query === 'string') {
      q = query;
    } else if (query && 'key' in query) {
      if (query.key !== 'Enter') return;
      q = searchQuery;
    } else {
      q = searchQuery;
    }

    if (q.trim()) {
      navigate('/search?q=' + encodeURIComponent(q));
      setShowSuggestions(false);
    }
  };

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <nav className={`navbar ${scrolled ? 'navbar-scrolled' : ''}`}>
      <div className="navbar-container">
        {location.pathname !== '/' && (
           <button className="btn-icon" onClick={() => navigate(-1)} style={{ marginRight: '16px', background: 'var(--bg-tertiary)', borderRadius: '50%', padding: '8px' }}>
             <ChevronLeft size={24} />
           </button>
        )}
        <Link to="/" className="navbar-logo">
          <Camera className="logo-icon" size={28} />
          <span className="logo-text">॥ जनक ॥</span>
        </Link>
        
        <div className="navbar-search desktop-only">
          <Search className="search-icon" size={18} />
          <input 
            type="text" 
            placeholder="Search amazing ideas..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={handleSearchSubmit}
            onFocus={() => setShowSuggestions(true)}
            onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
          />
          {showSuggestions && suggestions.length > 0 && (
            <div className="search-suggestions">
              {suggestions.map((s, i) => (
                <div 
                  key={i} 
                  className="suggestion-item"
                  onClick={() => {
                    setSearchQuery(s);
                    handleSearchSubmit(s);
                  }}
                >
                  <Search size={14} style={{ marginRight: 10, opacity: 0.6 }} />
                  {s}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="navbar-actions desktop-only">
          <button className="btn btn-primary" onClick={() => setIsUploadOpen(true)}>
            <Upload size={18} />
            <span>Upload</span>
          </button>
          <button className="btn btn-icon" onClick={() => navigate('/profile')}>
            <User size={20} />
          </button>
        </div>

        <button 
          className="mobile-menu-btn mobile-only"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
        >
          {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* Mobile Menu */}
      <div className={`mobile-menu ${isMobileMenuOpen ? 'open' : ''}`}>
        <div className="navbar-search mobile-search">
          <Search className="search-icon" size={18} />
          <input type="text" placeholder="Search..." />
        </div>
        <button className="btn btn-primary w-full mt-4" onClick={() => setIsUploadOpen(true)}>
          <Upload size={18} />
          <span>Upload Image</span>
        </button>
        <button className="btn w-full mt-2" style={{ background: 'var(--bg-tertiary)', color: 'white' }} onClick={() => { navigate('/profile'); setIsMobileMenuOpen(false); }}>
          <User size={18} />
          <span>Profile</span>
        </button>
      </div>

      <UploadModal isOpen={isUploadOpen} onClose={() => setIsUploadOpen(false)} />
    </nav>
  );
};

export default Navbar;
