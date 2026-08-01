import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MapPin, Briefcase, Filter, Building, Layers, TrendingUp, ChevronRight } from 'lucide-react';
import { useJobs } from '../hooks/useJobs';
import JobCard from '../components/JobCard';
import JobSkeleton from '../components/JobSkeleton';
import JobModal from '../components/JobModal';

export default function Dashboard() {
  const { jobs, loading, error, fetchJobs } = useJobs();
  
  const [searchTerm, setSearchTerm] = useState('');
  const [filters, setFilters] = useState({ 
    location: '', type: '', sort: 'recent', company: '', category: '' 
  });
  const [selectedJob, setSelectedJob] = useState(null); 

  useEffect(() => {
    fetchJobs(searchTerm, filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      fetchJobs(searchTerm, filters);
    }, 500);
    return () => clearTimeout(delayDebounceFn);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchTerm]);

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  return (
    <div className="pt-24 pb-16 px-6 max-w-7xl mx-auto min-h-screen relative z-10">
      
      {/* Header Section */}
      <div className="mb-8">
        <h2 className="text-3xl font-black text-slate-900 tracking-tight">Discover Opportunities</h2>
        <p className="text-slate-500 mt-1 text-sm font-medium">Browse and filter real-time job openings.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* LEFT COLUMN: Filters (25%) */}
        <div className="lg:col-span-3 space-y-5">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm sticky top-24">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-4">
              <Filter className="w-4 h-4" /> Filters
            </h3>
            
            <div className="space-y-4">
              <div>
                <label className="text-xs font-semibold text-slate-500 mb-1.5 block">Location</label>
                <div className="flex items-center gap-2 px-3 py-2.5 bg-slate-50 border border-slate-200/80 rounded-xl focus-within:border-sky-400 transition-colors">
                  <MapPin className="w-4 h-4 text-slate-400" />
                  <select value={filters.location} onChange={(e) => handleFilterChange('location', e.target.value)} className="bg-transparent outline-none cursor-pointer w-full text-sm font-medium text-slate-700">
                    <option value="">All Locations</option>
                    <option value="Remote">Remote</option>
                    <option value="Dubai">Dubai, UAE</option>
                    <option value="Riyadh">Riyadh, KSA</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-500 mb-1.5 block">Employment Type</label>
                <div className="flex items-center gap-2 px-3 py-2.5 bg-slate-50 border border-slate-200/80 rounded-xl focus-within:border-sky-400 transition-colors">
                  <Briefcase className="w-4 h-4 text-slate-400" />
                  <select value={filters.type} onChange={(e) => handleFilterChange('type', e.target.value)} className="bg-transparent outline-none cursor-pointer w-full text-sm font-medium text-slate-700">
                    <option value="">All Types</option>
                    <option value="FULL_TIME">Full-Time</option>
                    <option value="CONTRACT">Contract</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-500 mb-1.5 block">Department</label>
                <div className="flex items-center gap-2 px-3 py-2.5 bg-slate-50 border border-slate-200/80 rounded-xl focus-within:border-sky-400 transition-colors">
                  <Layers className="w-4 h-4 text-slate-400" />
                  <select value={filters.category} onChange={(e) => handleFilterChange('category', e.target.value)} className="bg-transparent outline-none cursor-pointer w-full text-sm font-medium text-slate-700">
                    <option value="">All Roles</option>
                    <option value="Engineering">Engineering</option>
                    <option value="Product">Product</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* CENTER COLUMN: Search & Grid (50%) */}
        <div className="lg:col-span-6 space-y-6">
          <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="bg-white p-2 rounded-[1.25rem] border border-slate-200 shadow-sm flex flex-col md:flex-row gap-2 relative z-20">
            <div className="flex-1 flex items-center gap-2.5 px-4 py-2 bg-slate-50 rounded-xl border border-slate-200/50 focus-within:border-sky-500/40 focus-within:ring-4 focus-within:ring-sky-500/10 focus-within:bg-white transition-all duration-300">
              <Search className="w-5 h-5 text-slate-400" />
              <input 
                type="text" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search roles, companies, or keywords..."
                className="w-full bg-transparent text-slate-900 text-sm font-medium outline-none placeholder-slate-400"
                onKeyDown={(e) => e.key === 'Enter' && fetchJobs(searchTerm, filters)}
              />
            </div>
          </motion.div>

          {error && <div className="bg-red-50 text-red-600 border border-red-100 px-5 py-4 rounded-xl text-sm font-medium">{error}</div>}

          {loading ? (
            <JobSkeleton />
          ) : (
            <div className="grid gap-4">
              {jobs.map((job, idx) => (
                <JobCard key={job.atsJobId || job.id || idx} job={job} index={idx} onClick={() => setSelectedJob(job)} />
              ))}
              {!loading && jobs.length === 0 && !error && (
                 <div className="text-center py-16 text-slate-400 text-sm font-medium border-2 border-dashed border-slate-200 rounded-2xl mt-4 bg-white/50">
                   No open roles match your current parameters.
                 </div>
              )}
            </div>
          )}
        </div>

        {/* RIGHT COLUMN: Consumer Market Insights (25%) */}
        <div className="lg:col-span-3 space-y-5">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm sticky top-24">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2 mb-5">
              <TrendingUp className="w-4 h-4 text-blue-500" /> Market Insights
            </h3>
            
            <div className="space-y-6">
              <div>
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-3">Trending Roles</p>
                <ul className="space-y-3">
                  <li className="flex items-center justify-between group cursor-pointer">
                    <span className="text-sm font-medium text-slate-700 group-hover:text-blue-600 transition-colors">Backend Engineer</span>
                    <ChevronRight className="w-3 h-3 text-slate-300 group-hover:text-blue-500 transition-colors" />
                  </li>
                  <li className="flex items-center justify-between group cursor-pointer">
                    <span className="text-sm font-medium text-slate-700 group-hover:text-blue-600 transition-colors">Product Manager</span>
                    <ChevronRight className="w-3 h-3 text-slate-300 group-hover:text-blue-500 transition-colors" />
                  </li>
                  <li className="flex items-center justify-between group cursor-pointer">
                    <span className="text-sm font-medium text-slate-700 group-hover:text-blue-600 transition-colors">Data Scientist</span>
                    <ChevronRight className="w-3 h-3 text-slate-300 group-hover:text-blue-500 transition-colors" />
                  </li>
                </ul>
              </div>

              <div className="pt-5 border-t border-slate-100">
                <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-3">Top Hiring Companies</p>
                <div className="flex flex-wrap gap-2">
                  <span className="px-3 py-1 bg-slate-50 border border-slate-200 rounded-full text-xs font-medium text-slate-600">Careem</span>
                  <span className="px-3 py-1 bg-slate-50 border border-slate-200 rounded-full text-xs font-medium text-slate-600">Talabat</span>
                  <span className="px-3 py-1 bg-slate-50 border border-slate-200 rounded-full text-xs font-medium text-slate-600">Noon</span>
                </div>
              </div>
            </div>
            
            <div className="mt-6 pt-5 border-t border-slate-100">
              <button className="w-full bg-blue-50 hover:bg-blue-100 text-blue-600 text-xs font-bold py-2.5 rounded-xl transition-colors">
                Set up Job Alerts
              </button>
            </div>
          </div>
        </div>
      </div>

      <AnimatePresence>
        {selectedJob && (
          <JobModal job={selectedJob} onClose={() => setSelectedJob(null)} />
        )}
      </AnimatePresence>
    </div>
  );
}