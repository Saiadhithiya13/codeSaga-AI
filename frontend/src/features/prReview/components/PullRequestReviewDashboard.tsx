import React, { useState, useEffect } from 'react'
import { PullRequestReviewDto, PullRequestFindingDto } from '@/types/prReview.types'
import { triggerPrReview, getPrReviews, getPrFindings } from '@/features/prReview/api/prReviewApi'
import { formatDateTime } from '@/utils/formatters'

interface Props {
  repositoryId: string;
}

export default function PullRequestReviewDashboard({ repositoryId }: Props) {
  const [reviews, setReviews] = useState<PullRequestReviewDto[]>([])
  const [selectedReview, setSelectedReview] = useState<PullRequestReviewDto | null>(null)
  const [findings, setFindings] = useState<PullRequestFindingDto[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [prInput, setPrInput] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const loadReviews = async () => {
    try {
      const data = await getPrReviews(repositoryId)
      setReviews(data)
    } catch (e) {
      console.error(e)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadReviews()
  }, [repositoryId])

  const handleTrigger = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!prInput.trim()) return
    try {
      setIsSubmitting(true)
      await triggerPrReview(repositoryId, prInput)
      setPrInput('')
      alert('PR Review triggered! It will appear here shortly.')
      // Auto-refresh after 5 seconds to show in-progress state
      setTimeout(loadReviews, 5000)
    } catch (e: any) {
      alert('Failed to trigger review: ' + e.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSelectReview = async (review: PullRequestReviewDto) => {
    setSelectedReview(review)
    setFindings([])
    if (review.reviewSummary !== 'Review in progress...') {
      try {
        const f = await getPrFindings(repositoryId, review.id)
        setFindings(f)
      } catch (e) {
        console.error(e)
      }
    }
  }

  const getRiskColor = (score: number) => {
    if (score >= 81) return 'text-red-500'
    if (score >= 51) return 'text-orange-500'
    if (score >= 21) return 'text-amber-500'
    return 'text-emerald-500'
  }

  const getRiskLabel = (score: number) => {
    if (score >= 81) return 'Critical Risk'
    if (score >= 51) return 'High Risk'
    if (score >= 21) return 'Medium Risk'
    return 'Low Risk'
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

  if (isLoading) {
    return <div className="text-center p-8 text-slate-500">Loading PR Reviews...</div>
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
      
      {/* Sidebar: New Review + History */}
      <div className="lg:col-span-1 space-y-6">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <h3 className="text-sm font-bold text-white mb-4">Analyze Pull Request</h3>
          <form onSubmit={handleTrigger} className="flex gap-2">
            <input
              type="text"
              value={prInput}
              onChange={(e) => setPrInput(e.target.value)}
              placeholder="e.g. 42"
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-violet-500"
            />
            <button
              type="submit"
              disabled={isSubmitting || !prInput.trim()}
              className="bg-violet-600 hover:bg-violet-500 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
            >
              Run
            </button>
          </form>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden flex flex-col max-h-[600px]">
          <div className="p-4 border-b border-slate-800 bg-slate-900/50">
            <h3 className="text-sm font-bold text-white">Review History</h3>
          </div>
          <div className="overflow-y-auto flex-1 p-2 space-y-1">
            {reviews.length === 0 ? (
              <p className="text-xs text-slate-500 text-center p-4">No reviews yet.</p>
            ) : (
              reviews.map(rev => (
                <button
                  key={rev.id}
                  onClick={() => handleSelectReview(rev)}
                  className={`w-full text-left p-3 rounded-lg transition-colors border ${
                    selectedReview?.id === rev.id
                      ? 'bg-violet-500/10 border-violet-500/30'
                      : 'border-transparent hover:bg-slate-800/50'
                  }`}
                >
                  <div className="text-xs font-mono text-slate-400 mb-1">PR #{rev.githubPrId}</div>
                  <div className="text-sm font-medium text-white truncate mb-2">{rev.title}</div>
                  <div className="flex justify-between items-center text-xs">
                    <span className={getRiskColor(rev.riskScore)}>Risk: {rev.riskScore}</span>
                    <span className="text-slate-500">{formatDateTime(rev.createdAt)}</span>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Main Area: Review Details */}
      <div className="lg:col-span-3">
        {!selectedReview ? (
          <div className="h-full min-h-[400px] flex items-center justify-center bg-slate-900/50 border border-slate-800 border-dashed rounded-2xl text-slate-500">
            Select a PR review from the sidebar or run a new one.
          </div>
        ) : (
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:p-8 shadow-xl">
            <div className="flex flex-col md:flex-row justify-between items-start gap-4 mb-8">
              <div>
                <div className="text-sm text-violet-400 font-mono mb-2">Pull Request #{selectedReview.githubPrId}</div>
                <h2 className="text-2xl font-bold text-white mb-2">{selectedReview.title}</h2>
                <div className="text-xs text-slate-400">Reviewed {formatDateTime(selectedReview.createdAt)}</div>
              </div>
              
              <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 text-center min-w-[150px]">
                <div className={`text-3xl font-black mb-1 ${getRiskColor(selectedReview.riskScore)}`}>
                  {selectedReview.riskScore}
                </div>
                <div className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
                  {getRiskLabel(selectedReview.riskScore)}
                </div>
              </div>
            </div>

            <div className="bg-slate-800/30 border border-slate-800 rounded-xl p-5 mb-8">
              <h3 className="text-sm font-bold text-slate-300 mb-3 uppercase tracking-wider">AI Review Summary</h3>
              <p className="text-slate-300 whitespace-pre-wrap leading-relaxed">
                {selectedReview.reviewSummary}
              </p>
            </div>

            {selectedReview.reviewSummary !== 'Review in progress...' && (
              <div>
                <h3 className="text-lg font-bold text-white mb-4">Findings & Recommendations ({findings.length})</h3>
                {findings.length === 0 ? (
                  <div className="p-8 text-center text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
                    LGTM! No significant issues found in this pull request.
                  </div>
                ) : (
                  <div className="space-y-4">
                    {findings.map(finding => (
                      <div key={finding.id} className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden">
                        <div className="bg-slate-900 px-4 py-3 border-b border-slate-800 flex flex-wrap justify-between items-center gap-2">
                          <div className="font-mono text-xs text-indigo-300 break-all">{finding.filePath}</div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs bg-slate-800 text-slate-300 px-2 py-1 rounded">
                              {finding.category}
                            </span>
                            <span className="text-xs bg-slate-800 text-slate-400 px-2 py-1 rounded">
                              Conf: {finding.confidenceScore}%
                            </span>
                            <span className={`text-xs px-2 py-1 rounded font-bold ${getSeverityBadge(finding.severity)}`}>
                              {finding.severity}
                            </span>
                          </div>
                        </div>
                        <div className="p-4">
                          <p className="text-sm text-slate-300 mb-3">{finding.description}</p>
                          <div className="bg-indigo-950/20 p-3 rounded-lg border border-indigo-500/10">
                            <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider block mb-1">Recommendation</span>
                            <p className="text-sm text-indigo-200">{finding.recommendation}</p>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
