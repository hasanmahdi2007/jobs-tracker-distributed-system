import { motion } from 'framer-motion';
import { X, MapPin, Briefcase, Building, DollarSign, ExternalLink } from 'lucide-react';

export default function JobModal({ job, onClose }) {
  if (!job) return null;

  // THE DECODER
  const decodeHTML = (htmlString) => {
    if (!htmlString) return "";
    const textArea = document.createElement("textarea");
    textArea.innerHTML = htmlString;
    return textArea.value;
  };

  const rawDescription = job.descriptionText || job.description || "";
  const cleanHTML = decodeHTML(rawDescription);

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/20 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div 
        initial={{ opacity: 0, scale: 0.95, y: 20 }} 
        animate={{ opacity: 1, scale: 1, y: 0 }} 
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        onClick={(e) => e.stopPropagation()} 
        // Dropped width to max-w-lg and height to max-h-[75vh]
        className="bg-white border border-slate-200 shadow-2xl w-full max-w-lg max-h-[75vh] rounded-[1.25rem] flex flex-col overflow-hidden relative"
      >
        {/* Header Section */}
        <div className="p-4 border-b border-slate-100 flex-shrink-0">
          <button 
            onClick={onClose} 
            className="absolute top-4 right-4 p-1.5 text-slate-400 hover:text-slate-900 bg-slate-50 hover:bg-slate-100 rounded-full transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
          
          <h2 className="text-lg sm:text-xl font-bold text-slate-900 tracking-tight pr-8 mb-2.5">
            {job.title}
          </h2>
          
          <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-slate-600">
            <span className="flex items-center gap-1 bg-slate-50 px-2 py-1 rounded-md border border-slate-200/60">
              <Building className="w-3 h-3 text-slate-400" /> {job.companyName}
            </span>
            <span className="flex items-center gap-1 bg-slate-50 px-2 py-1 rounded-md border border-slate-200/60">
              <MapPin className="w-3 h-3 text-slate-400" /> {job.location || 'Remote'}
            </span>
            <span className="flex items-center gap-1 bg-slate-50 px-2 py-1 rounded-md border border-slate-200/60">
              <Briefcase className="w-3 h-3 text-slate-400" /> {job.employmentType || 'Full-Time'}
            </span>
            {job.salaryMin && (
              <span className="flex items-center gap-1 bg-green-50 text-green-700 px-2 py-1 rounded-md border border-green-200/60">
                <DollarSign className="w-3 h-3" /> 
                {job.salaryMin} - {job.salaryMax} {job.salaryCurrency}
              </span>
            )}
          </div>
        </div>

        {/* Description Body */}
        <div className="p-4 overflow-y-auto flex-grow text-sm text-slate-600 leading-relaxed">
          {cleanHTML ? (
            <div 
              className="
                [&>p]:mb-3 
                [&>ul]:list-disc [&>ul]:ml-5 [&>ul]:mb-3 [&>ul>li]:mb-1 [&>ul>li]:pl-1
                [&>ol]:list-decimal [&>ol]:ml-5 [&>ol]:mb-3 [&>ol>li]:mb-1
                [&>strong]:font-bold [&>strong]:text-slate-900 
                [&>b]:font-bold [&>b]:text-slate-900
                [&>h1]:text-lg [&>h1]:font-bold [&>h1]:text-slate-900 [&>h1]:mb-3 [&>h1]:mt-4
                [&>h2]:text-base [&>h2]:font-bold [&>h2]:text-slate-900 [&>h2]:mb-2 [&>h2]:mt-3
                [&>h3]:text-sm [&>h3]:font-bold [&>h3]:text-slate-900 [&>h3]:mb-1 [&>h3]:mt-2
                [&>a]:text-sky-600 [&>a]:hover:text-sky-700 [&>a]:underline
              "
              dangerouslySetInnerHTML={{ __html: cleanHTML }} 
            />
          ) : (
            <p className="italic text-slate-400 text-center py-8">No detailed description provided for this role.</p>
          )}
        </div>

        {/* Footer CTA */}
        <div className="p-3 border-t border-slate-100 bg-slate-50 flex-shrink-0 flex justify-end">
          <button 
            onClick={(e) => {
              e.stopPropagation();
              const url = job.applyUrl || job.apply_url || job.url;
              if (url) {
                const finalUrl = url.startsWith('http') ? url : `https://${url}`;
                window.open(finalUrl, '_blank', 'noopener,noreferrer');
              }
            }}
            className="bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold py-2 px-5 rounded-lg transition-all shadow-md active:scale-95 flex items-center gap-1.5"
          >
            Apply Now <ExternalLink className="w-3.5 h-3.5" />
          </button>
        </div>
      </motion.div>
    </div>
  );
}