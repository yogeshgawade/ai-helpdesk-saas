import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Activity,
  AlertTriangle,
  Ticket as TicketIcon,
  UserCheck,
} from 'lucide-react'
import { getTickets, type TicketListResponse } from '../api/tickets'
import { useAuth } from '../features/auth/AuthContext'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import type { Ticket } from '../features/tickets/types'
import {
  Card,
  EmptyState,
  ErrorState,
  PageHeader,
  Skeleton,
} from '../components/ui'

function DashboardPage() {
  const { user } = useAuth()
  const { activeOrganizationId, isLoading: orgsLoading } = useOrganizations()
  const navigate = useNavigate()

  const ticketsQuery = useQuery<TicketListResponse, Error>({
    queryKey: ['tickets-dashboard', activeOrganizationId],
    queryFn: () =>
      getTickets(activeOrganizationId!, {
        limit: 100,
        sort: 'newest',
      }),
    enabled: !!activeOrganizationId,
    staleTime: 20_000,
  })

  const isLoading = orgsLoading || ticketsQuery.isLoading
  const error = ticketsQuery.error

  const metrics = useMemo(() => {
    const tickets = ticketsQuery.data?.tickets ?? []

    const openTickets = tickets.filter((t) => t.status === 'OPEN')
    const unassignedTickets = openTickets.filter(
      (t) => !t.assignedAgentId,
    )
    const slaRiskTickets = tickets.filter(
      (t) =>
        t.status !== 'RESOLVED' &&
        t.status !== 'CLOSED' &&
        (t.slaFirstResponseBreached ||
          t.slaResolutionBreached ||
          isDueSoon(t)),
    )
    const assignedToMeTickets =
      user?.id
        ? tickets.filter(
            (t) =>
              t.assignedAgentId === user.id &&
              t.status !== 'RESOLVED' &&
              t.status !== 'CLOSED',
          )
        : []
    const recentTickets = tickets.slice(0, 10)

    return {
      openCount: openTickets.length,
      unassignedCount: unassignedTickets.length,
      slaRiskCount: slaRiskTickets.length,
      assignedToMeCount: assignedToMeTickets.length,
      recentTickets,
    }
  }, [ticketsQuery.data, user?.id])

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Dashboard"
          description="Loading your support workspace overview..."
        />
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i} className="p-5">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="mt-3 h-8 w-12" />
            </Card>
          ))}
        </div>
        <Card className="p-5">
          <Skeleton className="h-5 w-40" />
          <div className="mt-4 space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-4 w-full" />
            ))}
          </div>
        </Card>
      </div>
    )
  }

  if (error) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Dashboard"
          description="Overview of your support workspace"
        />
        <ErrorState
          title="Failed to load tickets"
          description="Please try again later or refresh the page."
        />
      </div>
    )
  }

  const hasTickets = metrics.recentTickets.length > 0

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Overview of your support workspace"
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Open tickets"
          value={metrics.openCount}
          icon={<TicketIcon className="h-5 w-5" />}
          tone="warning"
          onClick={() =>
            navigate('/app/tickets', {
              state: { filterStatus: 'OPEN' },
            })
          }
        />
        <MetricCard
          label="Unassigned"
          value={metrics.unassignedCount}
          icon={<UserCheck className="h-5 w-5" />}
          tone="info"
          onClick={() =>
            navigate('/app/tickets', {
              state: { filterUnassigned: true },
            })
          }
        />
        <MetricCard
          label="SLA risk / breached"
          value={metrics.slaRiskCount}
          icon={<AlertTriangle className="h-5 w-5" />}
          tone="danger"
          onClick={() =>
            navigate('/app/tickets', {
              state: { filterSlaRisk: true },
            })
          }
        />
        <MetricCard
          label="Assigned to me"
          value={metrics.assignedToMeCount}
          icon={<Activity className="h-5 w-5" />}
          tone="success"
          onClick={() =>
            navigate('/app/tickets', {
              state: { filterAssignedToMe: true },
            })
          }
        />
      </div>

      <Card className="p-5 sm:p-6">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-base font-semibold text-[var(--app-text)]">
              Recent tickets
            </h2>
            <p className="text-sm text-[var(--app-text-muted)]">
              Latest tickets created in your organization
            </p>
          </div>
        </div>

        {!hasTickets ? (
          <EmptyState
            title="No tickets yet"
            description="Tickets created in your organization will appear here."
          />
        ) : (
          <div className="divide-y divide-[var(--app-border)]">
            {metrics.recentTickets.map((ticket) => (
              <button
                key={ticket.id}
                type="button"
                onClick={() =>
                  navigate(`/app/tickets/${ticket.id}`)
                }
                className="group flex w-full items-center justify-between gap-4 py-3 text-left hover:bg-[var(--app-surface-muted)]"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-[var(--app-text)]">
                    {ticket.subject}
                  </p>
                  <p className="mt-0.5 text-xs text-[var(--app-text-muted)]">
                    #{ticket.id.slice(0, 8)} •{' '}
                    {new Date(ticket.createdAt).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex shrink-0 items-center gap-2">
                  <StatusBadge status={ticket.status} />
                  <PriorityBadge priority={ticket.priority} />
                </div>
              </button>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

function MetricCard({
  label,
  value,
  icon,
  tone,
  onClick,
}: {
  label: string
  value: number
  icon: React.ReactNode
  tone: 'info' | 'warning' | 'danger' | 'success'
  onClick?: () => void
}) {
  const toneClasses = {
    info: 'bg-blue-500/10 text-blue-400',
    warning: 'bg-amber-500/10 text-amber-400',
    danger: 'bg-red-500/10 text-red-400',
    success: 'bg-emerald-500/10 text-emerald-400',
  }

  return (
    <Card
      className={`group cursor-pointer p-5 transition-colors hover:bg-[var(--app-surface-muted)] ${
        onClick ? '' : 'pointer-events-none'
      }`}
      onClick={onClick}
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-[var(--app-text-muted)]">
            {label}
          </p>
          <p className="mt-2 text-2xl font-bold text-[var(--app-text)]">
            {value}
          </p>
        </div>
        <div
          className={`flex h-10 w-10 items-center justify-center rounded-lg ${toneClasses[tone]}`}
        >
          {icon}
        </div>
      </div>
    </Card>
  )
}

function StatusBadge({
  status,
}: {
  status: Ticket['status']
}) {
  const styles: Record<Ticket['status'], string> = {
    OPEN: 'bg-blue-500/10 text-blue-400',
    IN_PROGRESS: 'bg-amber-500/10 text-amber-400',
    PENDING: 'bg-purple-500/10 text-purple-400',
    RESOLVED: 'bg-emerald-500/10 text-emerald-400',
    CLOSED: 'bg-slate-500/10 text-slate-400',
  }

  return (
    <span
      className={`rounded px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${styles[status]}`}
    >
      {status.replace(/_/g, ' ')}
    </span>
  )
}

function PriorityBadge({
  priority,
}: {
  priority: Ticket['priority']
}) {
  const styles: Record<Ticket['priority'], string> = {
    LOW: 'bg-slate-500/10 text-slate-400',
    MEDIUM: 'bg-blue-500/10 text-blue-400',
    HIGH: 'bg-amber-500/10 text-amber-400',
    URGENT: 'bg-red-500/10 text-red-400',
  }

  return (
    <span
      className={`rounded px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${styles[priority]}`}
    >
      {priority}
    </span>
  )
}

function isDueSoon(ticket: Ticket): boolean {
  const now = Date.now()
  const thresholdMs = 30 * 60 * 1000 // 30 minutes

  const firstDue =
    ticket.firstResponseDueAt != null
      ? new Date(ticket.firstResponseDueAt).getTime()
      : null
  const resolutionDue =
    ticket.resolutionDueAt != null
      ? new Date(ticket.resolutionDueAt).getTime()
      : null

  if (firstDue != null && firstDue - now < thresholdMs) {
    return true
  }

  if (resolutionDue != null && resolutionDue - now < thresholdMs) {
    return true
  }

  return false
}

export default DashboardPage
