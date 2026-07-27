import { motion } from 'framer-motion';
import { X, MapPin, Briefcase, Building, DollarSign, ExternalLink } from 'lucide-react';

export default function JobModal({ job, onClose }) {
  if (!job) return null;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/20 backdrop-blur-sm"
      onClick={onClose} // Clicking the background closes it
    >
      <motion.div 
        initial={{ opacity: 0, scale: 0.95, y: 20 }} 
        animate={{ opacity: 1, scale: 1, y: 0 }} 
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        onClick={(e) => e.stopPropagation()} // Prevent clicking inside from closing it
        className="bg-white border border-slate-200 shadow-2xl w-full max-w-2xl max-h-[90vh] rounded-[2rem] flex flex-col overflow-hidden relative"
      >
        {/* Header Section */}
        <div className="p-6 sm:p-8 border-b border-slate-100 flex-shrink-0">
          <button 
            onClick={onClose} 
            className="absolute top-6 right-6 p-2 text-slate-400 hover:text-slate-900 bg-slate-50 hover:bg-slate-100 rounded-full transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
          
          <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight pr-10 mb-3">
            {job.title}
          </h2>
          
          <div className="flex flex-wrap items-center gap-3 text-sm font-medium text-slate-600">
            <span className="flex items-center gap-1.5 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/60">
              <Building className="w-4 h-4 text-slate-400" /> {job.companyName}
            </span>
            <span className="flex items-center gap-1.5 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/60">
              <MapPin className="w-4 h-4 text-slate-400" /> {job.location || 'Remote'}
            </span>
            <span className="flex items-center gap-1.5 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/60">
              <Briefcase className="w-4 h-4 text-slate-400" /> {job.employmentType || 'Full-Time'}
            </span>
            {job.salaryMin && (
              <span className="flex items-center gap-1.5 bg-green-50 text-green-700 px-3 py-1.5 rounded-lg border border-green-200/60">
                <DollarSign className="w-4 h-4" /> 
                {job.salaryMin} - {job.salaryMax} {job.salaryCurrency}
              </span>
            )}
          </div>
        </div>

        {/* Scrollable Description Body */}
        <div className="p-6 sm:p-8 overflow-y-auto flex-grow text-slate-600 leading-relaxed whitespace-pre-wrap">
          {job.descriptionText || job.description ? (
            <p>{job.descriptionText || job.description}</p>
          ) : (
            <p className="italic text-slate-400 text-center py-10">No detailed description provided for this role.</p>
          )}
        </div>

        {/* Footer CTA */}
        <div className="p-6 border-t border-slate-100 bg-slate-50 flex-shrink-0 flex justify-end">
          <a 
            href={job.applyUrl} 
            target="_blank" 
            rel="noopener noreferrer"
            className="bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 px-8 rounded-xl transition-all shadow-md active:scale-95 flex items-center gap-2"
          >
            Apply Now <ExternalLink className="w-4 h-4" />
          </a>
        </div>
      </motion.div>
    </div>
  );
}