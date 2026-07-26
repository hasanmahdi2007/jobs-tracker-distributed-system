import { Link, useLocation } from 'react-router-dom';
import { Briefcase } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Navbar() {
  const location = useLocation();

  return (
    <motion.nav 
      initial={{ y: -20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ type: "spring", stiffness: 300, damping: 30 }}
      className="fixed top-0 w-full z-50 bg-white/70 backdrop-blur-xl border-b border-slate-200/60"
    >
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-3 group">
          <div className="p-2 bg-slate-900 rounded-xl shadow-sm group-hover:scale-105 group-active:scale-95 transition-all duration-300">
            <Briefcase className="w-5 h-5 text-white" />
          </div>
          <span className="text-lg font-bold tracking-tight text-slate-900">Nexus</span>
        </Link>
      </div>
    </motion.nav>
  );
}