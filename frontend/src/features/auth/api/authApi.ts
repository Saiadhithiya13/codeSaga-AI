import apiClient from '@/lib/axios'
import type { ApiResponse, AuthTokenResponse, GitHubCallbackRequest } from '@/types/api.types'

/**
 * Handles the GitHub OAuth callback.
 * POST /api/v1/auth/github/callback
 */
export async function githubCallback(data: GitHubCallbackRequest): Promise<AuthTokenResponse> {
  const response = await apiClient.post<ApiResponse<AuthTokenResponse>>('/auth/github/callback', data)
  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Authentication failed')
  }
  return response.data.data
}

/**
 * Refreshes the authentication tokens using the HttpOnly refresh cookie.
 * POST /api/v1/auth/refresh
 */
export async function refreshToken(): Promise<AuthTokenResponse> {
  const response = await apiClient.post<ApiResponse<AuthTokenResponse>>('/auth/refresh')
  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Token refresh failed')
  }
  return response.data.data
}

/**
 * Logs the user out by revoking their session and clearing cookies.
 * POST /api/v1/auth/logout
 */
export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout')
}
