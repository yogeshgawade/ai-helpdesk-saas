export type TicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'PENDING'
  | 'RESOLVED'
  | 'CLOSED'

export type TicketPriority =
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH'
  | 'URGENT'

export interface Ticket {
  id: string
  organizationId: string
  customerId: string
  assignedAgentId: string | null
  subject: string
  status: TicketStatus
  priority: TicketPriority
  category: string | null
  slaPolicyId: string | null
  aiSummary: string | null
  aiSummarizedAt: string | null
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
  firstResponseDueAt: string | null
  resolutionDueAt: string | null
  slaFirstResponseBreached: boolean
  slaResolutionBreached: boolean
}

export interface TicketMessage {
  id: string
  ticketId: string
  authorId: string
  body: string
  internalNote: boolean
  aiGenerated: boolean
  createdAt: string
}
