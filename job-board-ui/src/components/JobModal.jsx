import { motion } from 'framer-motion';
import { X, MapPin, Briefcase, Building, DollarSign, ExternalLink } from 'lucide-react';

export default function JobModal({ job, onClose }) {
  if (!job) return null;

  // STEP 1: THE DECODER
  // This function takes the scrambled "&lt;p&gt;" and translates it back to "<p>"
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
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-900/20 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div 
        initial={{ opacity: 0, scale: 0.95, y: 20 }} 
        animate={{ opacity: 1, scale: 1, y: 0 }} 
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        onClick={(e) => e.stopPropagation()} 
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

        {/* STEP 2: THE INJECTOR */}
        {/* We pass the decoded HTML into dangerouslySetInnerHTML, and use Tailwind to style the new paragraphs and lists */}
        <div className="p-6 sm:p-8 overflow-y-auto flex-grow text-slate-600 leading-relaxed">
          {cleanHTML ? (
            <div 
              className="
                [&>p]:mb-4 
                [&>ul]:list-disc [&>ul]:ml-6 [&>ul]:mb-4 [&>ul>li]:mb-1.5 [&>ul>li]:pl-1
                [&>ol]:list-decimal [&>ol]:ml-6 [&>ol]:mb-4 [&>ol>li]:mb-1.5
                [&>strong]:font-bold [&>strong]:text-slate-900 
                [&>b]:font-bold [&>b]:text-slate-900
                [&>h1]:text-2xl [&>h1]:font-bold [&>h1]:text-slate-900 [&>h1]:mb-4 [&>h1]:mt-6
                [&>h2]:text-xl [&>h2]:font-bold [&>h2]:text-slate-900 [&>h2]:mb-3 [&>h2]:mt-5
                [&>h3]:text-lg [&>h3]:font-bold [&>h3]:text-slate-900 [&>h3]:mb-2 [&>h3]:mt-4
                [&>a]:text-sky-600 [&>a]:hover:text-sky-700 [&>a]:underline
              "
              dangerouslySetInnerHTML={{ __html: cleanHTML }} 
            />
          ) : (
            <p className="italic text-slate-400 text-center py-10">No detailed description provided for this role.</p>
          )}
        </div>

        {/* Footer CTA */}
        <div className="p-6 border-t border-slate-100 bg-slate-50 flex-shrink-0 flex justify-end">
          <button 
            onClick={(e) => {
              e.stopPropagation();
              const url = job.applyUrl || job.apply_url || job.url;
              if (url) {
                const finalUrl = url.startsWith('http') ? url : `https://${url}`;
                window.open(finalUrl, '_blank', 'noopener,noreferrer');
              }
            }}
            className="bg-slate-900 hover:bg-slate-800 text-white font-bold py-3.5 px-8 rounded-xl transition-all shadow-md active:scale-95 flex items-center gap-2"
          >
            Apply Now <ExternalLink className="w-4 h-4" />
          </button>
        </div>
      </motion.div>
    </div>
  );
}