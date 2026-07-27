import { useState, useCallback } from 'react';

export function useJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchJobs = useCallback(async (searchTerm, filters = {}) => {
    // 1. Instantly trigger the loading skeleton so you know the button works
    setLoading(true);
    setError(null);
    
    try {
      // 2. Grab the hidden key from the .env file
      const hiddenApiKey = import.meta.env.VITE_API_KEY;
      
      // Safety check: if Vite can't find the .env file, throw a visible error
      if (!hiddenApiKey) {
        throw new Error("Missing API Key! Check your .env file and restart Vite.");
      }

      const queryParams = new URLSearchParams({
        search: searchTerm || '',
        page: 0,
        size: 20,
        ...(filters.location && { location: filters.location }),
        ...(filters.type && { type: filters.type }),
        ...(filters.sort && { sort: filters.sort })
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
      setLoading(false); // Stop the loading skeleton
    }
  }, []);

  return { jobs, loading, error, fetchJobs };
}