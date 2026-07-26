import { Routes, Route, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Dashboard from './pages/Dashboard';

function App() {
  const location = useLocation();

  return (
    <div className="min-h-screen bg-[#050505] text-slate-100 font-sans selection:bg-blue-500/30 selection:text-blue-200">
      <Navbar />
      
      {/* AnimatePresence handles the smooth fade out when changing pages */}
      <AnimatePresence mode="wait">
        <Routes location={location} key={location.pathname}>
          <Route path="/" element={<Home />} />
          <Route path="/dashboard" element={<Dashboard />} />
        </Routes>
      </AnimatePresence>
    </div>
  );
}

export default App;