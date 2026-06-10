import React, { useState, useEffect } from 'react'
import { TechnicalDebtReportDto, TechnicalDebtFindingDto } from '@/types/technicalDebt.types'
import { getLatestDebtReport, getDebtFindings, triggerDebtAnalysis } from '@/features/debt/api/technicalDebtApi'

interface Props {
  repositoryId: string;
}

export default function TechnicalDebtDashboard({ repositoryId }: Props) {
  const [report, setReport] = useState<TechnicalDebtReportDto | null>(null)
  const [findings, setFindings] = useState<TechnicalDebtFindingDto[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isAnalyzing, setIsAnalyzing] = useState(false)

  const loadData = async () => {
    try {
      const rep = await getLatestDebtReport(repositoryId)
      if (rep) {
        setReport(rep)
        const finds = await getDebtFindings(repositoryId)
        setFindings(finds)
      }
    } catch (e) {
      console.error(e)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [repositoryId])

  const handleAnalyze = async () => {
    try {
      setIsAnalyzing(true)
      await triggerDebtAnalysis(repositoryId)
      // Start polling
      const intervalId = setInterval(async () => {
        const rep = await getLatestDebtReport(repositoryId)
        if (rep && rep.aiAssessment !== 'Analysis in progress...') {
          clearInterval(intervalId)
          await loadData()
          setIsAnalyzing(false)
        }
      }, 5000)
    } catch (e: any) {
      alert('Failed to trigger analysis: ' + e.message)
      setIsAnalyzing(false)
    }
  }

  if (isLoading) {
    return <div className="text-center p-8 text-slate-500">Loading Technical Debt Data...</div>
  }

  if (!report) {
    return (
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-8 text-center">
        <h3 className="text-xl font-bold text-white mb-2">Technical Debt Analyzer</h3>
        <p className="text-slate-400 mb-6 max-w-md mx-auto">
          Scan your repository for code smells, architectural issues, and maintainability concerns using hybrid static and AI analysis.
        </p>
        <button
          onClick={handleAnalyze}
          disabled={isAnalyzing}
          className="bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2 px-6 rounded-xl transition-colors disabled:opacity-50"
        >
          {isAnalyzing ? 'Analysis Started...' : 'Analyze Repository Health'}
        </button>
      </div>
    )
  }

  const getScoreColor = (score: number) => {
    if (score >= 80) return 'text-emerald-400'
    if (score >= 60) return 'text-amber-400'
    return 'text-red-400'
  }

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'bg-red-500/10 text-red-400 border border-red-500/20'
      case 'HIGH': return 'bg-orange-500/10 text-orange-400 border border-orange-500/20'
      case 'MEDIUM': return 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
      case 'LOW': return 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
      default: return 'bg-slate-800 text-slate-400'
    }
  }

  return (
    <div className="space-y-6">
      <div className="bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-800 rounded-2xl p-6">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-bold text-white">Repository Health Score</h3>
          <button
            onClick={handleAnalyze}
            disabled={isAnalyzing || report.aiAssessment === 'Analysis in progress...'}
            className="text-sm bg-slate-800 hover:bg-slate-700 text-white py-1 px-4 rounded-lg disabled:opacity-50"
          >
            {isAnalyzing || report.aiAssessment === 'Analysis in progress...' ? 'Analyzing...' : 'Re-analyze'}
          </button>
        </div>
        
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-8">
          <div className="text-center p-4 bg-slate-900/50 rounded-xl border border-slate-800">
            <div className={`text-4xl font-bold mb-1 ${getScoreColor(report.overallScore)}`}>
              {report.overallScore}
            </div>
            <div className="text-xs text-slate-500 uppercase tracking-wide">Overall</div>
          </div>
          <div className="text-center p-4 bg-slate-900/50 rounded-xl border border-slate-800">
            <div className={`text-2xl font-bold mb-1 ${getScoreColor(report.maintainabilityScore)}`}>
              {report.maintainabilityScore}
            </div>
            <div className="text-xs text-slate-500 uppercase tracking-wide">Maintainability</div>
          </div>
          <div className="text-center p-4 bg-slate-900/50 rounded-xl border border-slate-800">
            <div className={`text-2xl font-bold mb-1 ${getScoreColor(report.complexityScore)}`}>
              {report.complexityScore}
            </div>
            <div className="text-xs text-slate-500 uppercase tracking-wide">Complexity</div>
          </div>
          <div className="text-center p-4 bg-slate-900/50 rounded-xl border border-slate-800">
            <div className={`text-2xl font-bold mb-1 ${getScoreColor(report.duplicationScore)}`}>
              {report.duplicationScore}
            </div>
            <div className="text-xs text-slate-500 uppercase tracking-wide">Duplication</div>
          </div>
        </div>

        <div className="bg-slate-900/50 rounded-xl p-4 border border-slate-800">
          <h4 className="text-sm font-semibold text-slate-300 mb-2">AI Assessment</h4>
          <p className="text-sm text-slate-400 whitespace-pre-wrap">{report.aiAssessment}</p>
        </div>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <h3 className="text-lg font-bold text-white mb-4">Actionable Findings ({findings.length})</h3>
        
        {findings.length === 0 ? (
          <div className="text-center p-8 text-slate-500">No technical debt findings found.</div>
        ) : (
          <div className="space-y-4">
            {findings.map(finding => (
              <div key={finding.id} className="bg-slate-950 border border-slate-800 rounded-xl p-4">
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-2 mb-3">
                  <div className="font-mono text-sm text-indigo-300">{finding.filePath}</div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs bg-slate-800 text-slate-300 px-2 py-1 rounded">
                      {finding.category.replace('_', ' ')}
                    </span>
                    <span className={`text-xs px-2 py-1 rounded font-bold ${getSeverityBadge(finding.severity)}`}>
                      {finding.severity}
                    </span>
                  </div>
                </div>
                <p className="text-sm text-slate-300 mb-2">{finding.description}</p>
                <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-800/50">
                  <span className="text-xs font-semibold text-emerald-500 uppercase tracking-wider block mb-1">Recommendation</span>
                  <p className="text-sm text-slate-400">{finding.recommendation}</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
