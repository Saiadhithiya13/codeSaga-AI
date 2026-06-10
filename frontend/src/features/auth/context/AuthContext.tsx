import { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import type { UserResponse } from '@/types/api.types'
import { fetchMe } from '@/features/auth/api/userApi'
import { logout as apiLogout } from '@/features/auth/api/authApi'

interface AuthContextType {
  user: UserResponse | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (user: UserResponse) => void
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    let mounted = true

    const loadUser = async () => {
      try {
        const userData = await fetchMe()
        if (mounted) {
          setUser(userData)
        }
      } catch (err) {
        // Not authenticated or network error
        if (mounted) {
          setUser(null)
        }
      } finally {
        if (mounted) {
          setIsLoading(false)
        }
      }
    }

    loadUser()

    return () => {
      mounted = false
    }
  }, [])

  const login = (newUser: UserResponse) => {
    setUser(newUser)
  }

  const logout = async () => {
    try {
      await apiLogout()
    } catch (err) {
      console.warn('Logout API failed, clearing local state anyway', err)
    } finally {
      setUser(null)
    }
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
