import { useState, useCallback } from 'react';

export function useJobs() {
  const [jobs, setJobs] = useState([]);
  // Track Spring Boot's pagination metadata
  const [pageData, setPageData] = useState({ number: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Add `page` as a parameter (defaulting to 0)
  const fetchJobs = useCallback(async (searchTerm, filters = {}, page = 0) => {
    setLoading(true);
    setError(null);
    
    try {
      const hiddenApiKey = import.meta.env.VITE_API_KEY;
      
      if (!hiddenApiKey) {
        throw new Error("Missing API Key! Check your .env file and restart Vite.");
      }

      const queryParams = new URLSearchParams({
        search: searchTerm || '',
        page: page, // Pass the page number to Spring Boot
        size: 20,
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
      
      if (!response.ok) {
        throw new Error(`Gateway Error ${response.status}: Failed to fetch jobs.`);
      }
      
      const data = await response.json();
      
      // Update jobs list
      setJobs(data.content || (Array.isArray(data) ? data : []));
      
      // Save pagination metadata from Spring Boot
      setPageData({
        number: data.number || 0,
        totalPages: data.totalPages || 0,
        totalElements: data.totalElements || 0
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