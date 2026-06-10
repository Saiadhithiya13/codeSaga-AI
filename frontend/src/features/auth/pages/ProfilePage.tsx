import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/features/auth/context/AuthContext'
import { formatDateTime, timeAgo } from '@/utils/formatters'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  if (!user) return null

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-slate-950 p-6 md:p-12">
      <div className="max-w-3xl mx-auto space-y-8 animate-fade-in">
        
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-white tracking-tight">Profile</h1>
            <p className="text-slate-400 mt-1">Manage your account and authentication state.</p>
          </div>
          <button
            onClick={handleLogout}
            className="text-sm font-medium text-slate-300 bg-slate-800/50 hover:bg-slate-800 px-4 py-2 rounded-lg transition-colors border border-slate-700 hover:border-slate-600"
          >
            Sign Out
          </button>
        </div>

        {/* Profile Card */}
        <div className="bg-slate-900/60 backdrop-blur-sm border border-slate-800 rounded-2xl p-6 md:p-8 shadow-2xl">
          <div className="flex flex-col md:flex-row gap-8 items-start md:items-center">
            
            {/* Avatar */}
            {user.avatarUrl ? (
              <img 
                src={user.avatarUrl} 
                alt={`${user.login}'s avatar`}
                className="w-24 h-24 rounded-full border-2 border-slate-800 shadow-xl bg-slate-800"
              />
            ) : (
              <div className="w-24 h-24 rounded-full bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center text-3xl font-bold text-white shadow-xl">
                {user.login.charAt(0).toUpperCase()}
              </div>
            )}

            {/* Info */}
            <div className="flex-1">
              <div className="flex items-center gap-3 mb-1">
                <h2 className="text-2xl font-bold text-white">{user.name || user.login}</h2>
                <span className="px-2.5 py-0.5 rounded-full bg-violet-500/10 text-violet-400 text-xs font-semibold tracking-wide border border-violet-500/20">
                  {user.role}
                </span>
              </div>
              <p className="text-slate-400 font-medium mb-4">@{user.login}</p>
              
              <div className="flex flex-wrap gap-x-6 gap-y-3 text-sm text-slate-300">
                <div className="flex items-center gap-2">
                  <svg className="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                  </svg>
                  {user.email || 'No public email'}
                </div>
                <div className="flex items-center gap-2" title={formatDateTime(user.lastLoginAt)}>
                  <svg className="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Last active: {timeAgo(user.lastLoginAt)}
                </div>
              </div>
            </div>

            {/* GitHub Badge */}
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 flex items-center gap-3 w-full md:w-auto">
              <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
                <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
              </svg>
              <div>
                <p className="text-xs text-slate-400 font-medium leading-tight">Connected via</p>
                <p className="text-sm text-slate-200 font-bold leading-tight">GitHub OAuth</p>
              </div>
            </div>

          </div>
        </div>

        {/* System IDs (Debug) */}
        <div className="bg-slate-900/40 rounded-xl p-6 border border-slate-800/50">
          <h3 className="text-sm font-semibold text-slate-300 uppercase tracking-wider mb-4">System Identifiers</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p className="text-xs text-slate-500 mb-1">Internal User UUID</p>
              <code className="text-xs text-violet-300 bg-violet-500/10 px-2 py-1 rounded font-mono select-all">
                {user.id}
              </code>
            </div>
            <div>
              <p className="text-xs text-slate-500 mb-1">Account Created</p>
              <p className="text-sm text-slate-300 font-mono">
                {formatDateTime(user.createdAt)}
              </p>
            </div>
          </div>
        </div>

      </div>
    </div>
  )
}
