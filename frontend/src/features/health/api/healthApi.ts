import apiClient from '@/lib/axios'
import type { ApiResponse, HealthResponse, DiagnosticsDto } from '@/types/api.types'

/**
 * Fetches application health status from the backend.
 *
 * Endpoint: GET /api/v1/health
 * Always public — no auth required.
 */
export async function fetchHealth(): Promise<HealthResponse> {
  const response = await apiClient.get<ApiResponse<HealthResponse>>('/health')
  const body = response.data
  if (!body.success || !body.data) {
    throw new Error(body.message ?? 'Health check failed')
  }
  return body.data
}

export async function fetchDiagnostics(): Promise<DiagnosticsDto> {
  const response = await apiClient.get<ApiResponse<DiagnosticsDto>>('/health/diagnostics')
  const body = response.data
  if (!body.success || !body.data) {
    throw new Error(body.message ?? 'Failed to fetch diagnostics')
  }
  return body.data
}
