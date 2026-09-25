import { useEffect, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import { useAppWebSocket } from '../hooks/useAppWebSocket'
import Sidebar from './Sidebar'
import TopBar from './TopBar'

function AppLayout() {
  const {
    activeOrganizationId,
    setActiveOrganizationId,
  } = useOrganizations()

  const navigate = useNavigate()
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const [pendingOrganizationId, setPendingOrganizationId] =
    useState<string | null>(null)

  useAppWebSocket(activeOrganizationId)

  useEffect(() => {
    if (
      pendingOrganizationId !== null &&
      location.pathname === '/app/tickets'
    ) {
      setActiveOrganizationId(pendingOrganizationId)
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
      navigationState.targetOrganizationId !==
      activeOrganizationId
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

  function handleMobileMenuClose() {
    setMobileNavOpen(false)
  }

  return (
    <div className="min-h-screen bg-[var(--app-bg)] text-[var(--app-text)]">
      <Sidebar
        mobileOpen={mobileNavOpen}
        onMobileClose={handleMobileMenuClose}
        onOrganizationChange={handleOrganizationChange}
      />

      <div className="min-h-screen transition-[padding] duration-200 lg:pl-72">
        <TopBar onMenuClick={() => setMobileNavOpen(true)} />

        <main className="mx-auto w-full max-w-[1600px] px-4 py-6 sm:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AppLayout
