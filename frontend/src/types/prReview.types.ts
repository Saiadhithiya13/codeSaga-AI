export interface PullRequestReviewDto {
  id: string;
  repositoryId: string;
  githubPrId: string;
  title: string;
  reviewSummary: string;
  riskScore: number;
  createdAt: string;
}

export interface PullRequestFindingDto {
  id: string;
  filePath: string;
  category: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  confidenceScore: number;
  description: string;
  recommendation: string;
}
