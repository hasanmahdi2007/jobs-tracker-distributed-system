import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { ArrowRight, Terminal, Globe, Shield } from 'lucide-react';

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
    <div className="relative min-h-screen flex flex-col items-center justify-center pt-20 px-6 z-10">
      <motion.div 
        variants={container}
        initial="hidden"
        animate="show"
        className="max-w-5xl mx-auto text-center"
      >
        {/* Top Badge */}
        <motion.div variants={item} className="mb-8 inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white border border-slate-200 text-slate-600 text-sm font-semibold shadow-sm">
          <Terminal className="w-4 h-4 text-blue-500" />
          <span>System V2.0 Online</span>
        </motion.div>

        {/* Hero Text */}
        <motion.h1 variants={item} className="text-6xl md:text-8xl font-black tracking-tighter text-slate-900 leading-[1.05] mb-6">
          Navigate the grid with <br className="hidden md:block" />
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-indigo-600">
            absolute precision.
          </span>
        </motion.h1>

        {/* Subtitle */}
        <motion.p variants={item} className="text-lg md:text-xl text-slate-500 font-medium max-w-2xl mx-auto mb-10 leading-relaxed">
          Powered by a highly available Spring Boot microservice architecture and a real-time PostgreSQL synchronization pipeline.
        </motion.p>

        {/* Call to Action */}
        <motion.div variants={item} className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link 
            to="/dashboard"
            className="group relative inline-flex items-center justify-center gap-2 px-8 py-4 bg-slate-900 text-white font-bold rounded-2xl hover:bg-slate-800 transition-all shadow-[0_8px_30px_rgb(0,0,0,0.12)] hover:shadow-[0_8px_30px_rgb(0,0,0,0.2)] active:scale-95"
          >
            Enter the Console
            <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </Link>
        </motion.div>

        {/* Mini Bento Grid */}
        <motion.div variants={item} className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-24 max-w-4xl mx-auto text-left">
          
          <div className="p-8 bg-white/60 backdrop-blur-md border border-slate-200/80 rounded-[2rem] shadow-sm hover:shadow-md transition-shadow group">
            <div className="w-12 h-12 bg-blue-50 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Globe className="w-6 h-6 text-blue-600" />
            </div>
            <h3 className="text-xl font-bold text-slate-900 mb-2 tracking-tight">Distributed Pipeline</h3>
            <p className="text-slate-500 font-medium leading-relaxed">
              Aggregating tech roles globally across multiple redundant backend nodes for zero downtime.
            </p>
          </div>

          <div className="p-8 bg-white/60 backdrop-blur-md border border-slate-200/80 rounded-[2rem] shadow-sm hover:shadow-md transition-shadow group">
            <div className="w-12 h-12 bg-indigo-50 rounded-2xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Shield className="w-6 h-6 text-indigo-600" />
            </div>
            <h3 className="text-xl font-bold text-slate-900 mb-2 tracking-tight">Secure Gateway</h3>
            <p className="text-slate-500 font-medium leading-relaxed">
              Protected by reactive Spring Cloud WebFlux routing and strict token-based client authentication.
            </p>
          </div>

        </motion.div>
      </motion.div>
    </div>
  );
}