import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { router } from './routes/router'
import { queryClient } from './lib/query-client'
import { AuthProvider } from './features/auth/AuthContext'
import { OrganizationProvider } from './features/organizations/OrganizationContext'
import './index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <OrganizationProvider>
          <RouterProvider router={router} />
        </OrganizationProvider>
      </QueryClientProvider>
    </AuthProvider>
  </StrictMode>,
)
