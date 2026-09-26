import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useQuery } from '@tanstack/react-query'
import { getMyOrganizations } from '../../api/organizations'
import type { Organization } from './types'
import { useAuth } from '../auth/AuthContext'

interface OrganizationContextValue {
  organizations: Organization[]
  activeOrganization: Organization | null
  activeOrganizationId: string | null
  setActiveOrganizationId: (organizationId: string) => void
  isLoading: boolean
  error: Error | null
}

const OrganizationContext =
  createContext<OrganizationContextValue | undefined>(undefined)

export function OrganizationProvider({
  children,
}: {
  children: ReactNode
}) {
  const { user, isAuthenticated } = useAuth()

  const [selectedOrganizationId, setSelectedOrganizationId] =
    useState<string | null>(null)

  const {
    data: organizations = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['my-organizations', user?.id ?? null],
    queryFn: getMyOrganizations,
    enabled: isAuthenticated && user !== null,
  })

  const activeOrganizationId = useMemo(() => {
    if (!isAuthenticated || organizations.length === 0) {
      return null
    }

    const selectedOrganizationExists = organizations.some(
      (organization) =>
        organization.id === selectedOrganizationId,
    )

    return selectedOrganizationExists
      ? selectedOrganizationId
      : organizations[0].id
  }, [
    isAuthenticated,
    organizations,
    selectedOrganizationId,
  ])

  const activeOrganization = useMemo(
    () =>
      organizations.find(
        (organization) =>
          organization.id === activeOrganizationId,
      ) ?? null,
    [organizations, activeOrganizationId],
  )

  function setActiveOrganizationId(organizationId: string) {
    const organizationExists = organizations.some(
      (organization) => organization.id === organizationId,
    )

    if (organizationExists) {
      setSelectedOrganizationId(organizationId)
    }
  }

  return (
    <OrganizationContext.Provider
      value={{
        organizations,
        activeOrganization,
        activeOrganizationId,
        setActiveOrganizationId,
        isLoading,
        error: error as Error | null,
      }}
    >
      {children}
    </OrganizationContext.Provider>
  )
}

export function useOrganizations() {
  const context = useContext(OrganizationContext)

  if (!context) {
    throw new Error(
      'useOrganizations must be used inside OrganizationProvider',
    )
  }

  return context
}
