import { motion } from 'framer-motion';

export default function JobSkeleton() {
  return (
    <div className="mt-8 grid gap-4">
      {[1, 2, 3].map((i) => (
        <motion.div 
          key={i}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: i * 0.1 }}
          className="bg-white p-6 rounded-[1.5rem] border border-slate-100 flex flex-col md:flex-row justify-between items-center gap-4"
        >
          <div className="w-full">
            <div className="h-6 bg-slate-200 rounded-md w-1/3 mb-4 animate-pulse"></div>
            <div className="flex gap-4">
              <div className="h-8 bg-slate-100 rounded-lg w-24 animate-pulse"></div>
              <div className="h-8 bg-slate-100 rounded-lg w-24 animate-pulse"></div>
            </div>
          </div>
          <div className="h-10 bg-slate-100 rounded-xl w-32 shrink-0 animate-pulse"></div>
        </motion.div>
      ))}
    </div>
  );
}