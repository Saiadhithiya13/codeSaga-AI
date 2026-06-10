import { useEffect, useState } from 'react'
import { getRemoteRepositories, connectRepository } from '@/features/repository/api/repositoryApi'
import type { GitHubRepoDto } from '@/types/repository.types'

interface ConnectRepositoryModalProps {
  onClose: () => void
  onConnected: () => void
}

export default function ConnectRepositoryModal({ onClose, onConnected }: ConnectRepositoryModalProps) {
  const [repos, setRepos] = useState<GitHubRepoDto[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [connectingRepo, setConnectingRepo] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  useEffect(() => {
    let mounted = true
    const fetchRepos = async () => {
      try {
        const data = await getRemoteRepositories()
        if (mounted) setRepos(data)
      } catch (err: any) {
        if (mounted) setError(err.message || 'Failed to load repositories')
      } finally {
        if (mounted) setIsLoading(false)
      }
    }
    fetchRepos()
    return () => { mounted = false }
  }, [])

  const handleConnect = async (fullName: string) => {
    try {
      setConnectingRepo(fullName)
      await connectRepository(fullName)
      onConnected()
      onClose()
    } catch (err: any) {
      setError(err.message || `Failed to connect ${fullName}`)
      setConnectingRepo(null)
    }
  }

  const filteredRepos = repos.filter(repo => 
    repo.fullName?.toLowerCase().includes(search.toLowerCase()) || false
  )

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden animate-fade-in">
        
        {/* Header */}
        <div className="p-6 border-b border-slate-800 flex justify-between items-center bg-slate-900/50">
          <div>
            <h2 className="text-xl font-bold text-white">Connect Repository</h2>
            <p className="text-sm text-slate-400 mt-1">Select a GitHub repository to analyze.</p>
          </div>
          <button 
            onClick={onClose}
            className="text-slate-400 hover:text-white transition-colors p-2 rounded-lg hover:bg-slate-800"
          >
            ✕
          </button>
        </div>

        {/* Search */}
        <div className="p-4 border-b border-slate-800 bg-slate-900/30">
          <input
            type="text"
            placeholder="Search repositories..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-white placeholder-slate-500 focus:outline-none focus:border-violet-500 focus:ring-1 focus:ring-violet-500 transition-all"
          />
        </div>

        {/* List */}
        <div className="flex-1 overflow-y-auto p-2">
          {isLoading ? (
            <div className="flex justify-center items-center h-48">
              <div className="w-8 h-8 border-4 border-violet-500 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : error ? (
            <div className="p-6 text-center text-red-400 bg-red-400/10 rounded-xl m-4 border border-red-400/20">
              {error}
            </div>
          ) : filteredRepos.length === 0 ? (
            <div className="p-12 text-center text-slate-500">
              No repositories found.
            </div>
          ) : (
            <div className="space-y-2 p-2">
              {filteredRepos.map(repo => (
                <div key={repo.id} className="flex items-center justify-between p-4 rounded-xl hover:bg-slate-800/50 transition-colors border border-transparent hover:border-slate-700 group">
                  <div>
                    <div className="flex items-center gap-3 mb-1">
                      <h3 className="text-white font-medium">{repo.fullName}</h3>
                      {repo.private && (
                        <span className="px-2 py-0.5 text-[10px] font-bold tracking-wide uppercase bg-slate-800 text-slate-400 rounded-full border border-slate-700">
                          Private
                        </span>
                      )}
                    </div>
                    {repo.description && (
                      <p className="text-sm text-slate-400 line-clamp-1">{repo.description}</p>
                    )}
                    <div className="flex gap-4 mt-2 text-xs text-slate-500 font-medium">
                      {repo.language && <span className="flex items-center gap-1">
                        <span className="w-2 h-2 rounded-full bg-violet-400 block"></span>
                        {repo.language}
                      </span>}
                      <span>⭐ {repo.stargazersCount}</span>
                    </div>
                  </div>
                  <button
                    onClick={() => handleConnect(repo.fullName)}
                    disabled={connectingRepo === repo.fullName}
                    className="shrink-0 ml-4 px-4 py-2 bg-slate-800 hover:bg-violet-600 text-white text-sm font-medium rounded-lg transition-colors border border-slate-700 hover:border-violet-500 disabled:opacity-50 flex items-center gap-2"
                  >
                    {connectingRepo === repo.fullName ? (
                      <>
                        <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                        Connecting...
                      </>
                    ) : 'Connect'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
