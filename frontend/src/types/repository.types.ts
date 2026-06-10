export interface RepositoryDto {
  id: string;
  githubRepoId: number;
  fullName: string;
  name: string;
  description: string | null;
  language: string | null;
  isPrivate: boolean;
  defaultBranch: string;
  starsCount: number;
  forksCount: number;
  openIssuesCount: number;
  indexingStatus: 'PENDING' | 'INDEXING' | 'INDEXED' | 'FAILED';
  lastSyncedAt: string | null;
  createdAt: string;
}

export interface GitHubRepoDto {
  id: number;
  fullName: string;
  name: string;
  description: string | null;
  language: string | null;
  private: boolean;
  defaultBranch: string;
  stargazersCount: number;
  forksCount: number;
  openIssuesCount: number;
}

export interface RepositoryContributorDto {
  id: string;
  username: string;
  avatarUrl: string | null;
  contributions: number;
}

export interface RepositoryMetricDto {
  id: string;
  contributorCount: number;
  openPrCount: number;
  openIssueCount: number;
  starsCount: number;
  forksCount: number;
  recordedAt: string;
}

export interface RepositoryFileDto {
  id: string;
  path: string;
  extension: string | null;
  sizeBytes: number;
  shaHash: string;
  lastIndexedAt: string | null;
}

export interface CodeChunkDto {
  id: string;
  chunkIndex: number;
  startLine: number;
  endLine: number;
  content: string;
  contentHash: string;
  tokenEstimate: number;
  embeddingStatus: 'NOT_EMBEDDED' | 'EMBEDDING' | 'EMBEDDED' | 'FAILED';
}

export interface VectorStatsDto {
  totalChunks: number;
  embeddedChunks: number;
  pendingChunks: number;
  failedChunks: number;
}

export interface SemanticSearchResultDto {
  chunkId: string;
  repositoryFileId: string;
  filePath: string;
  chunkIndex: number;
  language: string;
  content: string;
  similarityScore: number;
}
