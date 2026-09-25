import { useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import {
  useMutation,
  useQuery,
} from '@tanstack/react-query'
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Bot,
  CheckCircle2,
  Clock3,
  Gauge,
  Lightbulb,
  RefreshCw,
  Ticket,
  TrendingUp,
  Users,
} from 'lucide-react'
import {
  generateAnalyticsAiInsight,
  getAnalytics,
  getLatestAnalyticsAiInsight,
  type AnalyticsBreakdown,
  type AnalyticsResponse,
} from '../api/analytics'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  PageHeader,
  Select,
  Skeleton,
} from '../components/ui'

type DateRange = '7' | '30' | '90'

function AnalyticsPage() {
  const { activeOrganizationId, activeOrganization } =
    useOrganizations()

  const [dateRange, setDateRange] = useState<DateRange>('30')
  const [generatedInsight, setGeneratedInsight] = useState<{
    insight: string
    model: string
    createdAt?: string
  } | null>(null)

  const { from, to } = useMemo(() => {
    const end = new Date()
    const start = new Date()

    start.setDate(end.getDate() - Number(dateRange) + 1)

    return {
      from: start.toISOString().slice(0, 10),
      to: end.toISOString().slice(0, 10),
    }
  }, [dateRange])

  const analyticsQuery = useQuery({
    queryKey: [
      'analytics',
      activeOrganizationId,
      from,
      to,
    ],
    queryFn: () =>
      getAnalytics(
        activeOrganizationId as string,
        from,
        to,
      ),
    enabled: Boolean(activeOrganizationId),
  })

  const persistedInsightQuery = useQuery({
    queryKey: [
      'analytics-ai-insight',
      activeOrganizationId,
      from,
      to,
    ],
    queryFn: () =>
      getLatestAnalyticsAiInsight(
        activeOrganizationId as string,
        from,
        to,
      ),
    enabled: Boolean(activeOrganizationId),
  })

  const generateInsightMutation = useMutation({
    mutationFn: () =>
      generateAnalyticsAiInsight(
        activeOrganizationId as string,
        from,
        to,
      ),
    onSuccess: (data) => {
      setGeneratedInsight({
        insight: data.insight,
        model: data.model,
        createdAt: data.createdAt,
      })
    },
  })

  const insight =
    generatedInsight ??
    (persistedInsightQuery.data
      ? {
          insight: persistedInsightQuery.data.insight,
          model: persistedInsightQuery.data.model,
          createdAt: persistedInsightQuery.data.createdAt,
        }
      : null)

  function handleDateRangeChange(value: DateRange) {
    setDateRange(value)
    setGeneratedInsight(null)
    generateInsightMutation.reset()
  }

  if (!activeOrganizationId) {
    return (
      <EmptyState
        title="Select an organization"
        description="Choose an organization from the sidebar to view analytics."
        icon={<BarChart3 className="h-6 w-6" />}
      />
    )
  }

  if (analyticsQuery.isLoading) {
    return <AnalyticsSkeleton />
  }

  if (analyticsQuery.isError || !analyticsQuery.data) {
    return (
      <ErrorState
        title="Analytics could not be loaded"
        description="Check your connection and try again."
        action={
          <Button
            variant="secondary"
            size="sm"
            onClick={() => analyticsQuery.refetch()}
          >
            Try again
          </Button>
        }
      />
    )
  }

  const analytics = analyticsQuery.data
  const overview = analytics.overview

  return (
    <div>
      <PageHeader
        eyebrow={activeOrganization?.name}
        title="Analytics"
        description="Understand support volume, response performance, and SLA health."
        actions={
          <Select
            value={dateRange}
            onChange={(event) =>
              handleDateRangeChange(
                event.target.value as DateRange,
              )
            }
            aria-label="Analytics date range"
          >
            <option value="7">Last 7 days</option>
            <option value="30">Last 30 days</option>
            <option value="90">Last 90 days</option>
          </Select>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          label="Total tickets"
          value={overview.totalTickets}
          icon={<Ticket className="h-5 w-5" />}
          tone="info"
        />
        <MetricCard
          label="Open tickets"
          value={overview.openTickets}
          icon={<Activity className="h-5 w-5" />}
          tone="warning"
        />
        <MetricCard
          label="Resolved tickets"
          value={overview.resolvedTickets}
          icon={<CheckCircle2 className="h-5 w-5" />}
          tone="success"
        />
        <MetricCard
          label="Pending tickets"
          value={overview.pendingTickets}
          icon={<Clock3 className="h-5 w-5" />}
          tone="neutral"
        />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <Card className="p-5 sm:p-6">
          <SectionHeading
            icon={<TrendingUp className="h-5 w-5" />}
            title="Ticket volume"
            description={`Daily ticket activity over the last ${dateRange} days.`}
          />

          {analytics.ticketVolume.length === 0 ? (
            <div className="mt-6">
              <EmptyState
                title="No ticket activity"
                description="There is no ticket activity in this date range."
              />
            </div>
          ) : (
            <TicketVolumeChart
              items={analytics.ticketVolume}
            />
          )}
        </Card>

        <SlaHealthCard
          firstResponseRate={
            overview.firstResponseSlaBreachRate
          }
          resolutionRate={
            overview.resolutionSlaBreachRate
          }
        />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <ResponsePerformanceCard
          averageFirstResponseMinutes={
            overview.averageFirstResponseMinutes
          }
          averageResolutionMinutes={
            overview.averageResolutionMinutes
          }
        />

        <AiInsightCard
          insight={insight}
          isLoading={persistedInsightQuery.isLoading}
          isGenerating={generateInsightMutation.isPending}
          isError={generateInsightMutation.isError}
          onGenerate={() => generateInsightMutation.mutate()}
        />
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-3">
        <BreakdownCard
          title="Tickets by status"
          items={analytics.ticketsByStatus}
          tone="info"
        />
        <BreakdownCard
          title="Tickets by priority"
          items={analytics.ticketsByPriority}
          tone="warning"
        />
        <BreakdownCard
          title="Tickets by category"
          items={analytics.ticketsByCategory}
          tone="purple"
        />
      </div>

      <AgentWorkloadCard
        workload={analytics.agentWorkload}
      />
    </div>
  )
}

function MetricCard({
  label,
  value,
  icon,
  tone,
}: {
  label: string
  value: number
  icon: ReactNode
  tone: 'info' | 'warning' | 'success' | 'neutral'
}) {
  return (
    <Card className="p-4">
      <div className="flex items-center justify-between">
        <span className="text-sm text-[var(--app-text-muted)]">
          {label}
        </span>

        <span
          className={[
            'rounded-lg p-2',
            tone === 'success'
              ? 'bg-emerald-500/15 text-emerald-500'
              : tone === 'warning'
                ? 'bg-amber-500/15 text-amber-500'
                : tone === 'info'
                  ? 'bg-sky-500/15 text-sky-500'
                  : 'bg-slate-500/15 text-slate-500',
          ].join(' ')}
        >
          {icon}
        </span>
      </div>

      <p className="mt-3 text-2xl font-semibold text-[var(--app-text)]">
        {value.toLocaleString()}
      </p>
    </Card>
  )
}

function SectionHeading({
  icon,
  title,
  description,
}: {
  icon: ReactNode
  title: string
  description: string
}) {
  return (
    <div className="flex items-start gap-3">
      <div className="rounded-lg bg-indigo-500/15 p-2 text-indigo-500">
        {icon}
      </div>

      <div>
        <h2 className="font-semibold text-[var(--app-text)]">
          {title}
        </h2>
        <p className="mt-1 text-sm text-[var(--app-text-muted)]">
          {description}
        </p>
      </div>
    </div>
  )
}

function TicketVolumeChart({
  items,
}: {
  items: AnalyticsResponse['ticketVolume']
}) {
  const maxCount = Math.max(
    ...items.map((item) => item.count),
    1,
  )

  return (
    <div className="mt-8">
      <div className="flex h-56 items-end gap-1.5 sm:gap-2">
        {items.map((item) => {
          const height = Math.max(
            (item.count / maxCount) * 100,
            item.count > 0 ? 5 : 1,
          )

          return (
            <div
              key={item.date}
              className="group flex min-w-0 flex-1 flex-col items-center justify-end gap-2"
            >
              <span className="invisible text-[10px] text-[var(--app-text-muted)] group-hover:visible">
                {item.count}
              </span>

              <div
                className="w-full rounded-t-md bg-indigo-500/80 transition-colors group-hover:bg-indigo-400"
                style={{ height: `${height}%` }}
                title={`${item.date}: ${item.count} tickets`}
              />

              <span className="truncate text-[10px] text-[var(--app-text-subtle)]">
                {formatShortDate(item.date)}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

function SlaHealthCard({
  firstResponseRate,
  resolutionRate,
}: {
  firstResponseRate: number
  resolutionRate: number
}) {
  return (
    <Card className="p-5 sm:p-6">
      <SectionHeading
        icon={<Gauge className="h-5 w-5" />}
        title="SLA health"
        description="Percentage of tickets that breached each target."
      />

      <div className="mt-7 space-y-6">
        <ProgressMetric
          label="First response breach"
          value={firstResponseRate}
          tone="warning"
        />
        <ProgressMetric
          label="Resolution breach"
          value={resolutionRate}
          tone="danger"
        />
      </div>

      <div className="mt-6 flex items-start gap-2 rounded-lg bg-[var(--app-surface-muted)] p-3 text-xs text-[var(--app-text-muted)]">
        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
        Lower breach rates indicate healthier SLA performance.
      </div>
    </Card>
  )
}

function ProgressMetric({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone: 'warning' | 'danger'
}) {
  const color =
    tone === 'danger' ? 'bg-red-500' : 'bg-amber-500'

  return (
    <div>
      <div className="mb-2 flex justify-between gap-3 text-sm">
        <span className="text-[var(--app-text-muted)]">
          {label}
        </span>
        <span className="font-medium text-[var(--app-text)]">
          {formatPercentage(value)}
        </span>
      </div>

      <div className="h-2 overflow-hidden rounded-full bg-[var(--app-surface-muted)]">
        <div
          className={`h-full rounded-full ${color}`}
          style={{
            width: `${Math.min(Math.max(value, 0), 100)}%`,
          }}
        />
      </div>
    </div>
  )
}

function ResponsePerformanceCard({
  averageFirstResponseMinutes,
  averageResolutionMinutes,
}: {
  averageFirstResponseMinutes: number | null
  averageResolutionMinutes: number | null
}) {
  return (
    <Card className="p-5 sm:p-6">
      <SectionHeading
        icon={<Clock3 className="h-5 w-5" />}
        title="Response performance"
        description="Average time to respond and resolve tickets."
      />

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <div className="rounded-xl bg-[var(--app-surface-muted)] p-4">
          <p className="text-sm text-[var(--app-text-muted)]">
            First response
          </p>
          <p className="mt-2 text-2xl font-semibold text-[var(--app-text)]">
            {formatMinutes(averageFirstResponseMinutes)}
          </p>
        </div>

        <div className="rounded-xl bg-[var(--app-surface-muted)] p-4">
          <p className="text-sm text-[var(--app-text-muted)]">
            Resolution time
          </p>
          <p className="mt-2 text-2xl font-semibold text-[var(--app-text)]">
            {formatMinutes(averageResolutionMinutes)}
          </p>
        </div>
      </div>
    </Card>
  )
}

function AiInsightCard({
  insight,
  isLoading,
  isGenerating,
  isError,
  onGenerate,
}: {
  insight: {
    insight: string
    model: string
    createdAt?: string
  } | null
  isLoading: boolean
  isGenerating: boolean
  isError: boolean
  onGenerate: () => void
}) {
  return (
    <Card className="overflow-hidden">
      <div className="border-b border-[var(--app-border)] bg-indigo-500/5 p-5 sm:p-6">
        <div className="flex items-start justify-between gap-3">
          <SectionHeading
            icon={<Lightbulb className="h-5 w-5" />}
            title="AI insight"
            description="A generated summary of the selected period."
          />

          <Bot className="h-5 w-5 shrink-0 text-indigo-500" />
        </div>
      </div>

      <div className="p-5 sm:p-6">
        {isLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-5/6" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        ) : insight ? (
          <>
            <p className="whitespace-pre-wrap text-sm leading-6 text-[var(--app-text)]">
              {insight.insight}
            </p>

            <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
              <span className="text-xs text-[var(--app-text-subtle)]">
                {insight.model}
                {insight.createdAt
                  ? ` • ${formatDate(insight.createdAt)}`
                  : ''}
              </span>

              <Button
                variant="ghost"
                size="sm"
                onClick={onGenerate}
                disabled={isGenerating}
                icon={
                  <RefreshCw
                    className={[
                      'h-4 w-4',
                      isGenerating ? 'animate-spin' : '',
                    ].join(' ')}
                  />
                }
              >
                Regenerate
              </Button>
            </div>
          </>
        ) : (
          <div className="text-center">
            <p className="text-sm text-[var(--app-text-muted)]">
              Generate an AI summary to identify trends and risks.
            </p>

            <Button
              className="mt-4"
              size="sm"
              onClick={onGenerate}
              loading={isGenerating}
              icon={<SparklesIcon />}
            >
              Generate insight
            </Button>
          </div>
        )}

        {isError && (
          <p className="mt-4 text-sm text-red-500" role="alert">
            Failed to generate an AI insight. Please try again.
          </p>
        )}
      </div>
    </Card>
  )
}

function BreakdownCard({
  title,
  items,
  tone,
}: {
  title: string
  items: AnalyticsBreakdown[]
  tone: 'info' | 'warning' | 'purple'
}) {
  const total = items.reduce(
    (sum, item) => sum + item.count,
    0,
  )

  return (
    <Card className="p-5 sm:p-6">
      <h2 className="font-semibold text-[var(--app-text)]">
        {title}
      </h2>

      {items.length === 0 ? (
        <p className="mt-5 text-sm text-[var(--app-text-muted)]">
          No data available.
        </p>
      ) : (
        <div className="mt-6 space-y-4">
          {items.map((item) => {
            const percentage =
              total === 0 ? 0 : (item.count / total) * 100

            return (
              <div key={item.name}>
                <div className="mb-1.5 flex justify-between gap-3 text-sm">
                  <span className="truncate text-[var(--app-text-muted)]">
                    {formatLabel(item.name)}
                  </span>
                  <span className="font-medium text-[var(--app-text)]">
                    {item.count}
                  </span>
                </div>

                <div className="h-2 overflow-hidden rounded-full bg-[var(--app-surface-muted)]">
                  <div
                    className={[
                      'h-full rounded-full',
                      tone === 'warning'
                        ? 'bg-amber-500'
                        : tone === 'purple'
                          ? 'bg-purple-500'
                          : 'bg-sky-500',
                    ].join(' ')}
                    style={{ width: `${percentage}%` }}
                  />
                </div>
              </div>
            )
          })}
        </div>
      )}
    </Card>
  )
}

function AgentWorkloadCard({
  workload,
}: {
  workload: AnalyticsResponse['agentWorkload']
}) {
  return (
    <Card className="mt-6 overflow-hidden">
      <div className="border-b border-[var(--app-border)] p-5 sm:p-6">
        <SectionHeading
          icon={<Users className="h-5 w-5" />}
          title="Agent workload"
          description="Ticket distribution across support agents."
        />
      </div>

      {workload.length === 0 ? (
        <div className="p-5">
          <EmptyState
            title="No assigned tickets"
            description="Assigned ticket workload will appear here."
          />
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-left text-sm">
            <thead className="bg-[var(--app-surface-muted)] text-xs uppercase tracking-wide text-[var(--app-text-subtle)]">
              <tr>
                <th className="px-5 py-3 font-semibold">Agent</th>
                <th className="px-5 py-3 font-semibold">Open</th>
                <th className="px-5 py-3 font-semibold">Resolved</th>
                <th className="px-5 py-3 font-semibold">Total</th>
              </tr>
            </thead>

            <tbody className="divide-y divide-[var(--app-border)]">
              {workload.map((agent) => (
                <tr key={agent.agentId}>
                  <td className="px-5 py-4 font-mono text-xs text-[var(--app-text-muted)]">
                    {agent.agentId}
                  </td>
                  <td className="px-5 py-4 text-[var(--app-text)]">
                    {agent.openTickets}
                  </td>
                  <td className="px-5 py-4 text-[var(--app-text)]">
                    {agent.resolvedTickets}
                  </td>
                  <td className="px-5 py-4 font-semibold text-[var(--app-text)]">
                    {agent.totalTickets}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  )
}

function AnalyticsSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-end justify-between">
        <div className="space-y-3">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-8 w-44" />
          <Skeleton className="h-4 w-72" />
        </div>
        <Skeleton className="h-10 w-36" />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <Card key={index} className="p-5">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="mt-4 h-8 w-16" />
          </Card>
        ))}
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <Card className="p-6">
          <Skeleton className="h-6 w-40" />
          <Skeleton className="mt-8 h-56 w-full" />
        </Card>
        <Card className="p-6">
          <Skeleton className="h-6 w-32" />
          <Skeleton className="mt-8 h-24 w-full" />
          <Skeleton className="mt-6 h-24 w-full" />
        </Card>
      </div>
    </div>
  )
}

function SparklesIcon() {
  return <Lightbulb className="h-4 w-4" />
}

function formatMinutes(minutes: number | null) {
  if (minutes === null) {
    return '—'
  }

  if (minutes < 60) {
    return `${Math.round(minutes)}m`
  }

  const hours = minutes / 60

  if (hours < 24) {
    return `${hours.toFixed(1)}h`
  }

  return `${(hours / 24).toFixed(1)}d`
}

function formatPercentage(value: number) {
  return `${value.toFixed(1)}%`
}

function formatLabel(value: string) {
  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (character) =>
      character.toUpperCase(),
    )
}

function formatShortDate(value: string) {
  const date = new Date(`${value}T00:00:00`)
  return date.toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  })
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

export default AnalyticsPage
