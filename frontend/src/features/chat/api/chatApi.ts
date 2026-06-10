import apiClient from '@/utils/apiClient'
import { ApiResponse } from '@/types/api.types'
import { ChatSessionDto, ChatSessionCreateDto, ChatMessageDto } from '@/types/chat.types'

export async function createChatSession(data: ChatSessionCreateDto): Promise<ChatSessionDto> {
  const response = await apiClient.post<ApiResponse<ChatSessionDto>>('/chat/sessions', data)
  return response.data.data!
}

export async function getUserSessions(): Promise<ChatSessionDto[]> {
  const response = await apiClient.get<ApiResponse<ChatSessionDto[]>>('/chat/sessions')
  return response.data.data || []
}

export async function getSessionMessages(sessionId: string): Promise<ChatMessageDto[]> {
  const response = await apiClient.get<ApiResponse<ChatMessageDto[]>>(`/chat/sessions/${sessionId}/messages`)
  return response.data.data || []
}

export async function deleteChatSession(sessionId: string): Promise<void> {
  await apiClient.delete(`/chat/sessions/${sessionId}`)
}

// Note: streamMessage requires special handling (SSE) and is typically handled directly in the component.
