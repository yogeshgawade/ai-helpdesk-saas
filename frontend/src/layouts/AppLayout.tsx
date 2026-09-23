import { useEffect, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import { useAppWebSocket } from '../hooks/useAppWebSocket'
import { NotificationBell } from '../components/NotificationBell'

function AppLayout() {
  const {
    organizations,
    activeOrganizationId,
    setActiveOrganizationId,
  } = useOrganizations()

  const navigate = useNavigate()
  const location = useLocation()

  useAppWebSocket(activeOrganizationId)

  const [pendingOrganizationId, setPendingOrganizationId] =
    useState<string | null>(null)

  useEffect(() => {
    if (
      pendingOrganizationId !== null &&
      location.pathname === '/app/tickets'
    ) {
      setActiveOrganizationId(pendingOrganizationId)
      setPendingOrganizationId(null)
      return
    }

    const navigationState = location.state as
      | {
          targetTicketId?: string
          targetOrganizationId?: string
        }
      | null

    if (
      location.pathname !== '/app/tickets' ||
      !navigationState?.targetTicketId ||
      !navigationState.targetOrganizationId
    ) {
      return
    }

    if (
      navigationState.targetOrganizationId !== activeOrganizationId
    ) {
      setActiveOrganizationId(
        navigationState.targetOrganizationId,
      )
      return
    }

    navigate(
      `/app/tickets/${navigationState.targetTicketId}`,
      { replace: true, state: null },
    )
  }, [
    location.pathname,
    location.state,
    pendingOrganizationId,
    activeOrganizationId,
    navigate,
    setActiveOrganizationId,
  ])

  function handleOrganizationChange(organizationId: string) {
    if (location.pathname.startsWith('/app/tickets/')) {
      setPendingOrganizationId(organizationId)
      navigate('/app/tickets', { replace: true })
      return
    }

    setActiveOrganizationId(organizationId)
  }

  return (
    <div className="min-h-screen bg-slate-950 text-white">
      <header className="border-b border-slate-800 bg-slate-900">
        <div className="flex items-center justify-between px-6 py-4">
          <h1 className="text-lg font-semibold">AI Helpdesk</h1>

          <div className="flex items-center gap-4">
            <nav className="flex items-center gap-3 text-sm">
              <button
                type="button"
                onClick={() => navigate('/app/dashboard')}
                className="text-slate-300 hover:text-white"
              >
                Dashboard
              </button>
              <button
                type="button"
                onClick={() => navigate('/app/tickets')}
                className="text-slate-300 hover:text-white"
              >
                Tickets
              </button>
              <button
                type="button"
                onClick={() => navigate('/app/knowledge-base')}
                className="text-slate-300 hover:text-white"
              >
                Knowledge Base
              </button>
              <button
                type="button"
                onClick={() => navigate('/app/sla-policies')}
                className="text-slate-300 hover:text-white"
              >
                SLA Policies
              </button>
              <button
                type="button"
                onClick={() => navigate('/app/analytics')}
                className="text-slate-300 hover:text-white"
              >
                Analytics
              </button>
            </nav>

            <NotificationBell />

            <select
              value={activeOrganizationId ?? ''}
              onChange={(event) =>
                handleOrganizationChange(event.target.value)
              }
              className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white outline-none"
            >
              {organizations.map((organization) => (
                <option key={organization.id} value={organization.id}>
                  {organization.name} ({organization.role})
                </option>
              ))}
            </select>
          </div>
        </div>
      </header>

      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default AppLayout
