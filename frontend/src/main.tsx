import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { router } from './routes/router'
import { queryClient } from './lib/query-client'
import { AuthProvider } from './features/auth/AuthContext'
import { OrganizationProvider } from './features/organizations/OrganizationContext'
import { ThemeProvider } from './features/theme/ThemeContext'
import { ToastProvider } from './features/toast/ToastContext'
import { ToastContainer } from './components/ToastContainer'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <QueryClientProvider client={queryClient}>
          <OrganizationProvider>
            <ToastProvider>
              <RouterProvider router={router} />
              <ToastContainer />
            </ToastProvider>
          </OrganizationProvider>
        </QueryClientProvider>
      </AuthProvider>
    </ThemeProvider>
  </StrictMode>,
)
