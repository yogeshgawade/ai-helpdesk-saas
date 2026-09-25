import { apiClient } from './client'
import type {
  OrganizationMember,
  OrganizationRole,
} from '../features/organizations/types'

export interface AddMemberRequest {
  userId: string
  role: OrganizationRole
}

export async function addOrganizationMember(
  organizationId: string,
  request: AddMemberRequest,
): Promise<OrganizationMember> {
  const response = await apiClient.post<OrganizationMember>(
    `/api/orgs/${organizationId}/members`,
    request,
  )

  return response.data
}

export async function searchAvailableOrganizationUsers(
  organizationId: string,
  query: string,
): Promise<Array<{ userId: string; name: string; email: string }>> {
  const response = await apiClient.get<
    Array<{ userId: string; name: string; email: string }>
  >(`/api/orgs/${organizationId}/members/search`, {
    params: { query },
  })
  return response.data
}

export async function updateOrganizationMemberRole(
  organizationId: string,
  membershipId: string,
  role: OrganizationRole,
): Promise<OrganizationMember> {
  const response = await apiClient.patch<OrganizationMember>(
    `/api/orgs/${organizationId}/members/${membershipId}/role`,
    { role },
  )

  return response.data
}
