import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MapPin, Building2, Briefcase, ExternalLink, Key, Sparkles, ShieldCheck, ArrowRight, X } from 'lucide-react';

function App() {
  const [jobs, setJobs] = useState([]);
  const [searchTerm, setSearchTerm] = useState('Anthropic');
  const [loading, setLoading] = useState(false);
  const [apiKey, setApiKey] = useState(localStorage.getItem('job_finder_api_key') || '');
  const [error, setError] = useState(null);
  
  // Registration Modal State
  const [showRegModal, setShowRegModal] = useState(false);
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [registering, setRegistering] = useState(false);
  const [regSuccess, setRegSuccess] = useState(null);

  // Save API key to local storage so it persists
  useEffect(() => {
    if (apiKey) {
      localStorage.setItem('job_finder_api_key', apiKey);
    }
  }, [apiKey]);

  const fetchJobs = async () => {
    if (!apiKey) {
      setError('Please provide an API Key using the "Get API Key" button above.');
      return;
    }
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?search=${encodeURIComponent(searchTerm)}&page=0&size=10`, 
        { headers: { 'X-API-KEY': apiKey } }
      );

      if (!response.ok) throw new Error(`Gateway returned error status: ${response.status}. Check your API Key.`);

      const data = await response.json();
      setJobs(data.content || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch jobs. Is your backend running?');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setRegistering(true);
    setRegSuccess(null);
    setError(null);

    try {
      const response = await fetch('http://localhost:8080/api/v1/clients/register', {
        method: 'POST',
        headers: {
          'X-Admin-Key': 'super-secret-admin-password-123!',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ companyName, email, tierType: 'FREE' })
      });

      if (!response.ok) throw new Error('Registration failed. Check admin credentials.');

      const data = await response.json();
      setApiKey(data.apiKey);
      setRegSuccess(data.apiKey);
      setCompanyName('');
      setEmail('');
    } catch (err) {
      setError(err.message);
    } finally {
      setRegistering(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans selection:bg-blue-500 selection:text-white pb-24">
      
      {/* Background Glow Effects */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[350px] bg-gradient-to-tr from-blue-600/20 via-indigo-600/20 to-purple-600/20 blur-[120px] pointer-events-none rounded-full" />

      <div className="relative max-w-5xl mx-auto px-6 pt-16 space-y-10">
        
        {/* Navigation / Header Bar */}
        <motion.div 
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col md:flex-row items-center justify-between gap-4 border-b border-slate-800/80 pb-6"
        >
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-2xl shadow-lg shadow-blue-500/20">
              <Briefcase className="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold tracking-tight text-white">NexusJob</h2>
              <p className="text-xs text-slate-400">Distributed Intelligence Engine</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-900 border border-slate-800 text-xs text-slate-400">
              <span className={`w-2 h-2 rounded-full ${apiKey ? 'bg-emerald-500 shadow-lg shadow-emerald-500/50' : 'bg-amber-500'}`} />
              {apiKey ? 'API Key Active' : 'Key Required'}
            </div>
            
            <button 
              onClick={() => setShowRegModal(true)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 border border-slate-700/60 text-sm font-medium text-slate-200 transition-all hover:scale-105 active:scale-95 shadow-sm"
            >
              <Key className="w-4 h-4 text-blue-400" /> Manage API Key
            </button>
          </div>
        </motion.div>

        {/* Hero Banner & Search Console */}
        <div className="space-y-6 text-center max-w-2xl mx-auto">
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-semibold tracking-wide uppercase"
          >
            <Sparkles className="w-3.5 h-3.5" /> Powered by Spring Boot & PostgreSQL
          </motion.div>
          
          <motion.h1 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-4xl md:text-5xl font-black tracking-tight text-white leading-tight"
          >
            Discover high-impact roles across the tech frontier.
          </motion.h1>

          {/* Search Box Card */}
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="bg-slate-900/80 backdrop-blur-xl p-3 rounded-2xl shadow-2xl shadow-black/50 border border-slate-800 flex flex-col md:flex-row gap-2 mt-8"
          >
            <div className="flex-1 flex items-center gap-3 px-4 py-2 bg-slate-950/50 rounded-xl border border-slate-800/80 focus-within:border-blue-500 transition-all">
              <Search className="w-5 h-5 text-slate-400" />
              <input 
                type="text" 
                value={searchTerm} 
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search company, title, or stack..."
                className="w-full bg-transparent text-white placeholder-slate-500 outline-none text-sm md:text-base"
                onKeyDown={(e) => e.key === 'Enter' && fetchJobs()}
              />
            </div>
            
            <button 
              onClick={fetchJobs} 
              disabled={loading}
              className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold px-8 py-3 rounded-xl transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2 shadow-lg shadow-blue-600/30 cursor-pointer"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>Search <ArrowRight className="w-4 h-4" /></>
              )}
            </button>
          </motion.div>

          {error && (
            <motion.div 
              initial={{ opacity: 0, height: 0 }} 
              animate={{ opacity: 1, height: 'auto' }} 
              className="bg-red-500/10 text-red-400 border border-red-500/20 px-4 py-3 rounded-xl text-sm text-left"
            >
              {error}
            </motion.div>
          )}
        </div>

        {/* Results Grid / Feed */}
        {jobs.length > 0 && (
          <motion.div 
            initial="hidden"
            animate="show"
            variants={{ hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.08 } } }}
            className="space-y-4 pt-6"
          >
            <div className="flex items-center justify-between px-2">
              <h3 className="text-sm font-semibold tracking-wider text-slate-400 uppercase">Live Pipeline Results</h3>
              <span className="text-xs font-bold text-blue-400 bg-blue-500/10 border border-blue-500/20 px-3 py-1 rounded-full">
                {jobs.length} Matches Found
              </span>
            </div>

            <div className="grid gap-4">
              {jobs.map((job) => (
                <motion.div 
                  key={job.atsJobId || job.id} 
                  variants={{ hidden: { opacity: 0, y: 15 }, show: { opacity: 1, y: 0 } }}
                  whileHover={{ scale: 1.01, translateY: -2 }}
                  className="bg-slate-900/60 backdrop-blur-md p-6 rounded-2xl border border-slate-800/80 hover:border-slate-700 shadow-xl shadow-black/20 transition-all group relative overflow-hidden"
                >
                  <div className="absolute top-0 left-0 w-1 h-full bg-gradient-to-b from-blue-500 to-indigo-600 opacity-0 group-hover:opacity-100 transition-opacity" />
                  
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="space-y-2">
                      <h4 className="text-lg font-bold text-white group-hover:text-blue-400 transition-colors">
                        {job.title}
                      </h4>
                      <div className="flex flex-wrap items-center gap-4 text-xs font-medium text-slate-400">
                        <span className="flex items-center gap-1.5 text-slate-300 bg-slate-800/60 px-2.5 py-1 rounded-lg border border-slate-700/50">
                          <Building2 className="w-3.5 h-3.5 text-blue-400" /> {job.companyName}
                        </span>
                        <span className="flex items-center gap-1.5 bg-slate-800/60 px-2.5 py-1 rounded-lg border border-slate-700/50">
                          <MapPin className="w-3.5 h-3.5 text-indigo-400" /> {job.location || 'Remote / Global'}
                        </span>
                      </div>
                    </div>
                    
                    <a 
                      href={job.url} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="inline-flex items-center justify-center gap-2 bg-slate-800 hover:bg-blue-600 text-slate-200 hover:text-white px-4 py-2.5 rounded-xl text-sm font-semibold transition-all shadow-sm active:scale-95 whitespace-nowrap border border-slate-700/80 hover:border-blue-500"
                    >
                      View Role <ExternalLink className="w-3.5 h-3.5" />
                    </a>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}

      </div>

      {/* Registration Modal Popup / Slide-Over */}
      <AnimatePresence>
        {showRegModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
            <motion.div 
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="bg-slate-900 border border-slate-800 w-full max-w-md p-6 rounded-3xl shadow-2xl relative space-y-6"
            >
              <button 
                onClick={() => setShowRegModal(false)}
                className="absolute top-5 right-5 text-slate-400 hover:text-white bg-slate-800 p-2 rounded-full transition-colors"
              >
                <X className="w-4 h-4" />
              </button>

              <div className="space-y-1">
                <div className="inline-flex p-2.5 bg-blue-500/10 text-blue-400 rounded-xl mb-2">
                  <ShieldCheck className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-bold text-white">Client API Key Management</h3>
                <p className="text-xs text-slate-400">Register instantly with your backend gateway to generate a live credential.</p>
              </div>

              <form onSubmit={handleRegister} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">Company Name</label>
                  <input 
                    type="text" 
                    required
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    placeholder="e.g. MyStartup Inc"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:border-blue-500 outline-none transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-300">Developer Email</label>
                  <input 
                    type="email" 
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="dev@mystartup.com"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-white focus:border-blue-500 outline-none transition-all"
                  />
                </div>

                <button 
                  type="submit"
                  disabled={registering}
                  className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl text-sm transition-all shadow-lg shadow-blue-600/30 cursor-pointer disabled:opacity-50 mt-2"
                >
                  {registering ? 'Generating Key...' : 'Generate New API Key'}
                </button>
              </form>

              {regSuccess && (
                <div className="space-y-2 p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
                  <p className="text-xs font-semibold text-emerald-400">Success! Your key has been auto-applied:</p>
                  <div className="bg-slate-950 p-2 rounded text-xs font-mono text-slate-300 break-all select-all">
                    {regSuccess}
                  </div>
                </div>
              )}

              <div className="pt-2 border-t border-slate-800 space-y-2">
                <label className="text-xs font-semibold text-slate-400">Or Manually Paste Existing Key:</label>
                <input 
                  type="text" 
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                  placeholder="Paste UUID key here..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2 text-xs font-mono text-slate-300 focus:border-blue-500 outline-none"
                />
              </div>

            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}

export default App;