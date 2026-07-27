import { useState, useCallback } from 'react';

export function useJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Added the "filters" object to the parameters
  const fetchJobs = useCallback(async (searchTerm, filters = {}, apiKey, isGuest) => {
    if (!apiKey && !isGuest) return false;

    setLoading(true);
    setError(null);
    
    try {
      // Build the URL dynamically based on what the user selected
      const queryParams = new URLSearchParams({
        search: searchTerm || '',
        page: 0,
        size: 20, // Fetching 20 to fill out the grid nicely
        ...(filters.location && { location: filters.location }),
        ...(filters.type && { type: filters.type }),
        ...(filters.sort && { sort: filters.sort })
      });

      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?${queryParams.toString()}`, 
        { headers: { 'X-API-KEY': apiKey } }
      );
      
      if (!response.ok) {
        if (response.status === 401) throw new Error("Unauthorized: Invalid API Key.");
        throw new Error(`Gateway Error ${response.status}: Pipeline disrupted.`);
      }
      
      const data = await response.json();
      
      // Handle Spring Boot's Page<Job> format safely
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