import { createBrowserRouter, Navigate } from 'react-router-dom'
import MainLayout from '@/layouts/MainLayout'
import LoginPage from '@/features/auth/pages/LoginPage'
import AuthCallbackPage from '@/features/auth/pages/AuthCallbackPage'
import ProfilePage from '@/features/auth/pages/ProfilePage'
import RepositoriesPage from '@/features/repository/pages/RepositoriesPage'
import RepositoryDetailsPage from '@/features/repository/pages/RepositoryDetailsPage'
import ProtectedRoute from './ProtectedRoute'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <Navigate to="/dashboard" replace />
      },
      // Public Auth Routes
      {
        path: 'login',
        element: <LoginPage />
      },
      {
        path: 'auth/callback',
        element: <AuthCallbackPage />
      },
      // Protected Routes
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: 'dashboard',
            element: (
              <div className="p-8">
                <h1 className="text-3xl font-bold text-white">Dashboard</h1>
                <p className="text-slate-400 mt-2">Welcome back to CodeSage AI.</p>
              </div>
            )
          },
          {
            path: 'profile',
            element: <ProfilePage />
          },
          {
            path: 'repositories',
            element: <RepositoriesPage />
          },
          {
            path: 'repositories/:id',
            element: <RepositoryDetailsPage />
          }
        ]
      }
    ]
  }
])
