import { useState, useCallback } from 'react';

export function useJobs() {
  const [jobs, setJobs] = useState([]);
  const [pageData, setPageData] = useState({ number: 0, hasMore: true });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchJobs = useCallback(async (searchTerm = '', filters = {}, page = 0) => {
    setLoading(true);
    setError(null);
    
    try {
      // 1. NO MORE API KEY CHECKS!
      
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

      // 2. FETCH ANONYMOUSLY (The Gateway will track via IP instead)
      const response = await fetch(
        `http://localhost:8080/api/v1/jobs?${queryParams.toString()}`
      );
      
      if (!response.ok) throw new Error(`Gateway Error ${response.status}`);
      
      const data = await response.json();
      
      setJobs(data || []);
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