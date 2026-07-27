import { motion, useMotionTemplate, useMotionValue } from "framer-motion";
import { Building2, MapPin, ExternalLink } from "lucide-react";

export default function JobCard({ job, index, onClick }) {
  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);

  // This tracks the exact pixel the mouse is at inside the card
  function handleMouseMove({ currentTarget, clientX, clientY }) {
    const { left, top } = currentTarget.getBoundingClientRect();
    mouseX.set(clientX - left);
    mouseY.set(clientY - top);
  }

  return (
    <motion.div
      onClick={onClick}
      initial={{ opacity: 0, y: 20, filter: 'blur(10px)' }}
      animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
      transition={{ delay: index * 0.05, type: "spring", stiffness: 300, damping: 24 }}
      onMouseMove={handleMouseMove}
      className="group relative bg-white p-6 rounded-[1.5rem] border border-slate-200/60 overflow-hidden shadow-sm hover:shadow-lg hover:border-sky-200 transition-all duration-500 cursor-pointer"
    >
      {/* The Dynamic Mouse Spotlight */}
      <motion.div
        className="pointer-events-none absolute -inset-px rounded-[1.5rem] opacity-0 transition duration-300 group-hover:opacity-100"
        style={{
          background: useMotionTemplate`
            radial-gradient(
              400px circle at ${mouseX}px ${mouseY}px,
              rgba(56, 189, 248, 0.06),
              transparent 80%
            )
          `,
        }}
      />
      
      {/* Actual Content */}
      <div className="relative z-10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h4 className="text-xl font-bold text-slate-900 group-hover:text-sky-600 transition-colors tracking-tight">
            {job.title}
          </h4>
          <div className="flex flex-wrap gap-3 mt-3 text-sm font-semibold text-slate-500">
            <span className="flex items-center gap-2 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/60">
              <Building2 className="w-4 h-4 text-slate-400" /> {job.companyName}
            </span>
            <span className="flex items-center gap-2 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/60">
              <MapPin className="w-4 h-4 text-slate-400" /> {job.location || 'Remote'}
            </span>
          </div>
        </div>
        <a 
          href={job.url || job.applyUrl} 
          target="_blank" 
          rel="noopener noreferrer" 
          onClick={(e) => e.stopPropagation()} // Prevents the modal from opening when clicking Apply
          className="flex items-center justify-center gap-2 bg-white border border-slate-200 text-slate-700 hover:bg-slate-900 hover:text-white hover:border-slate-900 px-6 py-2.5 rounded-xl text-sm font-semibold transition-all shadow-sm active:scale-95 shrink-0 w-full md:w-auto"
        >
          Apply <ExternalLink className="w-4 h-4" />
        </a>
      </div>
    </motion.div>
  );
}