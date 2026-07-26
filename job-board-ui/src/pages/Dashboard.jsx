import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, Key, ShieldCheck, X, User } from 'lucide-react';
import { useJobs } from '../hooks/useJobs';
import JobCard from '../components/JobCard';
import JobSkeleton from '../components/JobSkeleton';

export default function Dashboard() {
  const [searchTerm, setSearchTerm] = useState('Anthropic');
  const [apiKey, setApiKey] = useState(localStorage.getItem('job_finder_api_key') || '');
  const [isGuest, setIsGuest] = useState(false);
  const [showRegModal, setShowRegModal] = useState(false);
  
  // Destructuring our custom hook!
  const { jobs, loading, error, fetchJobs } = useJobs();

  useEffect(() => {
    if (apiKey && apiKey !== 'guest') localStorage.setItem('job_finder_api_key', apiKey);
  }, [apiKey]);

  const executeSearch = () => {
    const hasAccess = fetchJobs(searchTerm, apiKey, isGuest);
    if (!hasAccess) setShowRegModal(true);
  };

  const handleGuestAccess = () => {
    setIsGuest(true);
    setApiKey('guest');
    setShowRegModal(false);
  };

  return (
    <div className="pt-32 pb-24 px-6 max-w-5xl mx-auto min-h-screen relative z-10">
      
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 mb-10">
        <div>
          <h2 className="text-4xl font-black text-slate-900 tracking-tight">Query Console</h2>
          <p className="text-slate-500 mt-1 font-medium">Live connection to PostgreSQL grid.</p>
        </div>
        
        <button 
          onClick={() => setShowRegModal(true)}
          className="flex items-center gap-2 px-5 py-2.5 rounded-full bg-white border border-slate-200 text-sm font-semibold text-slate-700 hover:border-slate-300 shadow-sm transition-all active:scale-95"
        >
          <Key className="w-4 h-4 text-sky-500" />
          {apiKey === 'guest' ? 'Upgrade from Guest' : apiKey ? 'Manage Access' : 'Authenticate'}
        </button>
      </div>

      <motion.div 
        initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
        className="bg-white p-3 rounded-[1.5rem] border border-slate-200 shadow-sm flex flex-col md:flex-row gap-3"
      >
        <div className="flex-1 flex items-center gap-3 px-5 py-3 bg-slate-50 rounded-xl border border-slate-200/50 focus-within:border-sky-500/40 focus-within:ring-4 focus-within:ring-sky-500/10 focus-within:bg-white transition-all duration-300">
          <Search className="w-5 h-5 text-slate-400" />
          <input 
            type="text" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search roles, companies, or keywords..."
            className="w-full bg-transparent text-slate-900 font-medium outline-none placeholder-slate-400"
            onKeyDown={(e) => e.key === 'Enter' && executeSearch()}
          />
        </div>
        <button 
          onClick={executeSearch} disabled={loading}
          className="bg-slate-900 hover:bg-slate-800 text-white font-semibold px-8 py-3.5 rounded-xl transition-all disabled:opacity-50 flex items-center justify-center shadow-md active:scale-95"
        >
          Execute Query
        </button>
      </motion.div>

      {error && <div className="mt-6 bg-red-50 text-red-600 border border-red-100 px-5 py-4 rounded-2xl text-sm font-medium">{error}</div>}

      {/* Conditionally render Skeleton vs Actual Cards */}
      {loading ? (
        <JobSkeleton />
      ) : (
        <div className="mt-8 grid gap-4">
          {jobs.map((job, idx) => (
            <JobCard key={job.atsJobId || idx} job={job} index={idx} />
          ))}
        </div>
      )}

      {/* Auth Modal stays the same */}
      <AnimatePresence>
        {showRegModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/20 backdrop-blur-sm">
            <motion.div 
              initial={{ opacity: 0, scale: 0.95, y: 10 }} animate={{ opacity: 1, scale: 1, y: 0 }} exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="bg-white border border-slate-200 shadow-2xl w-full max-w-md p-8 rounded-[2rem] relative"
            >
              <button onClick={() => setShowRegModal(false)} className="absolute top-6 right-6 text-slate-400 hover:text-slate-900"><X className="w-5 h-5" /></button>
              <div className="w-12 h-12 bg-sky-50 flex items-center justify-center rounded-2xl mb-6"><ShieldCheck className="w-6 h-6 text-sky-600" /></div>
              <h3 className="text-2xl font-bold text-slate-900 mb-2 tracking-tight">Gateway Access</h3>
              <p className="text-slate-500 mb-8 font-medium">Authenticate to query the system.</p>
              
              <button onClick={handleGuestAccess} className="w-full flex items-center justify-center gap-2 py-4 bg-white border border-slate-200 text-slate-700 font-bold rounded-xl hover:bg-slate-50 transition-all">
                <User className="w-5 h-5 text-slate-400" /> Continue as Guest
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}