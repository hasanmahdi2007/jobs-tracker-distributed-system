import { Routes, Route, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import Navbar from './components/NavBar';
import Home from './pages/Home';
import Dashboard from './pages/Dashboard';

export default function App() {
  const location = useLocation();

  return (
    <div className="min-h-screen bg-[#FDFDFD] text-slate-900 font-sans selection:bg-blue-100 selection:text-blue-900 overflow-hidden relative">
      
      {/* Premium Minimalist Grid Background */}
      <div className="absolute inset-0 z-0 pointer-events-none" 
           style={{
             backgroundImage: 'radial-gradient(#e2e8f0 1px, transparent 1px)', 
             backgroundSize: '24px 24px',
             maskImage: 'linear-gradient(to bottom, black 30%, transparent 100%)',
             WebkitMaskImage: 'linear-gradient(to bottom, black 30%, transparent 100%)'
           }} 
      />
      
      {/* Ultra-soft ambient lighting */}
      <div className="absolute top-[-10%] left-[-10%] w-[50vw] h-[50vw] bg-blue-50/50 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute top-[20%] right-[-10%] w-[40vw] h-[40vw] bg-purple-50/40 rounded-full blur-[120px] pointer-events-none" />

      <div className="relative z-10">
        {location.pathname !== '/' && <Navbar />}
        <AnimatePresence mode="wait">
          <Routes location={location} key={location.pathname}>
            <Route path="/" element={<Home />} />
            <Route path="/dashboard" element={<Dashboard />} />
          </Routes>
        </AnimatePresence>
      </div>
    </div>
  );
}