import { Link, useLocation } from 'react-router-dom';
import { Home } from 'lucide-react'; 
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
      <div className="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 group">
          <div className="p-1.5 bg-slate-900 rounded-lg shadow-sm group-hover:scale-105 group-active:scale-95 transition-all duration-300">
            <Home className="w-4 h-4 text-white" /> 
          </div>
          <span className="text-base font-bold tracking-tight text-slate-900">Home</span>
        </Link>
      </div>
    </motion.nav>
  );
}