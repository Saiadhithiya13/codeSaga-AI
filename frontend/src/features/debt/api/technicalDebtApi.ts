import apiClient from '@/utils/apiClient'
import { ApiResponse } from '@/types/api.types'
import { TechnicalDebtReportDto, TechnicalDebtFindingDto } from '@/types/technicalDebt.types'

export async function triggerDebtAnalysis(repositoryId: string): Promise<void> {
  await apiClient.post(`/repositories/${repositoryId}/technical-debt/analyze`)
}

export async function getLatestDebtReport(repositoryId: string): Promise<TechnicalDebtReportDto | null> {
  try {
    const response = await apiClient.get<ApiResponse<TechnicalDebtReportDto>>(`/repositories/${repositoryId}/technical-debt`)
    return response.data.data || null
  } catch (error: any) {
    if (error.response?.status === 404) {
      return null;
    }
    throw error;
  }
}

export async function getDebtFindings(repositoryId: string): Promise<TechnicalDebtFindingDto[]> {
  try {
    const response = await apiClient.get<ApiResponse<TechnicalDebtFindingDto[]>>(`/repositories/${repositoryId}/technical-debt/findings`)
    return response.data.data || []
  } catch (error: any) {
    if (error.response?.status === 404) {
      return [];
    }
    throw error;
  }
}

export async function getHealthScore(repositoryId: string): Promise<number | null> {
  try {
    const response = await apiClient.get<ApiResponse<number>>(`/repositories/${repositoryId}/health-score`)
    return response.data.data ?? null
  } catch (error: any) {
    if (error.response?.status === 404) {
      return null;
    }
    throw error;
  }
}
