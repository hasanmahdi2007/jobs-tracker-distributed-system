import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Globe, ShieldCheck, ArrowRight } from 'lucide-react';

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center pt-16 px-6 relative z-10">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="text-center max-w-3xl mx-auto"
      >
        {/* Top Live Badge */}
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-100 border border-slate-200 text-xs font-semibold text-slate-700 mb-6">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
          </span>
          Live Job Updates
        </div>

        {/* Downsized Main Headline */}
        <h1 className="text-4xl md:text-5xl font-black text-slate-900 tracking-tight leading-tight mb-5">
          Find your next role with <span className="text-blue-600">absolute precision.</span>
        </h1>

        {/* Downsized Sub-headline */}
        <p className="text-base md:text-lg text-slate-500 mb-8 max-w-xl mx-auto font-medium">
          Discover thousands of top-tier tech roles updated in real-time directly from the source.
        </p>

        {/* Shrunk CTA Button */}
        <Link to="/dashboard">
          <button className="bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm px-6 py-3 rounded-full transition-all shadow-md hover:shadow-lg active:scale-95 flex items-center gap-2 mx-auto">
            Browse Open Roles <ArrowRight className="w-4 h-4" />
          </button>
        </Link>
      </motion.div>

      {/* Feature Cards - Reduced padding, icon size, and margin-top */}
      <motion.div 
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.2 }}
        className="grid md:grid-cols-2 gap-5 mt-16 w-full max-w-4xl mx-auto"
      >
        <div className="bg-white p-6 rounded-[1.5rem] border border-slate-200 shadow-sm text-left hover:shadow-md transition-shadow">
          <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center mb-4 border border-blue-100">
            <Globe className="w-5 h-5 text-blue-600" />
          </div>
          <h3 className="text-lg font-bold text-slate-900 mb-2">Global Aggregation</h3>
          <p className="text-sm text-slate-500 leading-relaxed">
            We aggregate top tech roles from the best companies globally so you never miss an opportunity.
          </p>
        </div>

        <div className="bg-white p-6 rounded-[1.5rem] border border-slate-200 shadow-sm text-left hover:shadow-md transition-shadow">
          <div className="w-10 h-10 bg-purple-50 rounded-xl flex items-center justify-center mb-4 border border-purple-100">
            <ShieldCheck className="w-5 h-5 text-purple-600" />
          </div>
          <h3 className="text-lg font-bold text-slate-900 mb-2">Verified Listings</h3>
          <p className="text-sm text-slate-500 leading-relaxed">
            Every job is actively monitored and updated to ensure you only apply to open, active roles.
          </p>
        </div>
      </motion.div>
    </div>
  );
}