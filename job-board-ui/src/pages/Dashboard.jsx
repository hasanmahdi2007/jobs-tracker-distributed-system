import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MapPin, Building2, ExternalLink, Key, ShieldCheck, X, User } from 'lucide-react';

export default function Dashboard() {
  const [jobs, setJobs] = useState([]);
  const [searchTerm, setSearchTerm] = useState('Anthropic');
  const [loading, setLoading] = useState(false);
  const [apiKey, setApiKey] = useState(localStorage.getItem('job_finder_api_key') || '');
  const [isGuest, setIsGuest] = useState(false);
  const [error, setError] = useState(null);
  
  const [showRegModal, setShowRegModal] = useState(false);
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [registering, setRegistering] = useState(false);

  useEffect(() => {
    if (apiKey && apiKey !== 'guest') localStorage.setItem('job_finder_api_key', apiKey);
  }, [apiKey]);

  const fetchJobs = async () => {
    if (!apiKey && !isGuest) {
      setShowRegModal(true);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const headerKey = isGuest ? 'GUEST-ACCESS-TOKEN' : apiKey; 
      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?search=${encodeURIComponent(searchTerm)}&page=0&size=10`, 
        { headers: { 'X-API-KEY': headerKey } }
      );
      if (!response.ok) throw new Error(`Gateway Error ${response.status}: Unauthorized.`);
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
      setIsGuest(false);
      setShowRegModal(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setRegistering(false);
    }
  };

  const handleGuestAccess = () => {
    setIsGuest(true);
    setApiKey('guest');
    setShowRegModal(false);
  };

  // Modern Staggered Spring Animations
  const containerVars = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: { staggerChildren: 0.08 } }
  };

  const itemVars = {
    hidden: { opacity: 0, y: 20, scale: 0.95 },
    show: { opacity: 1, y: 0, scale: 1, transition: { type: "spring", stiffness: 300, damping: 24 } }
  };

  return (
    <div className="pt-32 pb-24 px-6 max-w-5xl mx-auto min-h-screen">
      
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex flex-col md:flex-row items-center justify-between gap-4 mb-10"
      >
        <div>
          <h2 className="text-4xl font-black text-slate-900 tracking-tight">Query Console</h2>
          <p className="text-slate-500 mt-1 font-medium">Live connection to PostgreSQL grid.</p>
        </div>
        
        <button 
          onClick={() => setShowRegModal(true)}
          className="flex items-center gap-2 px-5 py-2.5 rounded-full bg-white border border-slate-200 text-sm font-semibold text-slate-700 hover:border-slate-300 hover:shadow-[0_4px_12px_rgb(0,0,0,0.05)] transition-all active:scale-95"
        >
          <Key className="w-4 h-4 text-blue-500" />
          {apiKey === 'guest' ? 'Upgrade from Guest' : apiKey ? 'Manage Access' : 'Authenticate'}
        </button>
      </motion.div>

      {/* Search Input Card - Raycast Style */}
      <motion.div 
        initial={{ opacity: 0, scale: 0.98 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ type: "spring", stiffness: 400, damping: 30, delay: 0.1 }}
        className="bg-white p-3 rounded-[1.5rem] border border-slate-200/80 shadow-[0_8px_30px_rgb(0,0,0,0.04)] flex flex-col md:flex-row gap-3 relative z-20"
      >
        <div className="flex-1 flex items-center gap-3 px-5 py-3 bg-slate-50 rounded-xl border border-slate-200/50 focus-within:border-blue-500/40 focus-within:ring-4 focus-within:ring-blue-500/10 focus-within:bg-white transition-all duration-300">
          <Search className="w-5 h-5 text-slate-400" />
          <input 
            type="text" 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search roles, companies, or keywords..."
            className="w-full bg-transparent text-slate-900 font-medium outline-none placeholder-slate-400"
            onKeyDown={(e) => e.key === 'Enter' && fetchJobs()}
          />
        </div>
        <button 
          onClick={fetchJobs} 
          disabled={loading}
          className="bg-slate-900 hover:bg-slate-800 text-white font-semibold px-8 py-3.5 rounded-xl transition-all disabled:opacity-50 flex items-center justify-center shadow-md shadow-slate-900/10 active:scale-95"
        >
          {loading ? <div className="w-5 h-5 border-2 border-slate-300 border-t-white rounded-full animate-spin" /> : 'Execute Query'}
        </button>
      </motion.div>

      {error && (
        <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} className="mt-6 bg-red-50 text-red-600 border border-red-100 px-5 py-4 rounded-2xl text-sm font-medium">
          {error}
        </motion.div>
      )}

      {/* Results Grid - Staggered Spring Animation */}
      <motion.div 
        variants={containerVars}
        initial="hidden"
        animate="show"
        className="mt-8 grid gap-4"
      >
        {jobs.map((job, idx) => (
          <motion.div 
            key={job.atsJobId || idx} 
            variants={itemVars}
            whileHover={{ scale: 1.01, y: -2 }}
            className="group bg-white p-6 rounded-[1.5rem] border border-slate-200/80 hover:border-blue-200 shadow-sm hover:shadow-[0_8px_30px_rgb(0,0,0,0.06)] transition-all duration-300"
          >
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
              <div>
                <h4 className="text-xl font-bold text-slate-900 group-hover:text-blue-600 transition-colors tracking-tight">{job.title}</h4>
                <div className="flex flex-wrap gap-4 mt-3 text-sm font-medium text-slate-500">
                  <span className="flex items-center gap-2 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-100"><Building2 className="w-4 h-4 text-slate-400" /> {job.companyName}</span>
                  <span className="flex items-center gap-2 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-100"><MapPin className="w-4 h-4 text-slate-400" /> {job.location || 'Remote'}</span>
                </div>
              </div>
              <a href={job.url} target="_blank" rel="noopener noreferrer" className="flex items-center justify-center gap-2 bg-white border border-slate-200 text-slate-700 hover:bg-slate-900 hover:text-white hover:border-slate-900 px-6 py-2.5 rounded-xl text-sm font-semibold transition-all shadow-sm active:scale-95 shrink-0 w-full md:w-auto">
                Apply <ExternalLink className="w-4 h-4" />
              </a>
            </div>
          </motion.div>
        ))}
      </motion.div>

      {/* Modern Modal - Spring Popup */}
      <AnimatePresence>
        {showRegModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/20 backdrop-blur-sm">
            <motion.div 
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              transition={{ type: "spring", stiffness: 400, damping: 30 }}
              className="bg-white border border-slate-200 shadow-[0_20px_60px_rgb(0,0,0,0.1)] w-full max-w-md p-8 rounded-[2rem] relative"
            >
              <button onClick={() => setShowRegModal(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-900 bg-slate-50 hover:bg-slate-100 p-2 rounded-full transition-colors"><X className="w-5 h-5" /></button>
              
              <div className="w-12 h-12 bg-blue-50 flex items-center justify-center rounded-2xl mb-6">
                <ShieldCheck className="w-6 h-6 text-blue-600" />
              </div>
              <h3 className="text-2xl font-bold text-slate-900 mb-2 tracking-tight">Gateway Access</h3>
              <p className="text-slate-500 mb-8 font-medium">Create a token or continue as a guest.</p>
              
              <form onSubmit={handleRegister} className="space-y-3">
                <input required type="text" value={companyName} onChange={(e) => setCompanyName(e.target.value)} placeholder="Workspace Name" className="w-full bg-slate-50 border border-slate-200 rounded-xl px-5 py-3.5 text-slate-900 font-medium outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10 transition-all" />
                <input required type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Admin Email" className="w-full bg-slate-50 border border-slate-200 rounded-xl px-5 py-3.5 text-slate-900 font-medium outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10 transition-all" />
                <button type="submit" disabled={registering} className="w-full py-3.5 mt-2 bg-slate-900 text-white font-semibold rounded-xl hover:bg-slate-800 active:scale-[0.98] transition-all shadow-md shadow-slate-900/10">
                  {registering ? 'Generating...' : 'Generate Secure Token'}
                </button>
              </form>

              <div className="relative mt-8 mb-6">
                <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-100"></div></div>
                <div className="relative flex justify-center"><span className="bg-white px-4 text-xs font-bold text-slate-300 uppercase tracking-wider">OR</span></div>
              </div>

              <button 
                onClick={handleGuestAccess}
                className="w-full flex items-center justify-center gap-2 py-3.5 bg-white border border-slate-200 text-slate-700 font-semibold rounded-xl hover:bg-slate-50 hover:border-slate-300 active:scale-[0.98] transition-all"
              >
                <User className="w-5 h-5 text-slate-400" />
                Continue as Guest
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}