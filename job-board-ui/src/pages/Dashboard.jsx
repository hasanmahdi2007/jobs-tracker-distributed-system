import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Search, MapPin, Briefcase, Filter, Building, Layers } from 'lucide-react';
import { useJobs } from '../hooks/useJobs';
import JobCard from '../components/JobCard';
import JobSkeleton from '../components/JobSkeleton';
import JobModal from '../components/JobModal';

export default function Dashboard() {
  const [searchTerm, setSearchTerm] = useState('');
  const [filters, setFilters] = useState({ location: '', type: '', sort: 'recent', company: '', category: '' });
  const [selectedJob, setSelectedJob] = useState(null); 
  
  const { jobs, loading, error, fetchJobs } = useJobs();

  // The [filters] dependency means this runs automatically whenever ANY dropdown changes!
  useEffect(() => {
    fetchJobs(searchTerm, filters);
  }, [filters]);

  const executeSearch = () => {
    fetchJobs(searchTerm, filters);
  };

  return (
    <div className="pt-32 pb-24 px-6 max-w-5xl mx-auto min-h-screen relative z-10">
      
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 mb-10">
        <div>
          <h2 className="text-4xl font-black text-slate-900 tracking-tight">Query Console</h2>
          <p className="text-slate-500 mt-1 font-medium">Live connection to PostgreSQL grid.</p>
        </div>
      </div>

      <motion.div 
        initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
        className="bg-white p-3 rounded-[1.5rem] border border-slate-200 shadow-sm flex flex-col md:flex-row gap-3 relative z-20"
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
          Search
        </button>
      </motion.div>

      {/* The Filter Row */}
      <motion.div 
        initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
        className="flex flex-wrap items-center gap-3 mt-4 relative z-10"
      >
        <div className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200/80 rounded-xl text-sm font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <MapPin className="w-4 h-4 text-slate-400" />
          <select 
            value={filters.location} 
            onChange={(e) => setFilters({ ...filters, location: e.target.value })}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Locations</option>
            <option value="Remote">Remote</option>
            <option value="San Francisco">San Francisco, CA</option>
            <option value="New York">New York, NY</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200/80 rounded-xl text-sm font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <Briefcase className="w-4 h-4 text-slate-400" />
          <select 
            value={filters.type} 
            onChange={(e) => setFilters({ ...filters, type: e.target.value })}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Types</option>
            <option value="FULL_TIME">Full-Time</option>
            <option value="CONTRACT">Contract</option>
            <option value="PART_TIME">Part-Time</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200/80 rounded-xl text-sm font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <Layers className="w-4 h-4 text-slate-400" />
          <select 
            value={filters.category} 
            onChange={(e) => setFilters({ ...filters, category: e.target.value })}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Roles</option>
            <option value="Engineering">Engineering</option>
            <option value="Product">Product</option>
            <option value="Design">Design</option>
            <option value="Marketing">Marketing</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200/80 rounded-xl text-sm font-medium text-slate-600 hover:border-sky-300 transition-colors shadow-sm">
          <Building className="w-4 h-4 text-slate-400" />
          <select 
            value={filters.company} 
            onChange={(e) => setFilters({ ...filters, company: e.target.value })}
            className="bg-transparent outline-none cursor-pointer w-full"
          >
            <option value="">All Companies</option>
            <option value="Anthropic">Anthropic</option>
            <option value="Stripe">Stripe</option>
            <option value="Google">Google</option>
            <option value="Meta">Meta</option>
          </select>
        </div>

        <div className="flex items-center gap-2 px-4 py-2.5 bg-slate-50 border border-slate-200/80 rounded-xl text-sm font-medium text-slate-700 hover:border-slate-300 transition-colors shadow-sm ml-auto">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-slate-400 mr-1">Sort:</span>
          <select 
            value={filters.sort} 
            onChange={(e) => setFilters({ ...filters, sort: e.target.value })}
            className="bg-transparent outline-none cursor-pointer font-bold"
          >
            <option value="recent">Newest First</option>
            <option value="relevant">Most Relevant</option>
            <option value="salary_desc">Highest Salary</option>
            <option value="salary_asc">Lowest Salary</option>
          </select>
        </div>
      </motion.div>

      {error && <div className="mt-6 bg-red-50 text-red-600 border border-red-100 px-5 py-4 rounded-2xl text-sm font-medium">{error}</div>}

      {loading ? (
        <JobSkeleton />
      ) : (
        <div className="mt-8 grid gap-4">
          {jobs.map((job, idx) => (
            <JobCard 
              key={job.atsJobId || job.id || idx} 
              job={job} 
              index={idx} 
              onClick={() => setSelectedJob(job)} 
            />
          ))}
          {!loading && jobs.length === 0 && !error && (
             <div className="text-center py-20 text-slate-400 font-medium border-2 border-dashed border-slate-200 rounded-3xl mt-4">
               No signals detected. Adjust your filters or query.
             </div>
          )}
        </div>
      )}

      {/* The Job Details Modal */}
      <AnimatePresence>
        {selectedJob && (
          <JobModal job={selectedJob} onClose={() => setSelectedJob(null)} />
        )}
      </AnimatePresence>
    </div>
  );
}