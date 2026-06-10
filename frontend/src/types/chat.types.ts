export interface ChatSessionDto {
  id: string;
  repositoryId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatSessionCreateDto {
  repositoryId: string;
  title: string;
}

export interface ChatMessageDto {
  id: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  createdAt: string;
}

export interface ChatRequestDto {
  message: string;
}
