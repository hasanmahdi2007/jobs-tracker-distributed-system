import { useState, useCallback } from 'react';

export function useJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchJobs = useCallback(async (searchTerm, apiKey, isGuest) => {
    if (!apiKey && !isGuest) return false; // Triggers auth modal in UI

    setLoading(true);
    setError(null);
    
    try {
      const headerKey = isGuest ? 'GUEST-ACCESS-TOKEN' : apiKey; 
      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?search=${encodeURIComponent(searchTerm)}&page=0&size=10`, 
        { headers: { 'X-API-KEY': headerKey } }
      );
      
      if (!response.ok) throw new Error(`Gateway Error ${response.status}: Pipeline disrupted.`);
      
      const data = await response.json();
      // This checks for a Page object first, then a direct List array, then falls back to empty.
      setJobs(data.content || (Array.isArray(data) ? data : []));
      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  return { jobs, loading, error, fetchJobs };
}