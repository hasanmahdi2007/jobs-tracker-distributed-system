import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MapPin, Briefcase, Filter, Building, Layers } from 'lucide-react';
import { useJobs } from '../hooks/useJobs';
import JobCard from '../components/JobCard';
import JobSkeleton from '../components/JobSkeleton';
import JobModal from '../components/JobModal';

export default function Dashboard() {
  const { jobs, loading, error, fetchJobs } = useJobs();
  
  const [searchTerm, setSearchTerm] = useState('');
  const [filters, setFilters] = useState({ 
    location: '', 
    type: '', 
    sort: 'recent', 
    company: '', 
    category: '' 
  });
  const [selectedJob, setSelectedJob] = useState(null); 

  // 1. INSTANT Auto-Filter for Dropdowns
  // Runs immediately whenever a user selects an option in the filter menus
  useEffect(() => {
    fetchJobs(searchTerm, filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  // 2. DEBOUNCED Auto-Filter for the Search Bar
  // Waits 500ms after the user stops typing to prevent spamming your Spring Boot API
  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      fetchJobs(searchTerm, filters);
    }, 500);

    return () => clearTimeout(delayDebounceFn);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchTerm]);

  // Helper to cleanly update a specific filter without overwriting the others
  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  return (
    <div className="pt-24 pb-16 px-6 max-w-5xl mx-auto min-h-screen relative z-10">
      
      {/* Header Section */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 mb-8">
        <div>
          <h2 className="text-3xl font-black text-slate-900 tracking-tight">Discover Opportunities</h2>
          <p className="text-slate-500 mt-1 text-sm font-medium">Browse and filter real-time job openings.</p>
        </div>
      </div>

      {/* Main Search Bar */}
      <motion.div 
        initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
        className="bg-white p-2.5 rounded-[1.25rem] border border-slate-200 shadow-sm flex flex-col md:flex-row gap-2.5 relative z-20"
      >
        <div className="flex-1 flex items-center gap-2.5 px-4 py-2.5 bg-slate-50 rounded-xl border border-slate-200/50 focus-within:border-sky-500/40 focus-within:ring-4 focus-within:ring-sky-500/10 focus-within:bg-white transition-all duration-300">
          <Search className="w-4 h-4 text-slate-400" />
          <input 
            type="text" 
            value={searchTerm} 
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search roles, companies, or keywords..."
            className="w-full bg-transparent text-slate-900 text-sm font-medium outline-none placeholder-slate-400"
            // If they hit Enter, it bypasses the 500ms delay and searches instantly
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                fetchJobs(searchTerm, filters);
              }
            }}
          />
        </div>
        
        {/* We keep the button for users who prefer to click */}
        <button 
          onClick={() => fetchJobs(searchTerm, filters)} 
          disabled={loading}
          className="bg-slate-900 hover:bg-slate-800 text-white text-sm font-semibold px-6 py-2.5 rounded-xl transition-all disabled:opacity-50 flex items-center justify-center shadow-md active:scale-95"
        >
          Search
        </button>
      </motion.div>

      {/* The Dynamic Filter Row */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
        className="flex flex-wrap items-center gap-2 mt-4 relative z-10"
      >
        <div className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-200/80 rounded-lg text-xs font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <MapPin className="w-3.5 h-3.5 text-slate-400" />
          <select 
            value={filters.location} 
            onChange={(e) => handleFilterChange('location', e.target.value)}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Locations</option>
            <option value="Remote">Remote</option>
            <option value="Dubai">Dubai, UAE</option>
            <option value="Riyadh">Riyadh, KSA</option>
            <option value="Cairo">Cairo, Egypt</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-200/80 rounded-lg text-xs font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <Briefcase className="w-3.5 h-3.5 text-slate-400" />
          <select 
            value={filters.type} 
            onChange={(e) => handleFilterChange('type', e.target.value)}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Types</option>
            <option value="FULL_TIME">Full-Time</option>
            <option value="CONTRACT">Contract</option>
            <option value="PART_TIME">Part-Time</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-200/80 rounded-lg text-xs font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <Layers className="w-3.5 h-3.5 text-slate-400" />
          <select 
            value={filters.category} 
            onChange={(e) => handleFilterChange('category', e.target.value)}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Roles</option>
            <option value="Engineering">Engineering</option>
            <option value="Product">Product</option>
            <option value="Design">Design</option>
            <option value="Sales">Sales</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-3 py-2 bg-white border border-slate-200/80 rounded-lg text-xs font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <Building className="w-3.5 h-3.5 text-slate-400" />
          <select 
            value={filters.company} 
            onChange={(e) => handleFilterChange('company', e.target.value)}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Companies</option>
            <option value="Careem">Careem</option>
            <option value="Talabat">Talabat</option>
            <option value="Noon">Noon</option>
            <option value="Emirates">Emirates</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-3 py-2 bg-slate-50 border border-slate-200/80 rounded-lg text-xs font-medium text-slate-700 hover:border-slate-300 transition-colors shadow-sm ml-auto">
          <Filter className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-slate-400">Sort:</span>
          <select 
            value={filters.sort} 
            onChange={(e) => handleFilterChange('sort', e.target.value)}
            className="bg-transparent outline-none cursor-pointer font-bold"
          >
            <option value="recent">Newest First</option>
            <option value="relevant">Most Relevant</option>
          </select>
        </div>
      </motion.div>

      {/* Database Fetching Error Handling */}
      {error && <div className="mt-6 bg-red-50 text-red-600 border border-red-100 px-5 py-4 rounded-xl text-sm font-medium">{error}</div>}

      {/* The Core Job Grid */}
      {loading ? (
        <JobSkeleton />
      ) : (
        <div className="mt-6 grid gap-3">
          {jobs.map((job, idx) => (
            <JobCard 
              key={job.atsJobId || job.id || idx} 
              job={job} 
              index={idx} 
              onClick={() => setSelectedJob(job)} 
            />
          ))}
          {!loading && jobs.length === 0 && !error && (
             <div className="text-center py-16 text-slate-400 text-sm font-medium border-2 border-dashed border-slate-200 rounded-2xl mt-4">
               No open roles match your criteria. Try adjusting your filters.
             </div>
          )}
        </div>
      )}

      {/* The Pop-up Description Modal */}
      <AnimatePresence>
        {selectedJob && (
          <JobModal job={selectedJob} onClose={() => setSelectedJob(null)} />
        )}
      </AnimatePresence>
    </div>
  );
}