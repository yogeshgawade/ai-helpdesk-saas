import { createBrowserRouter, Navigate } from 'react-router-dom'
import AppLayout from '../layouts/AppLayout'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import DashboardPage from '../pages/DashboardPage'
import TicketsPage from '../pages/TicketsPage'
import TicketDetailPage from '../pages/TicketDetailPage'
import KnowledgeBasePage from '../pages/KnowledgeBasePage'
import SlaPoliciesPage from '../pages/SlaPoliciesPage'
import AnalyticsPage from '../pages/AnalyticsPage'
import ProtectedRoute from './ProtectedRoute'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/login" replace />,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/app',
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            index: true,
            element: <Navigate to="/app/dashboard" replace />,
          },
          {
            path: 'dashboard',
            element: <DashboardPage />,
          },
          {
            path: 'tickets',
            element: <TicketsPage />,
          },
          {
            path: 'tickets/:ticketId',
            element: <TicketDetailPage />,
          },
          {
            path: 'knowledge-base',
            element: <KnowledgeBasePage />,
          },
          {
            path: 'sla-policies',
            element: <SlaPoliciesPage />,
          },
          {
            path: 'analytics',
            element: <AnalyticsPage />,
          },
        ],
      },
    ],
  },
])
