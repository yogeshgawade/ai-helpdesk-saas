import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { useQuery } from '@tanstack/react-query'
import { getMyOrganizations } from '../../api/organizations'
import type { Organization } from './types'

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

const ACTIVE_ORGANIZATION_KEY = 'active_organization_id'

export function OrganizationProvider({
  children,
}: {
  children: ReactNode
}) {
  const [activeOrganizationId, setActiveOrganizationId] =
    useState<string | null>(() =>
      localStorage.getItem(ACTIVE_ORGANIZATION_KEY),
    )

  const {
    data: organizations = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['my-organizations'],
    queryFn: getMyOrganizations,
  })

  useEffect(() => {
    if (organizations.length === 0) {
      setActiveOrganizationId(null)
      return
    }

    const storedOrganizationExists = organizations.some(
      (organization) => organization.id === activeOrganizationId,
    )

    if (!storedOrganizationExists) {
      setActiveOrganizationId(organizations[0].id)
    }
  }, [organizations, activeOrganizationId])

  useEffect(() => {
    if (activeOrganizationId) {
      localStorage.setItem(
        ACTIVE_ORGANIZATION_KEY,
        activeOrganizationId,
      )
    } else {
      localStorage.removeItem(ACTIVE_ORGANIZATION_KEY)
    }
  }, [activeOrganizationId])

  const activeOrganization =
    organizations.find(
      (organization) => organization.id === activeOrganizationId,
    ) ?? null

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
