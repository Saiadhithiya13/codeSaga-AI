/**
 * Global TypeScript types for CodeSage AI API communication.
 *
 * These types mirror the Java records on the backend exactly:
 * - ApiResponse<T>  ↔  com.codesage.common.dto.ApiResponse<T>
 * - ErrorDetail     ↔  com.codesage.common.dto.ErrorDetail
 */

// ─── API Response Envelope ───────────────────────────────────────────────────

/**
 * Standard API response envelope returned by all endpoints.
 * Mirrors: com.codesage.common.dto.ApiResponse<T>
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  timestamp: string;  // ISO-8601 UTC
  path: string | null;
}

/**
 * Field-level validation error detail.
 * Mirrors: com.codesage.common.dto.ErrorDetail
 */
export interface ErrorDetail {
  field: string;
  message: string;
  rejectedValue: string | null;
}

// ─── Error Response ──────────────────────────────────────────────────────────

/** API error response where data is a list of validation errors */
export type ValidationErrorResponse = ApiResponse<ErrorDetail[]>;

/**
 * Normalized API error for use throughout the application.
 * Produced by the Axios response interceptor.
 */
export interface ApiError {
  message: string;
  status: number;
  path: string | null;
  errors?: ErrorDetail[];
}

// ─── Health ──────────────────────────────────────────────────────────────────

export interface ComponentStatus {
  status: 'UP' | 'DOWN';
  details: string | null;
}

export interface HealthResponse {
  status: 'UP' | 'DEGRADED' | 'DOWN';
  version: string;
  environment: string;
  timestamp: string;
  components: Record<string, ComponentStatus>;
}

export interface DiagnosticsDto {
  totalRepositories: number;
  totalFiles: number;
  totalChunks: number;
  chunkStatusCounts: Record<string, number>;
  chromaCollections: number;
}

// ─── Pagination ──────────────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// ─── Authentication & Users (Sprint 2) ───────────────────────────────────────

export interface UserResponse {
  id: string;
  login: string;
  name: string | null;
  email: string | null;
  avatarUrl: string | null;
  role: 'USER' | 'ADMIN';
  lastLoginAt: string | null;
  createdAt: string;
}

export interface AuthTokenResponse {
  user: UserResponse;
  isNewUser: boolean;
  message: string;
}

export interface GitHubCallbackRequest {
  code: string;
  state: string;
}
