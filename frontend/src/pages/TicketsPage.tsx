import { useEffect, useMemo, useState } from 'react'
import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query'
import {
  AlertTriangle,
  CheckCircle2,
  CircleDot,
  Clock3,
  Filter,
  Inbox,
  Plus,
  RefreshCw,
  Search,
  Ticket as TicketIcon,
  UserRound,
  X,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import {
  createTicket,
  getTickets,
} from '../api/tickets'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import type {
  Ticket,
  TicketPriority,
  TicketStatus,
} from '../features/tickets/types'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Input,
  PageHeader,
  Select,
  Skeleton,
  Spinner,
} from '../components/ui'

type StatusFilter = 'ALL' | TicketStatus
type PriorityFilter = 'ALL' | TicketPriority
type SortOrder = 'newest' | 'oldest'

const PAGE_SIZE = 20

const statusLabels: Record<TicketStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  PENDING: 'Pending',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
}

const priorityLabels: Record<TicketPriority, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  URGENT: 'Urgent',
}

function TicketsPage() {
  const {
    activeOrganizationId,
    activeOrganization,
  } = useOrganizations()

  const queryClient = useQueryClient()

  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<StatusFilter>('ALL')
  const [priority, setPriority] = useState<PriorityFilter>('ALL')
  const [sort, setSort] = useState<SortOrder>('newest')
  const [showFilters, setShowFilters] = useState(false)
  const [isCreateOpen, setIsCreateOpen] = useState(false)

  const isStaffRole =
    activeOrganization?.role === 'OWNER' ||
    activeOrganization?.role === 'ADMIN' ||
    activeOrganization?.role === 'AGENT'

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setSearch(searchInput.trim())
    }, 300)

    return () => window.clearTimeout(timeout)
  }, [searchInput])

  const filters = useMemo(
    () => ({
      search: search || undefined,
      status: status === 'ALL' ? undefined : status,
      priority: priority === 'ALL' ? undefined : priority,
      sort,
      limit: PAGE_SIZE,
    }),
    [priority, search, sort, status],
  )

  const ticketsQuery = useInfiniteQuery({
    queryKey: ['tickets', activeOrganizationId, filters],
    queryFn: ({ pageParam }) =>
      getTickets(activeOrganizationId!, {
        ...filters,
        cursor: pageParam ?? undefined,
      }),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor : undefined,
    enabled: Boolean(activeOrganizationId),
  })

  const tickets = ticketsQuery.data?.pages.flatMap(
    (page) => page.tickets,
  ) ?? []

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setStatus('ALL')
    setPriority('ALL')
    setSort('newest')
  }

  const hasActiveFilters =
    search.length > 0 ||
    status !== 'ALL' ||
    priority !== 'ALL' ||
    sort !== 'newest'

  if (!activeOrganizationId) {
    return (
      <EmptyState
        title="Select an organization"
        description="Choose an organization from the sidebar to view its tickets."
        icon={<TicketIcon className="h-6 w-6" />}
      />
    )
  }

  return (
    <div>
      <PageHeader
        eyebrow={activeOrganization?.name}
        title="Tickets"
        description="Track customer conversations, assignments, and SLA activity."
        actions={
          isStaffRole ? (
            <Button
              onClick={() => setIsCreateOpen(true)}
              icon={<Plus className="h-4 w-4" />}
            >
              New ticket
            </Button>
          ) : undefined
        }
      />

      <Card className="overflow-hidden">
        <div className="border-b border-[var(--app-border)] p-4 sm:p-5">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
            <div className="relative min-w-0 flex-1">
              <Search
                className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--app-text-subtle)]"
                aria-hidden="true"
              />

              <input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Search tickets by subject..."
                className="h-10 w-full rounded-lg border border-[var(--app-border)] bg-[var(--app-surface-muted)] pl-9 pr-9 text-sm text-[var(--app-text)] outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-400/20"
                aria-label="Search tickets"
              />

              {searchInput && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchInput('')
                    setSearch('')
                  }}
                  className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-[var(--app-text-muted)] hover:bg-[var(--app-surface)] hover:text-[var(--app-text)]"
                  aria-label="Clear search"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>

            <div className="flex items-center gap-2">
              <Button
                variant={showFilters ? 'primary' : 'secondary'}
                size="sm"
                onClick={() => setShowFilters((value) => !value)}
                icon={<Filter className="h-4 w-4" />}
              >
                Filters
                {hasActiveFilters && (
                  <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-white/20 px-1 text-[10px]">
                    {[
                      status !== 'ALL',
                      priority !== 'ALL',
                      sort !== 'newest',
                      search.length > 0,
                    ].filter(Boolean).length}
                  </span>
                )}
              </Button>

              <Button
                variant="ghost"
                size="sm"
                onClick={() => ticketsQuery.refetch()}
                disabled={ticketsQuery.isFetching}
                aria-label="Refresh tickets"
                icon={
                  <RefreshCw
                    className={[
                      'h-4 w-4',
                      ticketsQuery.isFetching ? 'animate-spin' : '',
                    ].join(' ')}
                  />
                }
              >
                <span className="hidden sm:inline">Refresh</span>
              </Button>
            </div>
          </div>

          {showFilters && (
            <div className="mt-4 grid gap-3 border-t border-[var(--app-border)] pt-4 sm:grid-cols-3">
              <Select
                label="Status"
                value={status}
                onChange={(event) =>
                  setStatus(event.target.value as StatusFilter)
                }
              >
                <option value="ALL">All statuses</option>
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="PENDING">Pending</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </Select>

              <Select
                label="Priority"
                value={priority}
                onChange={(event) =>
                  setPriority(event.target.value as PriorityFilter)
                }
              >
                <option value="ALL">All priorities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </Select>

              <Select
                label="Sort"
                value={sort}
                onChange={(event) =>
                  setSort(event.target.value as SortOrder)
                }
              >
                <option value="newest">Newest first</option>
                <option value="oldest">Oldest first</option>
              </Select>

              {hasActiveFilters && (
                <div className="sm:col-span-3">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={clearFilters}
                    icon={<X className="h-4 w-4" />}
                  >
                    Clear filters
                  </Button>
                </div>
              )}
            </div>
          )}
        </div>

        {ticketsQuery.isLoading && <TicketListSkeleton />}

        {ticketsQuery.isError && (
          <div className="p-5">
            <ErrorState
              title="Tickets could not be loaded"
              description="Check your connection and try again."
              action={
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => ticketsQuery.refetch()}
                >
                  Try again
                </Button>
              }
            />
          </div>
        )}

        {!ticketsQuery.isLoading &&
          !ticketsQuery.isError &&
          tickets.length === 0 && (
            <div className="p-5">
              <EmptyState
                title={
                  hasActiveFilters
                    ? 'No matching tickets'
                    : 'No tickets yet'
                }
                description={
                  hasActiveFilters
                    ? 'Try changing or clearing your filters.'
                    : 'Create a ticket to start managing customer conversations.'
                }
                icon={<Inbox className="h-6 w-6" />}
                action={
                  hasActiveFilters ? (
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={clearFilters}
                    >
                      Clear filters
                    </Button>
                  ) : isStaffRole ? (
                    <Button
                      size="sm"
                      onClick={() => setIsCreateOpen(true)}
                      icon={<Plus className="h-4 w-4" />}
                    >
                      Create your first ticket
                    </Button>
                  ) : undefined
                }
              />
            </div>
          )}

        {!ticketsQuery.isLoading &&
          !ticketsQuery.isError &&
          tickets.length > 0 && (
            <>
              <div className="hidden overflow-x-auto md:block">
                <table className="w-full text-left">
                  <thead className="border-b border-[var(--app-border)] bg-[var(--app-surface-muted)]">
                    <tr className="text-xs uppercase tracking-wide text-[var(--app-text-subtle)]">
                      <th className="px-5 py-3 font-semibold">
                        Ticket
                      </th>
                      <th className="px-5 py-3 font-semibold">
                        Status
                      </th>
                      <th className="px-5 py-3 font-semibold">
                        Priority
                      </th>
                      <th className="px-5 py-3 font-semibold">
                        Assignment
                      </th>
                      <th className="px-5 py-3 text-right font-semibold">
                        Updated
                      </th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-[var(--app-border)]">
                    {tickets.map((ticket) => (
                      <TicketTableRow
                        key={ticket.id}
                        ticket={ticket}
                      />
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="divide-y divide-[var(--app-border)] md:hidden">
                {tickets.map((ticket) => (
                  <TicketMobileCard
                    key={ticket.id}
                    ticket={ticket}
                  />
                ))}
              </div>

              <div className="flex flex-col items-center justify-between gap-3 border-t border-[var(--app-border)] px-5 py-4 sm:flex-row">
                <p className="text-sm text-[var(--app-text-muted)]">
                  Showing {tickets.length} ticket
                  {tickets.length === 1 ? '' : 's'}
                </p>

                {ticketsQuery.hasNextPage && (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => ticketsQuery.fetchNextPage()}
                    disabled={ticketsQuery.isFetchingNextPage}
                  >
                    {ticketsQuery.isFetchingNextPage ? (
                      <>
                        <Spinner size="sm" />
                        Loading more
                      </>
                    ) : (
                      'Load more'
                    )}
                  </Button>
                )}
              </div>
            </>
          )}
      </Card>

      {isCreateOpen && (
        <CreateTicketModal
          organizationId={activeOrganizationId}
          onClose={() => setIsCreateOpen(false)}
          onCreated={(ticket) => {
            setIsCreateOpen(false)
            queryClient.invalidateQueries({
              queryKey: ['tickets', activeOrganizationId],
            })
            window.location.assign(`/app/tickets/${ticket.id}`)
          }}
        />
      )}
    </div>
  )
}

function TicketTableRow({ ticket }: { ticket: Ticket }) {
  return (
    <tr className="group transition-colors hover:bg-[var(--app-surface-muted)]">
      <td className="max-w-md px-5 py-4">
        <Link
          to={`/app/tickets/${ticket.id}`}
          className="block rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-400"
        >
          <div className="flex items-start gap-3">
            <TicketIcon className="mt-0.5 h-4 w-4 shrink-0 text-indigo-500" />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-[var(--app-text)] group-hover:text-indigo-500">
                {ticket.subject}
              </p>
              <p className="mt-1 font-mono text-[11px] text-[var(--app-text-subtle)]">
                {ticket.id.slice(0, 8)}
              </p>
            </div>
          </div>
        </Link>
      </td>

      <td className="px-5 py-4">
        <StatusBadge status={ticket.status} />
      </td>

      <td className="px-5 py-4">
        <PriorityBadge priority={ticket.priority} />
      </td>

      <td className="px-5 py-4">
        <AssignmentCell ticket={ticket} />
      </td>

      <td className="whitespace-nowrap px-5 py-4 text-right text-sm text-[var(--app-text-muted)]">
        {formatRelativeTime(ticket.updatedAt)}
      </td>
    </tr>
  )
}

function TicketMobileCard({ ticket }: { ticket: Ticket }) {
  return (
    <Link
      to={`/app/tickets/${ticket.id}`}
      className="block p-4 transition-colors hover:bg-[var(--app-surface-muted)]"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-start gap-3">
          <TicketIcon className="mt-0.5 h-4 w-4 shrink-0 text-indigo-500" />

          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-[var(--app-text)]">
              {ticket.subject}
            </p>
            <p className="mt-1 font-mono text-[11px] text-[var(--app-text-subtle)]">
              {ticket.id.slice(0, 8)}
            </p>
          </div>
        </div>

        <span className="shrink-0 text-xs text-[var(--app-text-subtle)]">
          {formatRelativeTime(ticket.updatedAt)}
        </span>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2 pl-7">
        <StatusBadge status={ticket.status} />
        <PriorityBadge priority={ticket.priority} />
        <AssignmentCell ticket={ticket} />
      </div>
    </Link>
  )
}

function StatusBadge({ status }: { status: TicketStatus }) {
  const config: Record<
    TicketStatus,
    {
      tone: 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'purple'
      icon: typeof CircleDot
    }
  > = {
    OPEN: { tone: 'info', icon: CircleDot },
    IN_PROGRESS: { tone: 'purple', icon: Clock3 },
    PENDING: { tone: 'warning', icon: Clock3 },
    RESOLVED: { tone: 'success', icon: CheckCircle2 },
    CLOSED: { tone: 'neutral', icon: CheckCircle2 },
  }

  const item = config[status]
  const Icon = item.icon

  return (
    <Badge tone={item.tone} dot={false}>
      <Icon className="h-3 w-3" aria-hidden="true" />
      {statusLabels[status]}
    </Badge>
  )
}

function PriorityBadge({ priority }: { priority: TicketPriority }) {
  const tone =
    priority === 'URGENT'
      ? 'danger'
      : priority === 'HIGH'
        ? 'warning'
        : priority === 'MEDIUM'
          ? 'info'
          : 'neutral'

  return (
    <Badge tone={tone} dot={priority === 'URGENT'}>
      {priority === 'URGENT' && (
        <AlertTriangle className="h-3 w-3" aria-hidden="true" />
      )}
      {priorityLabels[priority]}
    </Badge>
  )
}

function AssignmentCell({ ticket }: { ticket: Ticket }) {
  if (!ticket.assignedAgentId) {
    return (
      <span className="inline-flex items-center gap-1.5 text-xs text-amber-500">
        <UserRound className="h-3.5 w-3.5" aria-hidden="true" />
        Unassigned
      </span>
    )
  }

  return (
    <span className="inline-flex max-w-32 items-center gap-1.5 truncate text-xs text-[var(--app-text-muted)]">
      <UserRound className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
      <span className="truncate">
        {ticket.assignedAgentId.slice(0, 8)}
      </span>
    </span>
  )
}

function TicketListSkeleton() {
  return (
    <div className="divide-y divide-[var(--app-border)]">
      {Array.from({ length: 6 }).map((_, index) => (
        <div
          key={index}
          className="flex items-center gap-4 px-5 py-4"
        >
          <Skeleton className="h-5 w-5 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-3 w-24" />
          </div>
          <Skeleton className="hidden h-6 w-20 sm:block" />
          <Skeleton className="hidden h-6 w-16 sm:block" />
        </div>
      ))}
    </div>
  )
}

interface CreateTicketModalProps {
  organizationId: string
  onClose: () => void
  onCreated: (ticket: Ticket) => void
}

function CreateTicketModal({
  organizationId,
  onClose,
  onCreated,
}: CreateTicketModalProps) {
  const [subject, setSubject] = useState('')
  const [priority, setPriority] =
    useState<TicketPriority>('MEDIUM')
  const [category, setCategory] = useState('')
  const [error, setError] = useState<string | null>(null)

  const createMutation = useMutation({
    mutationFn: () =>
      createTicket(organizationId, {
        subject: subject.trim(),
        priority,
        category: category.trim() || undefined,
      }),
    onSuccess: (ticket) => {
      onCreated(ticket)
    },
    onError: (mutationError) => {
      setError(
        mutationError instanceof Error
          ? mutationError.message
          : 'Failed to create ticket.',
      )
    },
  })

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    if (!subject.trim()) {
      setError('Enter a subject for the ticket.')
      return
    }

    createMutation.mutate()
  }

  return (
    <div
      className="fixed inset-0 z-[60] flex items-end justify-center bg-slate-950/70 p-0 sm:items-center sm:p-4"
      role="presentation"
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) {
          onClose()
        }
      }}
    >
      <div
        className="w-full max-w-lg rounded-t-2xl border border-[var(--app-border)] bg-[var(--app-surface)] p-5 shadow-2xl sm:rounded-2xl sm:p-6"
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-ticket-title"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="create-ticket-title"
              className="text-lg font-semibold text-[var(--app-text)]"
            >
              Create ticket
            </h2>
            <p className="mt-1 text-sm text-[var(--app-text-muted)]">
              Start a new customer support conversation.
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-2 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]"
            aria-label="Close create ticket dialog"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <Input
            label="Subject"
            value={subject}
            onChange={(event) => setSubject(event.target.value)}
            placeholder="Describe the customer's issue"
            autoFocus
            disabled={createMutation.isPending}
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <Select
              label="Priority"
              value={priority}
              onChange={(event) =>
                setPriority(event.target.value as TicketPriority)
              }
              disabled={createMutation.isPending}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </Select>

            <Input
              label="Category"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
              placeholder="Billing, technical..."
              disabled={createMutation.isPending}
            />
          </div>

          {error && (
            <p
              className="rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-500"
              role="alert"
            >
              {error}
            </p>
          )}

          <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
            <Button
              type="button"
              variant="ghost"
              onClick={onClose}
              disabled={createMutation.isPending}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              loading={createMutation.isPending}
              disabled={!subject.trim()}
            >
              Create ticket
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

function formatRelativeTime(value: string) {
  const date = new Date(value)
  const diffSeconds = Math.round(
    (date.getTime() - Date.now()) / 1000,
  )
  const absoluteSeconds = Math.abs(diffSeconds)

  if (absoluteSeconds < 60) {
    return 'Just now'
  }

  const minutes = Math.round(absoluteSeconds / 60)

  if (minutes < 60) {
    return `${minutes}m ago` 
  }

  const hours = Math.round(minutes / 60)

  if (hours < 24) {
    return `${hours}h ago` 
  }

  const days = Math.round(hours / 24)

  if (days < 7) {
    return `${days}d ago` 
  }

  return date.toLocaleDateString()
}

export default TicketsPage
