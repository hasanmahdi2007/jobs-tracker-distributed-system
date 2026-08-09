import { useState, useCallback } from 'react';

export function useJobs() {
  const [jobs, setJobs] = useState([]);
  // Replaced totalPages with a simple 'hasMore' boolean for infinite scroll / next buttons
  const [pageData, setPageData] = useState({ number: 0, hasMore: true });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchJobs = useCallback(async (searchTerm, filters = {}, page = 0) => {
    setLoading(true);
    setError(null);
    
    try {
      const hiddenApiKey = import.meta.env.VITE_API_KEY;
      if (!hiddenApiKey) throw new Error("Missing API Key in .env file!");

      const queryParams = new URLSearchParams({
        search: searchTerm || '',
        page: page,
        size: 10,
        ...(filters.location && { location: filters.location }),
        ...(filters.type && { type: filters.type }),
        ...(filters.sort && { sort: filters.sort }),
        ...(filters.company && { company: filters.company }),
        ...(filters.category && { category: filters.category })
      });

      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?${queryParams.toString()}`, 
        { headers: { 'X-API-KEY': hiddenApiKey } }
      );
      
      if (!response.ok) throw new Error(`Gateway Error ${response.status}`);
      
      // data is now a direct array: [ {job1}, {job2} ]
      const data = await response.json();
      
      setJobs(data || []);
      
      // If we got exactly 10 items back, there is likely a next page.
      // If we got less than 10, we've reached the end of the database!
      setPageData({
        number: page,
        hasMore: data.length === 10 
      });
      
    } catch (err) {
      console.error("Fetch Error:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  return { jobs, pageData, loading, error, fetchJobs };
}