export interface TechnicalDebtReportDto {
  id: string;
  repositoryId: string;
  overallScore: number;
  maintainabilityScore: number;
  complexityScore: number;
  duplicationScore: number;
  aiAssessment: string;
  generatedAt: string;
}

export interface TechnicalDebtFindingDto {
  id: string;
  filePath: string;
  category: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  recommendation: string;
}
