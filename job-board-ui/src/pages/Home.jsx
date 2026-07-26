import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { ArrowRight, Terminal, Globe, Zap } from 'lucide-react';

export default function Home() {
  const container = {
    hidden: { opacity: 0 },
    show: { opacity: 1, transition: { staggerChildren: 0.1 } }
  };

  const item = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0, transition: { type: "spring", stiffness: 300, damping: 24 } }
  };

  return (
    <div className="relative min-h-screen flex items-center justify-center overflow-hidden pt-16">
      {/* Premium Background Glows */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-blue-600/20 blur-[120px] rounded-full pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-[600px] h-[600px] bg-purple-600/10 blur-[150px] rounded-full pointer-events-none" />

      <motion.div 
        variants={container}
        initial="hidden"
        animate="show"
        className="relative z-10 max-w-5xl mx-auto px-6 text-center space-y-8"
      >
        <motion.div variants={item} className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-sm font-medium">
          <Terminal className="w-4 h-4" /> V1.0 Distributed Engine Live
        </motion.div>

        <motion.h1 variants={item} className="text-5xl md:text-7xl font-black tracking-tighter text-transparent bg-clip-text bg-gradient-to-br from-white via-slate-200 to-slate-500 leading-tight pb-2">
          Find your next role with <br /> distributed precision.
        </motion.h1>

        <motion.p variants={item} className="max-w-2xl mx-auto text-lg md:text-xl text-slate-400">
          Powered by a highly available Spring Boot microservice architecture and real-time PostgreSQL pipelines.
        </motion.p>

        <motion.div variants={item} className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
          <Link 
            to="/dashboard"
            className="group relative inline-flex items-center justify-center gap-2 px-8 py-4 bg-white text-slate-950 font-bold rounded-2xl hover:scale-105 active:scale-95 transition-all shadow-xl shadow-white/10"
          >
            Initialize Search <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </Link>
        </motion.div>

        {/* Feature Grid */}
        <motion.div variants={item} className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-16 max-w-3xl mx-auto text-left">
          <div className="p-6 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-sm">
            <Globe className="w-8 h-8 text-blue-400 mb-4" />
            <h3 className="text-xl font-bold text-white mb-2">Global Gateway</h3>
            <p className="text-slate-400 text-sm">Secure, token-authenticated API routing protecting our core databases.</p>
          </div>
          <div className="p-6 rounded-3xl bg-white/5 border border-white/10 backdrop-blur-sm">
            <Zap className="w-8 h-8 text-amber-400 mb-4" />
            <h3 className="text-xl font-bold text-white mb-2">Real-time Pipeline</h3>
            <p className="text-slate-400 text-sm">Lightning-fast query execution aggregating tech roles across the web.</p>
          </div>
        </motion.div>
      </motion.div>
    </div>
  );
}