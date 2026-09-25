import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import {
  generateAnalyticsAiInsight,
  getAnalytics,
  getLatestAnalyticsAiInsight,
} from '../api/analytics'
import { useOrganizations } from '../features/organizations/OrganizationContext'

type DateRange = '7' | '30' | '90'

function formatMinutes(minutes: number | null): string {
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

function formatPercentage(value: number): string {
  return `${value.toFixed(1)}%`
}

function AnalyticsPage() {
  const { activeOrganizationId, activeOrganization } =
    useOrganizations()

  const [dateRange, setDateRange] = useState<DateRange>('30')
  const [generatedInsight, setGeneratedInsight] = useState<{
    insight: string
    model: string
  } | null>(null)

  const { from, to } = useMemo(() => {
    const end = new Date()
    const start = new Date()

    start.setDate(end.getDate() - Number(dateRange) + 1)

    const formatDate = (date: Date) =>
      date.toISOString().slice(0, 10)

    return {
      from: formatDate(start),
      to: formatDate(end),
    }
  }, [dateRange])

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
      })
    },
  })

  const {
    data: persistedAiInsight,
  } = useQuery({
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

  const aiInsight = generatedInsight ?? (
    persistedAiInsight
      ? {
          insight: persistedAiInsight.insight,
          model: persistedAiInsight.model,
        }
      : null
  )

  const {
    data: analytics,
    isLoading,
    isError,
  } = useQuery({
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

  if (!activeOrganizationId) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold">
          Analytics
        </h1>
        <p className="mt-2 text-slate-400">
          Select an organization to view analytics.
        </p>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold">
          Analytics
        </h1>
        <p className="mt-4 text-slate-400">
          Loading analytics...
        </p>
      </div>
    )
  }

  if (isError || !analytics) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold">
          Analytics
        </h1>
        <div className="mt-6 rounded-xl border border-red-900 bg-red-950/40 p-4 text-red-300">
          Failed to load analytics.
        </div>
      </div>
    )
  }

  const { overview } = analytics

  const maxVolume = Math.max(
    ...analytics.ticketVolume.map((item) => item.count),
    1,
  )

  return (
    <div className="p-6">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
        <div>
          <h1 className="text-2xl font-semibold">
            Analytics
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            {activeOrganization?.name ?? 'Organization'} performance
            overview
          </p>
        </div>

        <select
          value={dateRange}
          onChange={(event) => {
            setDateRange(event.target.value as DateRange)
            setGeneratedInsight(null)
            generateInsightMutation.reset()
          }}
          className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-white outline-none"
        >
          <option value="7">Last 7 days</option>
          <option value="30">Last 30 days</option>
          <option value="90">Last 90 days</option>
        </select>
      </div>

      <section className="mt-6 rounded-xl border border-slate-800 bg-slate-900 p-5">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
          <div>
            <h2 className="text-lg font-semibold">
              AI insight
            </h2>
            <p className="mt-1 text-sm text-slate-400">
              Generate an AI-powered summary of the selected analytics period.
            </p>
          </div>

          <button
            type="button"
            onClick={() => generateInsightMutation.mutate()}
            disabled={generateInsightMutation.isPending}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {generateInsightMutation.isPending
              ? 'Generating...'
              : 'Generate AI Insight'}
          </button>
        </div>

        {generateInsightMutation.isError && (
          <div className="mt-4 rounded-lg border border-red-900 bg-red-950/40 p-4 text-sm text-red-300">
            Failed to generate AI insight.
          </div>
        )}

        {aiInsight && (
          <div className="mt-5 rounded-lg border border-slate-800 bg-slate-950/50 p-4">
            <p className="text-sm leading-6 text-slate-200">
              {aiInsight.insight}
            </p>
            <p className="mt-3 text-xs text-slate-500">
              Generated by {aiInsight.model}
            </p>
          </div>
        )}
      </section>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          label="Total tickets"
          value={overview.totalTickets}
        />
        <MetricCard
          label="Open tickets"
          value={overview.openTickets}
        />
        <MetricCard
          label="Pending tickets"
          value={overview.pendingTickets}
        />
        <MetricCard
          label="Resolved tickets"
          value={overview.resolvedTickets}
        />
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border border-slate-800 bg-slate-900 p-5">
          <h2 className="text-lg font-semibold">
            Response & resolution
          </h2>

          <div className="mt-5 grid grid-cols-2 gap-4">
            <MetricCard
              label="Avg. first response"
              value={formatMinutes(
                overview.averageFirstResponseMinutes,
              )}
            />
            <MetricCard
              label="Avg. resolution"
              value={formatMinutes(
                overview.averageResolutionMinutes,
              )}
            />
            <MetricCard
              label="First response SLA breach"
              value={formatPercentage(
                overview.firstResponseSlaBreachRate,
              )}
            />
            <MetricCard
              label="Resolution SLA breach"
              value={formatPercentage(
                overview.resolutionSlaBreachRate,
              )}
            />
          </div>
        </section>

        <section className="rounded-xl border border-slate-800 bg-slate-900 p-5">
          <h2 className="text-lg font-semibold">
            Ticket volume
          </h2>

          {analytics.ticketVolume.length === 0 ? (
            <p className="mt-5 text-sm text-slate-400">
              No ticket activity in this period.
            </p>
          ) : (
            <div className="mt-5 space-y-3">
              {analytics.ticketVolume.map((item) => (
                <div
                  key={item.date}
                  className="grid grid-cols-[80px_1fr_40px] items-center gap-3 text-sm"
                >
                  <span className="text-slate-400">
                    {item.date.slice(5)}
                  </span>

                  <div className="h-3 overflow-hidden rounded-full bg-slate-800">
                    <div
                      className="h-full rounded-full bg-indigo-500"
                      style={{
                        width: `${(item.count / maxVolume) * 100}%`,
                      }}
                    />
                  </div>

                  <span className="text-right text-slate-300">
                    {item.count}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-3">
        <BreakdownCard
          title="Tickets by status"
          items={analytics.ticketsByStatus}
        />

        <BreakdownCard
          title="Tickets by priority"
          items={analytics.ticketsByPriority}
        />

        <BreakdownCard
          title="Tickets by category"
          items={analytics.ticketsByCategory}
        />
      </div>

      <section className="mt-6 rounded-xl border border-slate-800 bg-slate-900 p-5">
        <h2 className="text-lg font-semibold">
          Agent workload
        </h2>

        {analytics.agentWorkload.length === 0 ? (
          <p className="mt-5 text-sm text-slate-400">
            No assigned tickets in this period.
          </p>
        ) : (
          <div className="mt-5 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400">
                  <th className="pb-3 font-medium">
                    Agent
                  </th>
                  <th className="pb-3 font-medium">
                    Open
                  </th>
                  <th className="pb-3 font-medium">
                    Resolved
                  </th>
                  <th className="pb-3 font-medium">
                    Total
                  </th>
                </tr>
              </thead>

              <tbody>
                {analytics.agentWorkload.map((agent) => (
                  <tr
                    key={agent.agentId}
                    className="border-b border-slate-800/60"
                  >
                    <td className="py-3 font-mono text-xs text-slate-300">
                      {agent.agentId}
                    </td>
                    <td className="py-3 text-slate-300">
                      {agent.openTickets}
                    </td>
                    <td className="py-3 text-slate-300">
                      {agent.resolvedTickets}
                    </td>
                    <td className="py-3 font-medium text-white">
                      {agent.totalTickets}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}

function MetricCard({
  label,
  value,
}: {
  label: string
  value: number | string
}) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900 p-5">
      <p className="text-sm text-slate-400">
        {label}
      </p>
      <p className="mt-2 text-2xl font-semibold text-white">
        {value}
      </p>
    </div>
  )
}

function BreakdownCard({
  title,
  items,
}: {
  title: string
  items: {
    name: string
    count: number
  }[]
}) {
  const total = items.reduce(
    (sum, item) => sum + item.count,
    0,
  )

  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900 p-5">
      <h2 className="text-lg font-semibold">
        {title}
      </h2>

      {items.length === 0 ? (
        <p className="mt-5 text-sm text-slate-400">
          No data available.
        </p>
      ) : (
        <div className="mt-5 space-y-4">
          {items.map((item) => {
            const percentage =
              total === 0
                ? 0
                : (item.count / total) * 100

            return (
              <div key={item.name}>
                <div className="mb-1 flex justify-between text-sm">
                  <span className="text-slate-300">
                    {item.name}
                  </span>
                  <span className="text-slate-400">
                    {item.count}
                  </span>
                </div>

                <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                  <div
                    className="h-full rounded-full bg-slate-400"
                    style={{
                      width: `${percentage}%`,
                    }}
                  />
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}

export default AnalyticsPage
