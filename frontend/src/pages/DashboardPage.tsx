import { useOrganizations } from '../features/organizations/OrganizationContext'

function DashboardPage() {
  const {
    organizations,
    activeOrganization,
    activeOrganizationId,
    isLoading,
    error,
  } = useOrganizations()

  if (isLoading) {
    return <div className="p-6">Loading organizations...</div>
  }

  if (error) {
    return (
      <div className="p-6 text-red-400">
        Failed to load organizations.
      </div>
    )
  }

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold">Dashboard</h1>

      <div className="mt-6 space-y-2">
        <p>
          Organizations: <strong>{organizations.length}</strong>
        </p>

        <p>
          Active organization:{' '}
          <strong>{activeOrganization?.name ?? 'None'}</strong>
        </p>

        <p>
          Active organization ID:{' '}
          <strong>{activeOrganizationId ?? 'None'}</strong>
        </p>

        <p>
          Role: <strong>{activeOrganization?.role ?? 'None'}</strong>
        </p>
      </div>
    </div>
  )
}

export default DashboardPage
