/**
 * Utility functions for formatting data.
 */

/**
 * Formats an ISO datetime string into a human-readable local date and time.
 */
export function formatDateTime(isoString: string | null | undefined): string {
  if (!isoString) return 'Never'
  
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return 'Invalid Date'

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  }).format(date)
}

/**
 * Returns a relative time string (e.g., "2 hours ago").
 */
export function timeAgo(isoString: string | null | undefined): string {
  if (!isoString) return 'Never'
  
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return 'Invalid Date'

  const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000)
  
  let interval = seconds / 31536000
  if (interval > 1) return Math.floor(interval) + ' years ago'
  
  interval = seconds / 2592000
  if (interval > 1) return Math.floor(interval) + ' months ago'
  
  interval = seconds / 86400
  if (interval > 1) return Math.floor(interval) + ' days ago'
  
  interval = seconds / 3600
  if (interval > 1) return Math.floor(interval) + ' hours ago'
  
  interval = seconds / 60
  if (interval > 1) return Math.floor(interval) + ' minutes ago'
  
  return 'Just now'
}
