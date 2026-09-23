import { apiClient } from './client'

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface SlaPolicy {
  id: string
  organizationId: string
  name: string
  firstResponseMinutes: number
  resolutionMinutes: number
  priority: TicketPriority
}

export interface SlaPolicyRequest {
  name: string
  firstResponseMinutes: number
  resolutionMinutes: number
  priority: TicketPriority
}

export async function getSlaPolicies(
  organizationId: string,
): Promise<SlaPolicy[]> {
  const response = await apiClient.get<SlaPolicy[]>(
    `/api/orgs/${organizationId}/sla-policies`,
  )

  return response.data
}

export async function createSlaPolicy(
  organizationId: string,
  request: SlaPolicyRequest,
): Promise<SlaPolicy> {
  const response = await apiClient.post<SlaPolicy>(
    `/api/orgs/${organizationId}/sla-policies`,
    request,
  )

  return response.data
}

export async function updateSlaPolicy(
  organizationId: string,
  policyId: string,
  request: SlaPolicyRequest,
): Promise<SlaPolicy> {
  const response = await apiClient.put<SlaPolicy>(
    `/api/orgs/${organizationId}/sla-policies/${policyId}`,
    request,
  )

  return response.data
}

export async function deleteSlaPolicy(
  organizationId: string,
  policyId: string,
): Promise<void> {
  await apiClient.delete(
    `/api/orgs/${organizationId}/sla-policies/${policyId}`,
  )
}
