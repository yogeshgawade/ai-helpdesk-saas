import { apiClient } from './client'
import type { Organization, OrganizationMember } from '../features/organizations/types'

export async function getMyOrganizations(): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>(
    '/api/me/organizations',
  )

  return response.data
}

export async function getOrganizationMembers(
  organizationId: string,
): Promise<OrganizationMember[]> {
  const response = await apiClient.get<OrganizationMember[]>(
    `/api/orgs/${organizationId}/members`,
  )

  return response.data
}
