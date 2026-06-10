import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '@/features/auth/context/AuthContext'
import { githubCallback } from '@/features/auth/api/authApi'

export default function AuthCallbackPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { login } = useAuth()
  
  const [error, setError] = useState<string | null>(null)
  
  // Prevent strict mode double-firing from doing 2 OAuth exchanges
  const processedRef = useRef(false)

  useEffect(() => {
    if (processedRef.current) return
    processedRef.current = true

    const code = searchParams.get('code')
    const state = searchParams.get('state')
    const savedState = sessionStorage.getItem('oauth_state')

    if (!code) {
      setError('No authorization code received from GitHub.')
      return
    }

    if (!state || state !== savedState) {
      setError('Security verification failed. CSRF state mismatch.')
      return
    }

    // Clear state
    sessionStorage.removeItem('oauth_state')

    const processAuth = async () => {
      try {
        const authData = await githubCallback({ code, state })
        login(authData.user)
        
        // Success! Redirect to profile or dashboard
        navigate('/profile', { replace: true })
        
      } catch (err: any) {
        setError(err.message || 'Authentication failed. Please try again.')
      }
    }

    processAuth()
  }, [searchParams, navigate, login])

  if (error) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4 text-center">
        <div className="w-16 h-16 bg-red-500/10 rounded-full flex items-center justify-center text-red-400 mb-6 border border-red-500/20">
          <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <h1 className="text-xl font-bold text-white mb-2">Authentication Failed</h1>
        <p className="text-slate-400 mb-8 max-w-md">{error}</p>
        <button
          onClick={() => navigate('/login', { replace: true })}
          className="bg-violet-600 hover:bg-violet-500 text-white font-medium py-2.5 px-6 rounded-lg transition-colors"
        >
          Return to Login
        </button>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4 text-center">
      <div className="w-12 h-12 border-4 border-violet-500 border-t-transparent rounded-full animate-spin mb-6" />
      <h1 className="text-xl font-medium text-white mb-2">Connecting to GitHub</h1>
      <p className="text-slate-400 text-sm">Exchanging secure tokens and syncing your profile…</p>
    </div>
  )
}
