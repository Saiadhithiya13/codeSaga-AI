import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { 
  getRepositoryDetails, 
  syncRepository, 
  disconnectRepository,
  getRepositoryContributors,
  getRepositoryMetrics,
  startIndexing,
  getIndexingStatus,
  getRepositoryFiles,
  getRepositoryChunks,
  startEmbedding,
  searchRepository,
  getVectorStats
} from '@/features/repository/api/repositoryApi'
import type { 
  RepositoryDto, 
  RepositoryContributorDto, 
  RepositoryMetricDto, 
  RepositoryFileDto, 
  CodeChunkDto,
  VectorStatsDto,
  SemanticSearchResultDto
} from '@/types/repository.types'
import { formatDateTime, timeAgo } from '@/utils/formatters'
import RepositoryChatPanel from '@/features/chat/components/RepositoryChatPanel'
import TechnicalDebtDashboard from '@/features/debt/components/TechnicalDebtDashboard'
import PullRequestReviewDashboard from '@/features/prReview/components/PullRequestReviewDashboard'

export default function RepositoryDetailsPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'DEBT' | 'PR_REVIEWS'>('OVERVIEW')
  
  const [repo, setRepo] = useState<RepositoryDto | null>(null)
  const [contributors, setContributors] = useState<RepositoryContributorDto[]>([])
  const [metrics, setMetrics] = useState<RepositoryMetricDto[]>([])
  const [files, setFiles] = useState<RepositoryFileDto[]>([])
  const [chunks, setChunks] = useState<CodeChunkDto[]>([])
  const [vectorStats, setVectorStats] = useState<VectorStatsDto | null>(null)
  
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<SemanticSearchResultDto[]>([])
  const [isSearching, setIsSearching] = useState(false)
  
  const [isLoading, setIsLoading] = useState(true)
  const [isSyncing, setIsSyncing] = useState(false)
  const [isStartingIndex, setIsStartingIndex] = useState(false)
  const [isStartingEmbedding, setIsStartingEmbedding] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadData = async () => {
    if (!id) return
    try {
      const [repoData, contributorsData, metricsData] = await Promise.all([
        getRepositoryDetails(id),
        getRepositoryContributors(id),
        getRepositoryMetrics(id)
      ])
      setRepo(repoData)
      setContributors(contributorsData)
      setMetrics(metricsData)

      if (repoData.indexingStatus === 'INDEXED') {
        const [filesData, chunksData, statsData] = await Promise.all([
          getRepositoryFiles(id),
          getRepositoryChunks(id),
          getVectorStats(id)
        ])
        setFiles(filesData)
        setChunks(chunksData)
        setVectorStats(statsData)
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load repository details')
    }
  }

  useEffect(() => {
    let mounted = true
    const init = async () => {
      setIsLoading(true)
      await loadData()
      if (mounted) setIsLoading(false)
    }
    init()
    return () => { mounted = false }
  }, [id])

  // Poll for indexing status if currently INDEXING
  useEffect(() => {
    if (repo?.indexingStatus !== 'INDEXING') return
    
    const intervalId = setInterval(async () => {
      try {
        const status = await getIndexingStatus(id!)
        if (status !== 'INDEXING') {
          await loadData() // reload everything once finished
        } else {
          setRepo(prev => prev ? { ...prev, indexingStatus: status as any } : prev)
        }
      } catch (e) {}
    }, 3000)
    
    return () => clearInterval(intervalId)
  }, [repo?.indexingStatus, id])

  const handleSync = async () => {
    if (!id || isSyncing) return
    try {
      setIsSyncing(true)
      await syncRepository(id)
      await loadData() // reload fresh data
    } catch (err: any) {
      alert(err.message || 'Failed to sync repository')
    } finally {
      setIsSyncing(false)
    }
  }

  const handleDisconnect = async () => {
    if (!id || !window.confirm('Are you sure you want to disconnect this repository? All historical metrics will be deleted.')) return
    try {
      await disconnectRepository(id)
      navigate('/repositories', { replace: true })
    } catch (err: any) {
      alert(err.message || 'Failed to disconnect repository')
    }
  }

  const handleStartIndexing = async () => {
    if (!id || isStartingIndex) return
    try {
      setIsStartingIndex(true)
      await startIndexing(id)
      setRepo(prev => prev ? { ...prev, indexingStatus: 'INDEXING' } : prev)
    } catch (err: any) {
      alert(err.message || 'Failed to start indexing')
    } finally {
      setIsStartingIndex(false)
    }
  }

  const handleStartEmbedding = async () => {
    if (!id || isStartingEmbedding) return
    try {
      setIsStartingEmbedding(true)
      await startEmbedding(id)
      // Poll stats instead
      const intervalId = setInterval(async () => {
        const stats = await getVectorStats(id)
        setVectorStats(stats)
        if (stats.pendingChunks === 0) clearInterval(intervalId)
      }, 3000)
    } catch (err: any) {
      alert(err.message || 'Failed to start embedding generation')
      setIsStartingEmbedding(false)
    }
  }

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!id || !searchQuery.trim() || isSearching) return
    try {
      setIsSearching(true)
      const results = await searchRepository(id, searchQuery)
      setSearchResults(results)
    } catch (err: any) {
      alert(err.message || 'Search failed')
    } finally {
      setIsSearching(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="w-12 h-12 border-4 border-violet-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (error || !repo) {
    return (
      <div className="p-8 max-w-7xl mx-auto">
        <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-6 rounded-2xl text-center">
          <p className="font-medium text-lg">{error || 'Repository not found'}</p>
          <button onClick={() => navigate('/repositories')} className="mt-4 text-sm underline hover:text-red-300">
            Back to Repositories
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 md:p-8 max-w-7xl mx-auto animate-fade-in space-y-8">
      
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 bg-slate-900 border border-slate-800 p-8 rounded-3xl relative overflow-hidden">
        {/* Background Accent */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-violet-500/5 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 pointer-events-none" />

        <div className="relative z-10 flex-1">
          <div className="flex items-center gap-3 mb-2">
            <h1 className="text-3xl md:text-4xl font-bold text-white tracking-tight">{repo.name}</h1>
            {repo.isPrivate && (
              <span className="px-3 py-1 text-xs font-bold uppercase tracking-wider bg-slate-800 text-slate-300 rounded-full border border-slate-700">
                Private
              </span>
            )}
          </div>
          <p className="text-slate-400 text-lg mb-4">{repo.fullName}</p>
          {repo.description && (
            <p className="text-slate-300 max-w-2xl">{repo.description}</p>
          )}
        </div>
        
        <div className="relative z-10 flex items-center gap-3 w-full md:w-auto">
          <button
            onClick={handleDisconnect}
            className="flex-1 md:flex-none px-4 py-2.5 bg-slate-800 hover:bg-red-500/20 text-slate-300 hover:text-red-400 text-sm font-medium rounded-xl transition-colors border border-slate-700 hover:border-red-500/30"
          >
            Disconnect
          </button>
          <button
            onClick={handleSync}
            disabled={isSyncing}
            className="flex-1 md:flex-none px-6 py-2.5 bg-violet-600 hover:bg-violet-500 text-white text-sm font-medium rounded-xl transition-colors shadow-lg shadow-violet-600/20 flex items-center justify-center gap-2 disabled:opacity-70"
          >
            {isSyncing ? (
              <>
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Syncing...
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Sync Now
              </>
            )}
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-4 border-b border-slate-800">
        <button
          onClick={() => setActiveTab('OVERVIEW')}
          className={`pb-3 text-sm font-medium transition-colors border-b-2 ${
            activeTab === 'OVERVIEW' ? 'border-violet-500 text-white' : 'border-transparent text-slate-500 hover:text-slate-300'
          }`}
        >
          Overview & Chat
        </button>
        <button
          onClick={() => setActiveTab('DEBT')}
          className={`pb-3 text-sm font-medium transition-colors border-b-2 ${
            activeTab === 'DEBT' ? 'border-violet-500 text-white' : 'border-transparent text-slate-500 hover:text-slate-300'
          }`}
        >
          Technical Debt
        </button>
        <button
          onClick={() => setActiveTab('PR_REVIEWS')}
          className={`pb-3 text-sm font-medium transition-colors border-b-2 ${
            activeTab === 'PR_REVIEWS' ? 'border-violet-500 text-white' : 'border-transparent text-slate-500 hover:text-slate-300'
          }`}
        >
          Pull Requests
        </button>
      </div>

      {activeTab === 'OVERVIEW' ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Left Column - Meta & Metrics */}
        <div className="space-y-8">
          
          {/* Metadata Card */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Repository Info</h3>
            <div className="space-y-4">
              <div className="flex justify-between items-center pb-4 border-b border-slate-800/50">
                <span className="text-slate-400 text-sm">Default Branch</span>
                <span className="text-white font-mono text-sm bg-slate-800 px-2 py-0.5 rounded">{repo.defaultBranch}</span>
              </div>
              <div className="flex justify-between items-center pb-4 border-b border-slate-800/50">
                <span className="text-slate-400 text-sm">Language</span>
                <span className="text-white font-medium text-sm flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-violet-400 block"></span>
                  {repo.language || 'Unknown'}
                </span>
              </div>
              <div className="flex justify-between items-center pb-4 border-b border-slate-800/50">
                <span className="text-slate-400 text-sm">Connected On</span>
                <span className="text-slate-300 text-sm" title={formatDateTime(repo.createdAt)}>
                  {timeAgo(repo.createdAt)}
                </span>
              </div>
              <div className="flex justify-between items-center pb-4 border-b border-slate-800/50">
                <span className="text-slate-400 text-sm">Last Synced</span>
                <span className="text-slate-300 text-sm font-medium" title={formatDateTime(repo.lastSyncedAt)}>
                  {repo.lastSyncedAt ? timeAgo(repo.lastSyncedAt) : 'Never'}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400 text-sm">Indexing Status</span>
                <span className={`text-xs font-bold px-2 py-1 rounded-full border ${
                  repo.indexingStatus === 'INDEXED' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' :
                  repo.indexingStatus === 'INDEXING' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20 animate-pulse' :
                  repo.indexingStatus === 'FAILED' ? 'bg-red-500/10 text-red-400 border-red-500/20' :
                  'bg-slate-800 text-slate-400 border-slate-700'
                }`}>
                  {repo.indexingStatus}
                </span>
              </div>
            </div>
          </div>

          {/* AI Code Processing Card */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Code Intelligence</h3>
            
            {repo.indexingStatus === 'INDEXED' ? (
              <div className="space-y-4">
                <div className="flex items-center gap-4 bg-slate-950/50 p-4 rounded-xl border border-slate-800/50">
                  <div className="p-2 bg-violet-500/20 rounded-lg text-violet-400">
                    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-white font-bold text-xl">{files.length}</p>
                    <p className="text-slate-500 text-xs font-medium uppercase tracking-wide">Indexed Files</p>
                  </div>
                </div>
                <div className="flex items-center gap-4 bg-slate-950/50 p-4 rounded-xl border border-slate-800/50">
                  <div className="p-2 bg-indigo-500/20 rounded-lg text-indigo-400">
                    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-white font-bold text-xl">{chunks.length}</p>
                    <p className="text-slate-500 text-xs font-medium uppercase tracking-wide">Code Chunks</p>
                  </div>
                </div>
                <button 
                  onClick={handleStartIndexing}
                  disabled={isStartingIndex}
                  className="w-full mt-2 py-2 text-sm text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors border border-slate-700 disabled:opacity-50"
                >
                  {isStartingIndex ? 'Re-indexing...' : 'Re-index Repository'}
                </button>
              </div>
            ) : repo.indexingStatus === 'INDEXING' ? (
              <div className="text-center p-6 bg-slate-950/50 rounded-xl border border-amber-500/20">
                <div className="w-10 h-10 border-4 border-amber-500/30 border-t-amber-500 rounded-full animate-spin mx-auto mb-4" />
                <p className="text-white font-medium mb-1">Indexing in progress...</p>
                <p className="text-slate-500 text-xs">Cloning, scanning, and chunking repository source code.</p>
              </div>
            ) : (
              <div className="text-center p-6 bg-slate-950/50 rounded-xl border border-slate-800/50">
                <div className="w-12 h-12 bg-slate-900 rounded-full flex items-center justify-center mx-auto mb-4 text-slate-600">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                </div>
                <p className="text-white font-medium mb-2">Not Indexed</p>
                <p className="text-slate-500 text-xs mb-4">Index this repository to enable semantic search and AI code analysis features.</p>
                <button
                  onClick={handleStartIndexing}
                  disabled={isStartingIndex}
                  className="w-full bg-violet-600 hover:bg-violet-500 text-white font-medium py-2 rounded-lg transition-colors shadow-lg shadow-violet-600/20 disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  {isStartingIndex ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      Starting...
                    </>
                  ) : 'Start Indexing'}
                </button>
              </div>
            )}
          </div>

          {/* Semantic Search Foundation */}
          {repo.indexingStatus === 'INDEXED' && vectorStats && (
            <div className="bg-gradient-to-br from-violet-900/40 to-slate-900 border border-violet-500/30 rounded-2xl p-6 relative overflow-hidden">
              <h3 className="text-sm font-semibold text-violet-300 uppercase tracking-wider mb-4">Vector Embeddings</h3>
              
              <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="bg-slate-900/80 border border-slate-700/50 p-4 rounded-xl text-center">
                  <p className="text-2xl font-bold text-white">{vectorStats.embeddedChunks}</p>
                  <p className="text-xs text-slate-400 uppercase tracking-wide mt-1">Embedded</p>
                </div>
                <div className="bg-slate-900/80 border border-slate-700/50 p-4 rounded-xl text-center">
                  <p className="text-2xl font-bold text-amber-400">{vectorStats.pendingChunks}</p>
                  <p className="text-xs text-slate-400 uppercase tracking-wide mt-1">Pending</p>
                </div>
              </div>

              {vectorStats.pendingChunks > 0 ? (
                <button
                  onClick={handleStartEmbedding}
                  disabled={isStartingEmbedding}
                  className="w-full bg-violet-600 hover:bg-violet-500 text-white font-medium py-2 rounded-xl transition-colors shadow-lg shadow-violet-600/20 disabled:opacity-50"
                >
                  {isStartingEmbedding ? 'Generating Embeddings...' : 'Generate Embeddings'}
                </button>
              ) : (
                <div className="w-full py-2 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-center rounded-xl font-medium text-sm">
                  ✓ Vector Database Ready
                </div>
              )}
            </div>
          )}

          {/* GitHub Stats Card */}
          <div className="bg-gradient-to-br from-slate-900 to-slate-950 border border-slate-800 rounded-2xl p-6 relative overflow-hidden">
            <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-6 relative z-10">GitHub Stats</h3>
            <div className="grid grid-cols-2 gap-4 relative z-10">
              <div className="bg-slate-900 border border-slate-800/50 p-4 rounded-xl flex flex-col items-center justify-center text-center">
                <span className="text-2xl font-bold text-white mb-1">{repo.starsCount}</span>
                <span className="text-xs text-slate-500 font-medium uppercase tracking-wide">Stars</span>
              </div>
              <div className="bg-slate-900 border border-slate-800/50 p-4 rounded-xl flex flex-col items-center justify-center text-center">
                <span className="text-2xl font-bold text-white mb-1">{repo.forksCount}</span>
                <span className="text-xs text-slate-500 font-medium uppercase tracking-wide">Forks</span>
              </div>
              <div className="bg-slate-900 border border-slate-800/50 p-4 rounded-xl flex flex-col items-center justify-center text-center">
                <span className="text-2xl font-bold text-violet-400 mb-1">{repo.openIssuesCount}</span>
                <span className="text-xs text-slate-500 font-medium uppercase tracking-wide">Issues & PRs</span>
              </div>
              <div className="bg-slate-900 border border-slate-800/50 p-4 rounded-xl flex flex-col items-center justify-center text-center">
                <span className="text-2xl font-bold text-indigo-400 mb-1">{contributors.length}</span>
                <span className="text-xs text-slate-500 font-medium uppercase tracking-wide">Contributors</span>
              </div>
            </div>
            {/* Decorative Icon */}
            <svg className="absolute -bottom-4 -right-4 w-32 h-32 text-slate-800/30 -rotate-12" fill="currentColor" viewBox="0 0 24 24">
              <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
            </svg>
          </div>
        </div>

        {/* Right Column - Contributors & History */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Contributors */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Top Contributors</h3>
            {contributors.length === 0 ? (
              <div className="p-8 text-center text-slate-500 border border-dashed border-slate-800 rounded-xl">
                No contributors synced yet. Click "Sync Now".
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {contributors.slice(0, 10).map((c, idx) => (
                  <div key={c.id} className="flex items-center gap-4 p-3 rounded-xl bg-slate-950/50 border border-slate-800/50 hover:border-slate-700 transition-colors">
                    <span className="text-xs font-bold text-slate-600 w-4">{idx + 1}.</span>
                    <img src={c.avatarUrl || `https://github.com/identicons/${c.username}.png`} alt={c.username} className="w-10 h-10 rounded-full bg-slate-800" />
                    <div className="flex-1 min-w-0">
                      <p className="text-white font-medium text-sm truncate">{c.username}</p>
                      <p className="text-slate-500 text-xs">{c.contributions} commits</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Sync History / Metrics Table */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 overflow-hidden">
            <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4">Recent Sync History</h3>
            {metrics.length === 0 ? (
              <div className="p-8 text-center text-slate-500 border border-dashed border-slate-800 rounded-xl">
                No sync history available.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm whitespace-nowrap">
                  <thead>
                    <tr className="text-slate-500 border-b border-slate-800">
                      <th className="pb-3 font-medium">Date</th>
                      <th className="pb-3 font-medium text-right">Stars</th>
                      <th className="pb-3 font-medium text-right">Issues/PRs</th>
                      <th className="pb-3 font-medium text-right">Contributors</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/50 text-slate-300">
                    {[...metrics].reverse().slice(0, 5).map(m => (
                      <tr key={m.id} className="hover:bg-slate-800/20 transition-colors">
                        <td className="py-3" title={formatDateTime(m.recordedAt)}>{timeAgo(m.recordedAt)}</td>
                        <td className="py-3 text-right">{m.starsCount}</td>
                        <td className="py-3 text-right">{m.openIssueCount}</td>
                        <td className="py-3 text-right">{m.contributorCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Semantic Search UI */}
          {vectorStats && vectorStats.embeddedChunks > 0 && (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
              <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-4 flex items-center gap-2">
                <svg className="w-4 h-4 text-violet-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                Semantic Search Test
              </h3>
              
              <form onSubmit={handleSearch} className="mb-6 flex gap-2">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="E.g., 'database connection logic' or 'user authentication'"
                  className="flex-1 bg-slate-950 border border-slate-700 rounded-xl px-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition-all text-sm"
                />
                <button
                  type="submit"
                  disabled={isSearching || !searchQuery.trim()}
                  className="px-4 py-2 bg-slate-800 hover:bg-violet-600 text-white text-sm font-medium rounded-xl transition-colors border border-slate-700 hover:border-violet-500 disabled:opacity-50"
                >
                  {isSearching ? 'Searching...' : 'Search'}
                </button>
              </form>

              {searchResults.length > 0 && (
                <div className="space-y-4">
                  <p className="text-xs text-slate-500 uppercase tracking-wider font-medium">Top Results</p>
                  {searchResults.map((res, i) => (
                    <div key={res.chunkId} className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden">
                      <div className="bg-slate-900/50 px-4 py-2 border-b border-slate-800 flex justify-between items-center text-xs">
                        <span className="text-slate-300 font-mono break-all">{res.filePath}</span>
                        <div className="flex items-center gap-3">
                          <span className="text-emerald-400 font-medium">{(res.similarityScore * 100).toFixed(1)}% match</span>
                          <span className="text-slate-500 bg-slate-800 px-2 py-0.5 rounded">Rank {i + 1}</span>
                        </div>
                      </div>
                      <div className="p-4 overflow-x-auto">
                        <pre className="text-xs text-slate-300 font-mono">
                          <code>{res.content}</code>
                        </pre>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Chat Panel */}
          <div className="pt-4 border-t border-slate-800/50 mt-8">
            <RepositoryChatPanel repositoryId={id!} />
          </div>
        </div>
      </div>
      ) : activeTab === 'DEBT' ? (
        <div className="max-w-4xl mx-auto">
          <TechnicalDebtDashboard repositoryId={id!} />
        </div>
      ) : (
        <div className="w-full">
          <PullRequestReviewDashboard repositoryId={id!} />
        </div>
      )}
    </div>
  )
}
