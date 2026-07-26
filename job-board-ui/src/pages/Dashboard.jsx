import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MapPin, Building2, ExternalLink, Key, ShieldCheck, X } from 'lucide-react';

export default function Dashboard() {
  const [jobs, setJobs] = useState([]);
  const [searchTerm, setSearchTerm] = useState('Anthropic');
  const [loading, setLoading] = useState(false);
  const [apiKey, setApiKey] = useState(localStorage.getItem('job_finder_api_key') || '');
  const [error, setError] = useState(null);
  
  const [showRegModal, setShowRegModal] = useState(false);
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [registering, setRegistering] = useState(false);

  useEffect(() => {
    if (apiKey) localStorage.setItem('job_finder_api_key', apiKey);
  }, [apiKey]);

  const fetchJobs = async () => {
    if (!apiKey) {
      setError('System restricted: Valid Gateway API Key required.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?search=${encodeURIComponent(searchTerm)}&page=0&size=10`, 
        { headers: { 'X-API-KEY': apiKey } }
      );
      if (!response.ok) throw new Error(`Gateway Error ${response.status}: Unauthorized or service down.`);
      const data = await response.json();
      setJobs(data.content || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setRegistering(true);
    setError(null);
    try {
      const response = await fetch('http://localhost:8080/api/v1/clients/register', {
        method: 'POST',
        headers: { 'X-Admin-Key': 'super-secret-admin-password-123!', 'Content-Type': 'application/json' },
        body: JSON.stringify({ companyName, email, tierType: 'FREE' })
      });
      if (!response.ok) throw new Error('Registration failed.');
      const data = await response.json();
      setApiKey(data.apiKey);
      setShowRegModal(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setRegistering(false);
    }
  };

  return (
    <div className="pt-24 pb-24 px-6 max-w-5xl mx-auto min-h-screen">
      
      {/* Control Panel */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 mb-8">
        <div>
          <h2 className="text-3xl font-bold text-white tracking-tight">Query Console</h2>
          <p className="text-slate-400">Search the active PostgreSQL pipeline.</p>
        </div>
        
        <button 
          onClick={() => setShowRegModal(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/5 border border-white/10 text-sm font-medium text-slate-200 hover:bg-white/10 transition-colors"
        >
          <Key className="w-4 h-4 text-emerald-400" />
          {apiKey ? 'Manage Key' : 'Authenticate System'}
        </button>
      </div>

      {/* Search Input Card */}
      <motion.div 
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white/5 backdrop-blur-md p-3 rounded-2xl border border-white/10 flex flex-col md:flex-row gap-2"
      >
        <div className="flex-1 flex items-center gap-3 px-4 py-2 bg-slate-950/50 rounded-xl border border-transparent focus-within:border-blue-500/50 transition-colors">
          <Search className="w-5 h-5 text-slate-400" />
          <input 
            type="text" 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search company, title, or stack..."
            className="w-full bg-transparent text-white outline-none placeholder-slate-500"
            onKeyDown={(e) => e.key === 'Enter' && fetchJobs()}
          />
        </div>
        <button 
          onClick={fetchJobs} 
          disabled={loading}
          className="bg-blue-600 hover:bg-blue-500 text-white font-semibold px-8 py-3 rounded-xl transition-all disabled:opacity-50 flex items-center justify-center"
        >
          {loading ? <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" /> : 'Execute Query'}
        </button>
      </motion.div>

      {error && (
        <div className="mt-4 bg-red-500/10 text-red-400 border border-red-500/20 px-4 py-3 rounded-xl text-sm">
          {error}
        </div>
      )}

      {/* Results Grid */}
      <motion.div className="mt-8 grid gap-4">
        {jobs.map((job, idx) => (
          <motion.div 
            key={job.atsJobId || idx} 
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: idx * 0.05 }}
            className="bg-white/5 p-6 rounded-2xl border border-white/10 hover:border-blue-500/30 hover:bg-white/[0.07] transition-all group"
          >
            <div className="flex flex-col md:flex-row justify-between gap-4">
              <div>
                <h4 className="text-lg font-bold text-white group-hover:text-blue-400 transition-colors">{job.title}</h4>
                <div className="flex flex-wrap gap-4 mt-2 text-xs font-medium text-slate-400">
                  <span className="flex items-center gap-1.5"><Building2 className="w-3.5 h-3.5 text-slate-500" /> {job.companyName}</span>
                  <span className="flex items-center gap-1.5"><MapPin className="w-3.5 h-3.5 text-slate-500" /> {job.location || 'Remote'}</span>
                </div>
              </div>
              <a href={job.url} target="_blank" rel="noopener noreferrer" className="flex items-center justify-center gap-2 bg-white/10 hover:bg-white text-white hover:text-slate-900 px-5 py-2 rounded-xl text-sm font-semibold transition-all">
                Apply <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </div>
          </motion.div>
        ))}
      </motion.div>

      {/* Registration Modal */}
      <AnimatePresence>
        {showRegModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <motion.div 
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="bg-slate-900 border border-white/10 w-full max-w-md p-6 rounded-3xl relative"
            >
              <button onClick={() => setShowRegModal(false)} className="absolute top-5 right-5 text-slate-400 hover:text-white"><X className="w-5 h-5" /></button>
              <ShieldCheck className="w-8 h-8 text-blue-400 mb-4" />
              <h3 className="text-xl font-bold text-white mb-6">Gateway Authentication</h3>
              
              <form onSubmit={handleRegister} className="space-y-4">
                <input required type="text" value={companyName} onChange={(e) => setCompanyName(e.target.value)} placeholder="Company Name" className="w-full bg-black/50 border border-white/10 rounded-xl px-4 py-3 text-white outline-none focus:border-blue-500" />
                <input required type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email Address" className="w-full bg-black/50 border border-white/10 rounded-xl px-4 py-3 text-white outline-none focus:border-blue-500" />
                <button type="submit" disabled={registering} className="w-full py-3 bg-white text-black font-bold rounded-xl hover:bg-slate-200 transition-colors">
                  {registering ? 'Generating...' : 'Generate Access Token'}
                </button>
              </form>

              <div className="mt-6 pt-6 border-t border-white/10">
                <p className="text-xs text-slate-400 mb-2">Or paste existing token:</p>
                <input type="text" value={apiKey} onChange={(e) => setApiKey(e.target.value)} placeholder="UUID-KEY..." className="w-full bg-black/50 border border-white/10 rounded-xl px-4 py-2 text-xs font-mono text-white outline-none" />
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}