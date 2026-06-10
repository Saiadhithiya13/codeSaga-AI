import apiClient from '@/utils/apiClient'
import { ApiResponse } from '@/types/api.types'
import { PullRequestReviewDto, PullRequestFindingDto } from '@/types/prReview.types'

export async function triggerPrReview(repositoryId: string, prId: string): Promise<void> {
  await apiClient.post(`/repositories/${repositoryId}/pull-requests/${prId}/review`)
}

export async function getPrReviews(repositoryId: string): Promise<PullRequestReviewDto[]> {
  const response = await apiClient.get<ApiResponse<PullRequestReviewDto[]>>(`/repositories/${repositoryId}/pull-requests/reviews`)
  return response.data.data || []
}

export async function getPrReview(repositoryId: string, reviewId: string): Promise<PullRequestReviewDto> {
  const response = await apiClient.get<ApiResponse<PullRequestReviewDto>>(`/repositories/${repositoryId}/pull-requests/reviews/${reviewId}`)
  return response.data.data!
}

export async function getPrFindings(repositoryId: string, reviewId: string): Promise<PullRequestFindingDto[]> {
  const response = await apiClient.get<ApiResponse<PullRequestFindingDto[]>>(`/repositories/${repositoryId}/pull-requests/reviews/${reviewId}/findings`)
  return response.data.data || []
}
