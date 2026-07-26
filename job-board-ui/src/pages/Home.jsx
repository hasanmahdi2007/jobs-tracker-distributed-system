import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Power, Activity } from 'lucide-react';

export default function Home() {
  return (
    <div className="relative min-h-screen flex items-center justify-center overflow-hidden bg-black">
      
      {/* LIVING BACKGROUND: Infinite breathing plasma orbs */}
      <motion.div 
        animate={{ 
          scale: [1, 1.5, 1], 
          x: [0, 100, -50, 0],
          y: [0, -100, 50, 0]
        }}
        transition={{ duration: 20, repeat: Infinity, ease: "linear" }}
        className="absolute top-1/4 left-1/4 w-[500px] h-[500px] bg-cyan-600/40 rounded-full blur-[120px] mix-blend-screen pointer-events-none"
      />
      
      <motion.div 
        animate={{ 
          scale: [1, 1.2, 1.8, 1], 
          x: [0, -150, 50, 0],
          y: [0, 150, -50, 0]
        }}
        transition={{ duration: 25, repeat: Infinity, ease: "linear" }}
        className="absolute bottom-1/4 right-1/4 w-[600px] h-[600px] bg-fuchsia-600/30 rounded-full blur-[150px] mix-blend-screen pointer-events-none"
      />

      <motion.div 
        animate={{ opacity: [0.3, 0.6, 0.3] }}
        transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }}
        className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 pointer-events-none mix-blend-overlay"
      />

      {/* FOREGROUND: The Glass Terminal */}
      <motion.div 
        initial={{ opacity: 0, scale: 0.9, y: 40 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ duration: 1.5, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 flex flex-col items-center justify-center text-center p-12 max-w-4xl w-full"
      >
        <motion.div 
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          transition={{ delay: 0.5, duration: 1 }}
          className="mb-8 inline-flex items-center gap-3 px-6 py-2 rounded-full bg-white/5 border border-white/10 backdrop-blur-2xl"
        >
          <motion.div 
            animate={{ scale: [1, 1.5, 1], opacity: [0.5, 1, 0.5] }}
            transition={{ duration: 2, repeat: Infinity }}
            className="w-2 h-2 rounded-full bg-cyan-400 shadow-[0_0_10px_rgba(34,211,238,0.8)]"
          />
          <span className="text-cyan-400 text-xs font-mono tracking-widest uppercase">Distributed Engine Active</span>
        </motion.div>

        <h1 className="text-6xl md:text-8xl font-black tracking-tighter text-transparent bg-clip-text bg-gradient-to-br from-white via-white to-white/20 mb-6 drop-shadow-[0_0_30px_rgba(255,255,255,0.2)]">
          ENTER THE <br /> NEXUS
        </h1>

        <p className="text-slate-400 text-lg md:text-xl font-light tracking-wide max-w-2xl mb-12">
          A real-time synchronization pipeline. Aggregating tech roles across the global grid.
        </p>

        {/* The Portal Button */}
        <Link to="/dashboard" className="group relative">
          {/* Glowing aura behind the button that expands on hover */}
          <div className="absolute -inset-1 bg-gradient-to-r from-cyan-500 to-fuchsia-600 rounded-full blur-lg opacity-40 group-hover:opacity-100 transition duration-500 group-hover:duration-200 animate-pulse"></div>
          
          <button className="relative flex items-center gap-4 px-10 py-5 bg-black border border-white/10 rounded-full text-white font-bold tracking-widest uppercase overflow-hidden transition-all hover:border-cyan-500/50 hover:bg-white/5 backdrop-blur-xl">
            <Power className="w-5 h-5 text-cyan-400 group-hover:animate-spin" />
            Initialize System
            
            {/* Light sweep effect on hover */}
            <div className="absolute inset-0 h-full w-full bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:animate-[shimmer_1.5s_infinite]"></div>
          </button>
        </Link>
      </motion.div>
    </div>
  );
}