import { apiClient } from './client'
import type { Ticket, TicketMessage } from '../features/tickets/types'

export interface TicketListResponse {
  tickets: Ticket[]
  nextCursor: string | null
  hasMore: boolean
}

export interface TicketListParams {
  search?: string
  status?: Ticket['status']
  priority?: Ticket['priority']
  category?: string
  sort?: 'newest' | 'oldest'
  cursor?: string
  limit?: number
}

export interface CreateTicketRequest {
  subject: string
  priority?: Ticket['priority']
  category?: string
  customerId?: string
}

export async function createTicket(
  organizationId: string,
  request: CreateTicketRequest,
): Promise<Ticket> {
  const response = await apiClient.post<Ticket>(
    `/api/orgs/${organizationId}/tickets`,
    request,
  )

  return response.data
}

export async function getTickets(
  organizationId: string,
  params?: TicketListParams,
): Promise<TicketListResponse> {
  const response = await apiClient.get<TicketListResponse>(
    `/api/orgs/${organizationId}/tickets`,
    {
      params,
    },
  )

  return response.data
}
export async function getTicket(
  organizationId: string,
  ticketId: string,
): Promise<Ticket> {
  const response = await apiClient.get<Ticket>(
    `/api/orgs/${organizationId}/tickets/${ticketId}`,
  )

  return response.data
}

export async function getTicketMessages(
  organizationId: string,
  ticketId: string,
): Promise<TicketMessage[]> {
  const response = await apiClient.get<TicketMessage[]>(
    `/api/orgs/${organizationId}/tickets/${ticketId}/messages`,
  )

  return response.data
}

export interface CreateTicketMessageRequest {
  body: string
  internalNote: boolean
}

export async function createTicketMessage(
  organizationId: string,
  ticketId: string,
  request: CreateTicketMessageRequest,
): Promise<TicketMessage> {
  const response = await apiClient.post<TicketMessage>(
    `/api/orgs/${organizationId}/tickets/${ticketId}/messages`,
    request,
  )

  return response.data
}

export interface UpdateTicketRequest {
  status?: Ticket['status']
  priority?: Ticket['priority']
  category?: string | null
  assignedAgentId?: string | null
}

export async function updateTicket(
  organizationId: string,
  ticketId: string,
  request: UpdateTicketRequest,
): Promise<Ticket> {
  const response = await apiClient.patch<Ticket>(
    `/api/orgs/${organizationId}/tickets/${ticketId}`,
    request,
  )

  return response.data
}

