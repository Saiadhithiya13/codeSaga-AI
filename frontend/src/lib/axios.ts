import axios, { type AxiosError, type AxiosResponse } from 'axios'
import type { ApiError, ApiResponse } from '@/types/api.types'

/**
 * Configured Axios instance for CodeSage AI API.
 *
 * Features:
 * - Base URL: /api/v1 (proxied to Spring Boot in dev via Vite)
 * - Request interceptor: attaches auth token (Sprint 2 — cookie-based, no localStorage)
 * - Response interceptor: normalizes API errors into ApiError shape
 * - Timeout: 15 seconds
 */
const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
  // Required for HttpOnly cookie auth (Sprint 2)
  withCredentials: true,
})

// ─── Request Interceptor ─────────────────────────────────────────────────────

apiClient.interceptors.request.use(
  (config) => {
    // Sprint 2: JWT is carried in HttpOnly cookies automatically by the browser.
    // No manual token attachment needed here.
    // This interceptor is reserved for adding request correlation IDs or
    // request timing metrics in future sprints.
    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// ─── Response Interceptor ────────────────────────────────────────────────────

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    // Pass successful responses through unchanged
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as any;

    if (error.response?.status === 401 && !originalRequest._retry && originalRequest.url !== '/auth/refresh') {
      if (isRefreshing) {
        return new Promise(function(resolve, reject) {
          failedQueue.push({ resolve, reject });
        }).then(() => {
          return apiClient(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await apiClient.post('/auth/refresh');
        processQueue(null, 'success');
        return apiClient(originalRequest);
      } catch (err) {
        processQueue(err, null);
        // If refresh fails, let the application handle the logout (e.g., AuthContext will catch the error and redirect)
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    const apiError: ApiError = {
      message: 'An unexpected error occurred',
      status: error.response?.status ?? 0,
      path: error.response?.data?.path ?? null,
    }

    if (error.response?.data) {
      const data = error.response.data
      apiError.message = data.message ?? apiError.message

      // Attach validation errors if present
      if (Array.isArray(data.data)) {
        apiError.errors = data.data as never
      }
    } else if (error.code === 'ECONNABORTED') {
      apiError.message = 'Request timed out. Please try again.'
    } else if (!error.response) {
      apiError.message = 'Network error. Please check your connection.'
    }

    return Promise.reject(apiError)
  }
)

export default apiClient
