import { apiClient } from './client'

export interface AnalyticsOverview {
  totalTickets: number
  openTickets: number
  pendingTickets: number
  resolvedTickets: number
  averageFirstResponseMinutes: number | null
  averageResolutionMinutes: number | null
  firstResponseSlaBreachRate: number
  resolutionSlaBreachRate: number
}

export interface AnalyticsBreakdown {
  name: string
  count: number
}

export interface AnalyticsTimeSeries {
  date: string
  count: number
}

export interface AnalyticsAgentWorkload {
  agentId: string
  openTickets: number
  resolvedTickets: number
  totalTickets: number
}

export interface AnalyticsResponse {
  overview: AnalyticsOverview
  ticketsByPriority: AnalyticsBreakdown[]
  ticketsByStatus: AnalyticsBreakdown[]
  ticketsByCategory: AnalyticsBreakdown[]
  ticketVolume: AnalyticsTimeSeries[]
  agentWorkload: AnalyticsAgentWorkload[]
}

export interface AnalyticsAiInsight {
  insightId: string
  insight: string
  model: string
  createdAt: string
}

export async function getLatestAnalyticsAiInsight(
  organizationId: string,
  from?: string,
  to?: string,
): Promise<AnalyticsAiInsight | null> {
  const response = await apiClient.get<AnalyticsAiInsight | null>(
    `/api/orgs/${organizationId}/analytics/ai-insight`,
    {
      params: {
        ...(from ? { from } : {}),
        ...(to ? { to } : {}),
      },
    },
  )

  return response.data
}

export async function generateAnalyticsAiInsight(
  organizationId: string,
  from?: string,
  to?: string,
): Promise<AnalyticsAiInsight> {
  const response = await apiClient.post<AnalyticsAiInsight>(
    `/api/orgs/${organizationId}/analytics/ai-insight`,
    null,
    {
      params: {
        ...(from ? { from } : {}),
        ...(to ? { to } : {}),
      },
    },
  )

  return response.data
}

export async function getAnalytics(
  organizationId: string,
  from?: string,
  to?: string,
): Promise<AnalyticsResponse> {
  const response = await apiClient.get<AnalyticsResponse>(
    `/api/orgs/${organizationId}/analytics`,
    {
      params: {
        ...(from ? { from } : {}),
        ...(to ? { to } : {}),
      },
    },
  )

  return response.data
}
