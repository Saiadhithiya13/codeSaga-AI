import { useEffect, useState } from 'react'
import { fetchHealth } from '@/features/health/api/healthApi'
import type { HealthResponse } from '@/types/api.types'

/**
 * Status page — displays real-time infrastructure health for CodeSage AI.
 *
 * Polls GET /api/v1/health every 30 seconds to keep status fresh.
 * Shows per-component (database, redis) status with color indicators.
 */
export default function StatusPage() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastChecked, setLastChecked] = useState<Date | null>(null)

  const check = async () => {
    try {
      const data = await fetchHealth()
      setHealth(data)
      setError(null)
      setLastChecked(new Date())
    } catch {
      setError('Unable to reach the backend. Is it running?')
      setHealth(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    check()
    const interval = setInterval(check, 30_000)
    return () => clearInterval(interval)
  }, [])

  const statusColor = (status: string) => {
    switch (status) {
      case 'UP':       return 'text-emerald-400'
      case 'DEGRADED': return 'text-amber-400'
      case 'DOWN':     return 'text-red-400'
      default:         return 'text-slate-400'
    }
  }

  const statusBg = (status: string) => {
    switch (status) {
      case 'UP':       return 'bg-emerald-500/10 border-emerald-500/20'
      case 'DEGRADED': return 'bg-amber-500/10 border-amber-500/20'
      case 'DOWN':     return 'bg-red-500/10 border-red-500/20'
      default:         return 'bg-slate-500/10 border-slate-500/20'
    }
  }

  const dotColor = (status: string) => {
    switch (status) {
      case 'UP':   return 'bg-emerald-400'
      case 'DOWN': return 'bg-red-400'
      default:     return 'bg-amber-400'
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col items-center justify-center p-8">

      {/* Header */}
      <div className="mb-12 text-center">
        <div className="inline-flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-indigo-600 flex items-center justify-center text-xl font-bold shadow-lg shadow-violet-500/25">
            ⚡
          </div>
          <h1 className="text-3xl font-bold tracking-tight bg-gradient-to-r from-violet-400 to-indigo-400 bg-clip-text text-transparent">
            CodeSage AI
          </h1>
        </div>
        <p className="text-slate-400 text-sm">Infrastructure Status Dashboard</p>
      </div>

      {/* Status Card */}
      <div className="w-full max-w-lg">
        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-sm p-6 shadow-2xl">

          {loading && (
            <div className="flex items-center justify-center gap-3 py-8">
              <div className="w-5 h-5 border-2 border-violet-500 border-t-transparent rounded-full animate-spin" />
              <span className="text-slate-400 text-sm">Checking infrastructure…</span>
            </div>
          )}

          {error && !loading && (
            <div className="rounded-xl border border-red-500/20 bg-red-500/10 px-5 py-4">
              <div className="flex items-center gap-2 mb-1">
                <div className="w-2 h-2 rounded-full bg-red-400 animate-pulse" />
                <span className="text-red-400 font-semibold text-sm">Backend Unreachable</span>
              </div>
              <p className="text-slate-400 text-sm">{error}</p>
            </div>
          )}

          {health && !loading && (
            <div className="space-y-5">

              {/* Overall Status */}
              <div className={`rounded-xl border px-5 py-4 ${statusBg(health.status)}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`w-2.5 h-2.5 rounded-full ${dotColor(health.status)} ${health.status === 'UP' ? 'animate-pulse' : ''}`} />
                    <span className="text-slate-300 text-sm font-medium">Overall Status</span>
                  </div>
                  <span className={`text-sm font-bold tracking-wide ${statusColor(health.status)}`}>
                    {health.status}
                  </span>
                </div>
              </div>

              {/* Components */}
              <div className="space-y-2">
                <p className="text-xs text-slate-500 uppercase tracking-wider font-medium px-1">
                  Infrastructure Components
                </p>
                {Object.entries(health.components).map(([name, component]) => (
                  <div
                    key={name}
                    className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-900 px-4 py-3"
                  >
                    <div className="flex items-center gap-2.5">
                      <div className={`w-2 h-2 rounded-full ${dotColor(component.status)}`} />
                      <span className="text-slate-300 text-sm capitalize">{name}</span>
                      {component.details && (
                        <span className="text-xs text-slate-500 truncate max-w-48" title={component.details}>
                          {component.details}
                        </span>
                      )}
                    </div>
                    <span className={`text-xs font-semibold ${statusColor(component.status)}`}>
                      {component.status}
                    </span>
                  </div>
                ))}
              </div>

              {/* Metadata */}
              <div className="border-t border-slate-800 pt-4 grid grid-cols-2 gap-y-2 text-xs">
                <span className="text-slate-500">Version</span>
                <span className="text-slate-400 text-right font-mono">{health.version}</span>
                <span className="text-slate-500">Environment</span>
                <span className="text-slate-400 text-right capitalize">{health.environment}</span>
                <span className="text-slate-500">Last checked</span>
                <span className="text-slate-400 text-right">
                  {lastChecked ? lastChecked.toLocaleTimeString() : '—'}
                </span>
              </div>
            </div>
          )}

        </div>

        {/* Refresh Button */}
        <button
          onClick={() => { setLoading(true); check() }}
          disabled={loading}
          className="mt-4 w-full rounded-xl border border-slate-800 bg-slate-900 hover:bg-slate-800 transition-colors text-slate-400 hover:text-slate-200 text-sm py-2.5 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? 'Checking…' : 'Refresh Now'}
        </button>
      </div>

      {/* Footer */}
      <p className="mt-10 text-slate-700 text-xs">
        Sprint 1 — Infrastructure Foundation
      </p>
    </div>
  )
}
