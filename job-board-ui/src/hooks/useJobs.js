import { useState, useCallback } from 'react';

export function useJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchJobs = useCallback(async (searchTerm, filters = {}) => {
    setLoading(true);
    setError(null);
    
    try {
      const hiddenApiKey = import.meta.env.VITE_API_KEY;
      
      if (!hiddenApiKey) {
        throw new Error("Missing API Key! Check your .env file and restart Vite.");
      }

      // Updated to include company and category!
      const queryParams = new URLSearchParams({
        search: searchTerm || '',
        page: 0,
        size: 20,
        ...(filters.location && { location: filters.location }),
        ...(filters.type && { type: filters.type }),
        ...(filters.sort && { sort: filters.sort }),
        ...(filters.company && { company: filters.company }),   // <-- Added
        ...(filters.category && { category: filters.category }) // <-- Added
      });

      console.log("Attempting to fetch with key:", hiddenApiKey.substring(0, 10) + "...");

      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?${queryParams.toString()}`, 
        { 
          headers: { 'X-API-KEY': hiddenApiKey } 
        }
      );
      
      if (!response.ok) {
        throw new Error(`Gateway Error ${response.status}: Failed to fetch jobs.`);
      }
      
      const data = await response.json();
      setJobs(data.content || (Array.isArray(data) ? data : []));
      
    } catch (err) {
      console.error("Fetch Error:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  return { jobs, loading, error, fetchJobs };
}