import { Outlet, Link, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/context/AuthContext'

export default function MainLayout() {
  const { user, isAuthenticated } = useAuth()
  const location = useLocation()

  // Define navigation links for the sidebar
  const navLinks = [
    { name: 'Dashboard', path: '/dashboard', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
    { name: 'Repositories', path: '/repositories', icon: 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4' }
  ]

  // If unauthenticated, just render the content without sidebar (e.g. LoginPage, Callback)
  if (!isAuthenticated) {
    return <Outlet />
  }

  return (
    <div className="flex h-screen bg-slate-950 overflow-hidden text-slate-300">
      
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col hidden md:flex">
        <div className="p-6">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center text-white font-bold shadow-lg shadow-violet-500/20">
              ⚡
            </div>
            <span className="font-bold text-lg text-white tracking-tight">CodeSage AI</span>
          </Link>
        </div>

        <nav className="flex-1 px-4 space-y-2 mt-4">
          {navLinks.map((link) => {
            const isActive = location.pathname.startsWith(link.path)
            return (
              <Link
                key={link.name}
                to={link.path}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive 
                    ? 'bg-violet-500/10 text-violet-400 font-semibold' 
                    : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                }`}
              >
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={link.icon} />
                </svg>
                {link.name}
              </Link>
            )
          })}
        </nav>

        {/* User Profile Mini */}
        <div className="p-4 border-t border-slate-800">
          <Link to="/profile" className="flex items-center gap-3 px-2 py-2 rounded-xl hover:bg-slate-800 transition-colors">
            <img 
              src={user?.avatarUrl || `https://github.com/identicons/${user?.login}.png`} 
              alt="Profile" 
              className="w-9 h-9 rounded-full bg-slate-800"
            />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">{user?.name || user?.login}</p>
              <p className="text-xs text-slate-500 truncate">@{user?.login}</p>
            </div>
          </Link>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>

    </div>
  )
}
