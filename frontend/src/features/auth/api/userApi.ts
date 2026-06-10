import apiClient from '@/lib/axios'
import type { ApiResponse, UserResponse } from '@/types/api.types'

/**
 * Retrieves the currently authenticated user's profile.
 * GET /api/v1/users/me
 */
export async function fetchMe(): Promise<UserResponse> {
  const response = await apiClient.get<ApiResponse<UserResponse>>('/users/me')
  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to fetch user profile')
  }
  return response.data.data
}
