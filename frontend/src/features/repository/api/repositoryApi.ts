import apiClient from '@/lib/axios'
import type { ApiResponse } from '@/types/api.types'
import type { 
  RepositoryDto, 
  GitHubRepoDto, 
  RepositoryContributorDto, 
  RepositoryMetricDto,
  RepositoryFileDto,
  CodeChunkDto,
  SemanticSearchResultDto,
  VectorStatsDto
} from '@/types/repository.types'

export async function getConnectedRepositories(): Promise<RepositoryDto[]> {
  const response = await apiClient.get<ApiResponse<RepositoryDto[]>>('/repositories')
  return response.data.data || []
}

export async function getRemoteRepositories(): Promise<GitHubRepoDto[]> {
  const response = await apiClient.get<ApiResponse<GitHubRepoDto[]>>('/repositories/remote')
  return response.data.data || []
}

export async function connectRepository(fullName: string): Promise<RepositoryDto> {
  const response = await apiClient.post<ApiResponse<RepositoryDto>>(`/repositories?fullName=${encodeURIComponent(fullName)}`)
  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to connect repository')
  }
  return response.data.data
}

export async function disconnectRepository(id: string): Promise<void> {
  await apiClient.delete(`/repositories/${id}`)
}

export async function getRepositoryDetails(id: string): Promise<RepositoryDto> {
  const response = await apiClient.get<ApiResponse<RepositoryDto>>(`/repositories/${id}`)
  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Failed to load repository')
  }
  return response.data.data
}

export async function syncRepository(id: string): Promise<void> {
  await apiClient.post(`/repositories/${id}/sync`)
}

export async function getRepositoryContributors(id: string): Promise<RepositoryContributorDto[]> {
  const response = await apiClient.get<ApiResponse<RepositoryContributorDto[]>>(`/repositories/${id}/contributors`)
  return response.data.data || []
}

export async function getRepositoryMetrics(id: string): Promise<RepositoryMetricDto[]> {
  const response = await apiClient.get<ApiResponse<RepositoryMetricDto[]>>(`/repositories/${id}/metrics`)
  return response.data.data || []
}

export async function startIndexing(id: string): Promise<void> {
  await apiClient.post(`/repositories/${id}/index`)
}

export async function getIndexingStatus(id: string): Promise<string> {
  const response = await apiClient.get<ApiResponse<string>>(`/repositories/${id}/index-status`)
  return response.data.data || 'PENDING'
}

export async function getRepositoryFiles(id: string): Promise<RepositoryFileDto[]> {
  const response = await apiClient.get<ApiResponse<RepositoryFileDto[]>>(`/repositories/${id}/files`)
  return response.data.data || []
}

export async function getRepositoryChunks(id: string): Promise<CodeChunkDto[]> {
  const response = await apiClient.get<ApiResponse<CodeChunkDto[]>>(`/repositories/${id}/chunks`)
  return response.data.data || []
}

export async function startEmbedding(id: string): Promise<void> {
  await apiClient.post(`/repositories/${id}/embed`)
}

export async function searchRepository(id: string, query: string, maxResults: number = 5): Promise<SemanticSearchResultDto[]> {
  const response = await apiClient.post<ApiResponse<SemanticSearchResultDto[]>>(`/repositories/${id}/search`, { query, maxResults })
  return response.data.data || []
}

export async function getVectorStats(id: string): Promise<VectorStatsDto> {
  const response = await apiClient.get<ApiResponse<VectorStatsDto>>(`/repositories/${id}/vector-stats`)
  if (!response.data.data) {
    throw new Error('Failed to load vector stats')
  }
  return response.data.data
}
