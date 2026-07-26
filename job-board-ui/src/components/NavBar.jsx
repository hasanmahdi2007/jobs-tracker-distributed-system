import { Link, useLocation } from 'react-router-dom';
import { Briefcase, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Navbar() {
  const location = useLocation();

  return (
    <nav className="fixed top-0 w-full z-50 bg-slate-950/50 backdrop-blur-xl border-b border-white/5">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-3 group">
          <div className="p-2 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl shadow-lg shadow-blue-500/20 group-hover:shadow-blue-500/40 transition-all">
            <Briefcase className="w-5 h-5 text-white" />
          </div>
          <span className="text-lg font-bold tracking-tight text-white">NexusJob</span>
        </Link>

        <div className="flex items-center gap-4">
          {location.pathname !== '/dashboard' && (
            <Link 
              to="/dashboard"
              className="relative inline-flex items-center justify-center gap-2 px-5 py-2 text-sm font-medium text-white transition-all bg-white/5 border border-white/10 rounded-full hover:bg-white/10 hover:scale-105 active:scale-95"
            >
              <Sparkles className="w-4 h-4 text-blue-400" />
              Launch Console
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}