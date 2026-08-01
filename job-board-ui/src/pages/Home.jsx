import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Globe, ShieldCheck, ArrowRight, Briefcase, MapPin, Search } from 'lucide-react';

export default function Home() {
  return (
    <div className="min-h-screen pt-24 px-6 relative z-10 overflow-hidden flex flex-col justify-center">
      
      {/* --- Main Hero: Two-Column Split Layout --- */}
      <div className="max-w-7xl mx-auto w-full grid lg:grid-cols-2 gap-12 items-center">
        
        {/* Left Column: Text & CTAs */}
        <motion.div 
          initial={{ opacity: 0, x: -30 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.6 }}
          className="text-left relative z-10"
        >
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-50 border border-blue-100 text-xs font-bold text-blue-700 mb-6 shadow-sm">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
            </span>
            20,873+ Active Roles Inside
          </div>

          <h1 className="text-5xl md:text-6xl font-black text-slate-900 tracking-tight leading-[1.1] mb-6">
            Find your next role with <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-sky-400">absolute precision.</span>
          </h1>

          <p className="text-lg text-slate-500 mb-8 max-w-lg font-medium leading-relaxed">
            Discover thousands of top-tier roles updated in real-time. Stop guessing and start applying to verified, active listings today.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 mb-8">
            <Link 
              to="/dashboard"
              className="inline-flex justify-center items-center gap-2 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-sm px-8 py-3.5 rounded-xl transition-all shadow-md hover:shadow-lg active:scale-95"
            >
              Browse Open Roles <ArrowRight className="w-4 h-4" />
            </Link>
            <button className="inline-flex justify-center items-center gap-2 bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 font-semibold text-sm px-8 py-3.5 rounded-xl transition-all shadow-sm hover:shadow active:scale-95">
              Post a Job
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Search className="w-4 h-4 text-slate-400 mr-1" />
            <span className="text-xs font-semibold text-slate-500 mr-2">Trending:</span>
            {['Software Engineer', 'Product Manager', 'Remote'].map((tag) => (
              <Link key={tag} to="/dashboard" className="px-3 py-1 bg-white border border-slate-200 hover:border-blue-300 hover:bg-blue-50 rounded-lg text-xs font-medium text-slate-600 transition-colors shadow-sm">
                {tag}
              </Link>
            ))}
          </div>
        </motion.div>

        {/* Right Column: Visual Floating Job Stack */}
        <motion.div 
          initial={{ opacity: 0, lg: 30 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.8, delay: 0.2 }}
          className="relative hidden lg:block h-[500px] w-full perspective-1000"
        >
          {/* Decorative background circle */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-100/50 rounded-full blur-3xl pointer-events-none" />
          
          {/* Card 1 (Bottom, tilted left) */}
          <div className="absolute top-20 right-12 w-80 bg-white p-5 rounded-2xl border border-slate-200 shadow-xl -rotate-6 scale-95 opacity-70 blur-[1px]">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-red-50 rounded-lg flex items-center justify-center font-bold text-red-600">N</div>
              <div>
                <p className="text-sm font-bold text-slate-900">Data Scientist</p>
                <p className="text-xs text-slate-500">Noon • Dubai, UAE</p>
              </div>
            </div>
            <div className="flex gap-2">
              <span className="px-2 py-1 bg-slate-100 rounded text-[10px] font-semibold text-slate-600">Full-Time</span>
              <span className="px-2 py-1 bg-slate-100 rounded text-[10px] font-semibold text-slate-600">Hybrid</span>
            </div>
          </div>

          {/* Card 2 (Middle, tilted right) */}
          <div className="absolute top-36 left-4 w-80 bg-white p-5 rounded-2xl border border-slate-200 shadow-2xl rotate-3 scale-100 opacity-90">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-10 h-10 bg-orange-50 rounded-lg flex items-center justify-center font-bold text-orange-600">T</div>
              <div>
                <p className="text-sm font-bold text-slate-900">Frontend Developer</p>
                <p className="text-xs text-slate-500">Talabat • Remote</p>
              </div>
            </div>
            <div className="flex gap-2">
              <span className="px-2 py-1 bg-slate-100 rounded text-[10px] font-semibold text-slate-600">Contract</span>
              <span className="px-2 py-1 bg-slate-100 rounded text-[10px] font-semibold text-slate-600">React</span>
            </div>
          </div>

          {/* Card 3 (Top, straight, primary focus) */}
          <div className="absolute top-12 left-16 w-[340px] bg-white p-6 rounded-2xl border border-blue-200 shadow-2xl z-10 transform hover:-translate-y-2 transition-transform duration-300">
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-green-50 rounded-xl flex items-center justify-center font-bold text-green-600 text-xl border border-green-100">C</div>
                <div>
                  <p className="text-base font-bold text-slate-900">Senior Product Manager</p>
                  <p className="text-xs font-medium text-slate-500 flex items-center gap-1 mt-0.5">
                    <MapPin className="w-3 h-3" /> Careem • Riyadh, KSA
                  </p>
                </div>
              </div>
              <div className="px-2 py-1 bg-green-100 text-green-700 text-[10px] font-bold rounded-md">NEW</div>
            </div>
            <p className="text-xs text-slate-500 mb-4 line-clamp-2">
              Join our core ride-hailing team to lead product strategy, roadmapping, and execution across the MENA region.
            </p>
            <div className="flex items-center justify-between border-t border-slate-100 pt-4">
              <span className="text-sm font-bold text-slate-900">$120k - $150k</span>
              <button className="text-xs font-bold text-blue-600 hover:text-blue-700">View Role →</button>
            </div>
          </div>
        </motion.div>
      </div>

      {/* --- Feature Cards & Company Strip --- */}
      <div className="max-w-7xl mx-auto w-full mt-24 pb-16">
        
        {/* Company Strip */}
        <div className="flex flex-wrap justify-center items-center gap-8 md:gap-16 text-slate-400/60 font-black text-xl md:text-2xl mb-16 grayscale">
          <span className="hover:grayscale-0 hover:text-green-500 transition-all cursor-default">CAREEM</span>
          <span className="hover:grayscale-0 hover:text-orange-500 transition-all cursor-default">TALABAT</span>
          <span className="hover:grayscale-0 hover:text-yellow-500 transition-all cursor-default">NOON</span>
          <span className="hover:grayscale-0 hover:text-blue-500 transition-all cursor-default">EMIRATES</span>
        </div>

        <div className="grid md:grid-cols-2 gap-6 w-full max-w-4xl mx-auto">
          <div className="bg-white/80 backdrop-blur-sm p-6 rounded-[1.5rem] border border-slate-200 shadow-sm text-left hover:shadow-md transition-shadow">
            <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center mb-4 border border-blue-100">
              <Globe className="w-5 h-5 text-blue-600" />
            </div>
            <h3 className="text-lg font-bold text-slate-900 mb-2">Global Aggregation</h3>
            <p className="text-sm text-slate-500 leading-relaxed">
              We pull listings directly from top company career pages so you have access to roles before they hit major job boards.
            </p>
          </div>

          <div className="bg-white/80 backdrop-blur-sm p-6 rounded-[1.5rem] border border-slate-200 shadow-sm text-left hover:shadow-md transition-shadow">
            <div className="w-10 h-10 bg-purple-50 rounded-xl flex items-center justify-center mb-4 border border-purple-100">
              <ShieldCheck className="w-5 h-5 text-purple-600" />
            </div>
            <h3 className="text-lg font-bold text-slate-900 mb-2">Verified Listings</h3>
            <p className="text-sm text-slate-500 leading-relaxed">
              Ghost jobs are a waste of time. Our platform continuously validates every listing to ensure you only apply to active roles.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}