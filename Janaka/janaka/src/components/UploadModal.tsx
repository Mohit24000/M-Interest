import { useState } from 'react';
import { X, UploadCloud } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import './UploadModal.css';

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const UploadModal = ({ isOpen, onClose }: UploadModalProps) => {
  const { user } = useAuth();
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) {
      if (f.size > 3 * 1024 * 1024) {
        alert('File is too large! Please choose an image smaller than 3MB.');
        e.target.value = '';
        return;
      }
      setFile(f);
      setPreview(URL.createObjectURL(f));
    }
  };

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !title) return;
    if (!user) { alert('Please log in first!'); return; }

    setLoading(true);
    const formData = new FormData();
    formData.append('image', file);
    formData.append('title', title);
    formData.append('description', description);
    formData.append('userId', user.userId);
    
    // Append tags individually
    if (tags.trim()) {
      tags.split(',').forEach(tag => {
        const t = tag.trim();
        if (t) formData.append('tags', t);
      });
    }

    try {
      const res = await fetch('/api/pins', {
        method: 'POST',
        body: formData,
        credentials: 'include',
      });
      if (res.ok) {
        setFile(null); setPreview(null); setTitle(''); setDescription('');
        onClose();
        window.location.reload();
      } else {
        const errorText = await res.text();
        alert(`Upload failed: ${errorText}`);
      }
    } catch {
      alert('Network error. Is the backend running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal-content animate-fade-in">
        <button className="modal-close" onClick={onClose}><X size={20} /></button>
        <p className="modal-title">Create Pin</p>

        <form onSubmit={handleUpload} className="upload-form-split">
          {/* Left — image drop zone */}
          <div className="upload-dropzone-container">
            <div className="upload-dropzone">
              <input type="file" accept="image/*" required onChange={handleFileChange} />
              {preview
                ? <img src={preview} alt="preview" style={{ width:'100%', height:'100%', objectFit:'cover', borderRadius:'32px 0 0 32px' }} />
                : <>
                    <UploadCloud size={56} className="dropzone-icon" />
                    <p style={{ color:'#999', fontSize:'0.95rem', textAlign:'center', padding:'0 24px' }}>
                      Choose a file or drag and drop it here
                    </p>
                    <p style={{ color:'#bbb', fontSize:'0.8rem', marginTop:'8px' }}>
                      We recommend using high quality .jpg files less than 3 MB
                    </p>
                  </>
              }
            </div>
          </div>

          {/* Right — fields */}
          <div className="upload-fields-container">
            <div className="form-group">
              <label>Title</label>
              <input
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                required
                placeholder="Add a title"
                style={{ fontSize:'1.4rem', fontWeight:700, paddingBottom:'8px' }}
              />
            </div>
            <div className="form-group">
              <label>Description</label>
              <textarea
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="Tell everyone what your Pin is about"
              />
            </div>
            <div className="form-group">
              <label>Tags (comma separated)</label>
              <input
                type="text"
                value={tags}
                onChange={e => setTags(e.target.value)}
                placeholder="e.g. nature, photography, art"
              />
            </div>
            <div style={{ marginTop:'auto' }}>
              <button
                type="submit"
                disabled={loading}
                style={{
                  background: loading ? '#ccc' : '#e60023',
                  color: 'white',
                  border: 'none',
                  borderRadius: '24px',
                  padding: '14px 32px',
                  fontWeight: 700,
                  fontSize: '1rem',
                  cursor: loading ? 'not-allowed' : 'pointer',
                  width: '100%',
                }}
              >
                {loading ? 'Publishing...' : 'Publish'}
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UploadModal;
