import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getConnectedRepositories } from '@/features/repository/api/repositoryApi'
import type { RepositoryDto } from '@/types/repository.types'
import ConnectRepositoryModal from '@/features/repository/components/ConnectRepositoryModal'
import { timeAgo } from '@/utils/formatters'

export default function RepositoriesPage() {
  const [repositories, setRepositories] = useState<RepositoryDto[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const fetchRepos = async () => {
    try {
      setIsLoading(true)
      const data = await getConnectedRepositories()
      setRepositories(data)
    } catch (err) {
      console.error('Failed to load connected repositories', err)
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchRepos()
  }, [])

  return (
    <div className="p-6 md:p-8 max-w-7xl mx-auto animate-fade-in">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white">Repositories</h1>
          <p className="text-slate-400 mt-1">Manage connected GitHub repositories for CodeSage analysis.</p>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="bg-violet-600 hover:bg-violet-500 text-white font-medium py-2 px-6 rounded-xl transition-colors shadow-lg shadow-violet-600/20 flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Connect Repository
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-48 bg-slate-900/50 rounded-2xl animate-pulse border border-slate-800"></div>
          ))}
        </div>
      ) : repositories.length === 0 ? (
        <div className="bg-slate-900/30 border border-slate-800 rounded-3xl p-12 text-center flex flex-col items-center justify-center">
          <div className="w-20 h-20 bg-slate-800/50 rounded-full flex items-center justify-center mb-6 text-slate-500">
            <svg className="w-10 h-10" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-white mb-2">No Repositories Connected</h2>
          <p className="text-slate-400 max-w-md mb-8">
            Connect a GitHub repository to start tracking metrics, analyzing code, and utilizing AI-powered intelligence.
          </p>
          <button
            onClick={() => setIsModalOpen(true)}
            className="bg-slate-800 hover:bg-slate-700 text-white font-medium py-3 px-8 rounded-xl transition-colors border border-slate-700"
          >
            Connect Your First Repository
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {repositories.map(repo => (
            <Link 
              key={repo.id} 
              to={`/repositories/${repo.id}`}
              className="bg-slate-900 border border-slate-800 hover:border-violet-500/50 rounded-2xl p-6 transition-all hover:-translate-y-1 hover:shadow-xl hover:shadow-violet-500/10 group flex flex-col"
            >
              <div className="flex items-start justify-between mb-4">
                <h3 className="text-lg font-bold text-white group-hover:text-violet-400 transition-colors break-all pr-4">
                  {repo.fullName}
                </h3>
                {repo.isPrivate && (
                  <span className="shrink-0 px-2 py-1 text-[10px] font-bold uppercase tracking-wider bg-slate-800 text-slate-400 rounded-full border border-slate-700">
                    Private
                  </span>
                )}
              </div>
              
              <p className="text-sm text-slate-400 mb-6 line-clamp-2 flex-1">
                {repo.description || 'No description provided.'}
              </p>
              
              <div className="grid grid-cols-2 gap-4 text-sm mt-auto pt-4 border-t border-slate-800/50">
                <div>
                  <p className="text-slate-500 text-xs mb-1">Language</p>
                  <p className="text-slate-300 font-medium">{repo.language || 'Mixed'}</p>
                </div>
                <div>
                  <p className="text-slate-500 text-xs mb-1">Last Synced</p>
                  <p className="text-slate-300 font-medium truncate" title={timeAgo(repo.lastSyncedAt)}>
                    {repo.lastSyncedAt ? timeAgo(repo.lastSyncedAt) : 'Never'}
                  </p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {isModalOpen && (
        <ConnectRepositoryModal 
          onClose={() => setIsModalOpen(false)} 
          onConnected={fetchRepos} 
        />
      )}
    </div>
  )
}
