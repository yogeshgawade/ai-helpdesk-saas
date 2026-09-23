import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useInfiniteQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import {
  createTicket,
  createTicketMessage,
  getTickets,
} from '../api/tickets'
import { getOrganizationMembers } from '../api/organizations'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import type {
  TicketPriority,
  TicketStatus,
} from '../features/tickets/types'

function TicketsPage() {
  const {
    activeOrganizationId,
    activeOrganization,
  } = useOrganizations()

  const queryClient = useQueryClient()

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [subject, setSubject] = useState('')
  const [createPriority, setCreatePriority] = useState<TicketPriority>('MEDIUM')
  const [createCategory, setCreateCategory] = useState('')
  const [customerId, setCustomerId] = useState('')
  const [initialMessage, setInitialMessage] = useState('')
  const [createError, setCreateError] = useState<string | null>(null)

  const isStaffRole =
    activeOrganization?.role === 'OWNER' ||
    activeOrganization?.role === 'ADMIN' ||
    activeOrganization?.role === 'AGENT'

  const membersQuery = useQuery({
    queryKey: ['organization-members', activeOrganizationId],
    queryFn: () => getOrganizationMembers(activeOrganizationId!),
    enabled: activeOrganizationId !== null && isStaffRole,
  })

  const customers =
    membersQuery.data?.filter(
      (member) => member.role === 'CUSTOMER',
    ) ?? []

  const createTicketMutation = useMutation({
    mutationFn: async () => {
      if (!activeOrganizationId) {
        throw new Error('No active organization selected.')
      }

      if (!subject.trim()) {
        throw new Error('Subject is required.')
      }

      if (isStaffRole && !customerId) {
        throw new Error('Customer is required.')
      }

      const ticket = await createTicket(
        activeOrganizationId,
        {
          subject: subject.trim(),
          priority: createPriority,
          category: createCategory || undefined,
          customerId: isStaffRole ? customerId : undefined,
        },
      )

      if (initialMessage.trim()) {
        await createTicketMessage(
          activeOrganizationId,
          ticket.id,
          {
            body: initialMessage.trim(),
            internalNote: false,
          },
        )
      }

      return ticket
    },
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['tickets', activeOrganizationId],
      })

      setIsCreateModalOpen(false)
      setSubject('')
      setCreatePriority('MEDIUM')
      setCreateCategory('')
      setCustomerId('')
      setInitialMessage('')
      setCreateError(null)
    },
    onError: (error) => {
      setCreateError(
        error instanceof Error
          ? error.message
          : 'Failed to create ticket.',
      )
    },
  })

  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<TicketStatus | ''>('')
  const [priority, setPriority] = useState<TicketPriority | ''>('')
  const [category, setCategory] = useState('')
  const [sort, setSort] = useState<'newest' | 'oldest'>('newest')
  const [pageSize, setPageSize] = useState(20)

  const {
    data,
    isLoading,
    isError,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useInfiniteQuery({
    queryKey: [
      'tickets',
      activeOrganizationId,
      search,
      status,
      priority,
      category,
      sort,
      pageSize,
    ],
    queryFn: ({ pageParam }) =>
      getTickets(activeOrganizationId!, {
        search: search || undefined,
        status: status || undefined,
        priority: priority || undefined,
        category: category || undefined,
        sort,
        cursor: pageParam,
        limit: pageSize,
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor : undefined,
    enabled: activeOrganizationId !== null,
  })

  const tickets =
    data?.pages.flatMap((page) => page.tickets) ?? []

  function clearFilters() {
    setSearch('')
    setStatus('')
    setPriority('')
    setCategory('')
  }

  if (isLoading) {
    return <div className="p-6">Loading tickets...</div>
  }

  if (isError) {
    return (
      <div className="p-6 text-red-400">
        Failed to load tickets.
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Tickets</h1>

          <p className="mt-2 text-slate-400">
            Organization: {activeOrganizationId}
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setCreateError(null)
            setIsCreateModalOpen(true)
          }}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500"
        >
          Create Ticket
        </button>
      </div>

      <div className="mt-6 rounded-xl border border-slate-800 bg-slate-900 p-4">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Search</span>
            <input
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search subject..."
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
            />
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Status</span>
            <select
              value={status}
              onChange={(event) =>
                setStatus(event.target.value as TicketStatus | '')
              }
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
            >
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In progress</option>
              <option value="PENDING">Pending</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Priority</span>
            <select
              value={priority}
              onChange={(event) =>
                setPriority(event.target.value as TicketPriority | '')
              }
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
            >
              <option value="">All priorities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Sort</span>
            <select
              value={sort}
              onChange={(event) =>
                setSort(event.target.value as 'newest' | 'oldest')
              }
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
            >
              <option value="newest">Newest first</option>
              <option value="oldest">Oldest first</option>
            </select>
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Page size</span>
            <select
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Category</span>
            <select
              value={category}
              onChange={(event) => setCategory(event.target.value)}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
            >
              <option value="">All categories</option>
              <option value="BILLING">Billing</option>
              <option value="TECHNICAL">Technical</option>
              <option value="ACCOUNT">Account</option>
              <option value="GENERAL">General</option>
              <option value="REFUND">Refund</option>
            </select>
          </label>
        </div>

        <button
          type="button"
          onClick={clearFilters}
          className="mt-4 rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800"
        >
          Clear filters
        </button>
      </div>

      {isCreateModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-lg rounded-xl border border-slate-800 bg-slate-900 p-6 shadow-xl">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold">Create Ticket</h2>

              <button
                type="button"
                onClick={() => {
                  if (!createTicketMutation.isPending) {
                    setIsCreateModalOpen(false)
                    setCreateError(null)
                  }
                }}
                className="text-slate-400 hover:text-white"
              >
                ✕
              </button>
            </div>

            <div className="mt-6 space-y-4">
              <label className="block text-sm">
                <span className="mb-2 block text-slate-400">
                  Subject
                </span>

                <input
                  type="text"
                  value={subject}
                  onChange={(event) => setSubject(event.target.value)}
                  placeholder="Describe the issue..."
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
                />
              </label>

              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-sm">
                  <span className="mb-2 block text-slate-400">
                    Priority
                  </span>

                  <select
                    value={createPriority}
                    onChange={(event) =>
                      setCreatePriority(
                        event.target.value as TicketPriority,
                      )
                    }
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
                  >
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                  </select>
                </label>

                <label className="block text-sm">
                  <span className="mb-2 block text-slate-400">
                    Category
                  </span>

                  <select
                    value={createCategory}
                    onChange={(event) =>
                      setCreateCategory(event.target.value)
                    }
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
                  >
                    <option value="">No category</option>
                    <option value="BILLING">Billing</option>
                    <option value="TECHNICAL">Technical</option>
                    <option value="ACCOUNT">Account</option>
                    <option value="GENERAL">General</option>
                    <option value="REFUND">Refund</option>
                  </select>
                </label>
              </div>

              {isStaffRole && (
                <label className="block text-sm">
                  <span className="mb-2 block text-slate-400">
                    Customer
                  </span>

                  <select
                    value={customerId}
                    onChange={(event) =>
                      setCustomerId(event.target.value)
                    }
                    disabled={membersQuery.isLoading}
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
                  >
                    <option value="">
                      {membersQuery.isLoading
                        ? 'Loading customers...'
                        : 'Select customer'}
                    </option>

                    {customers.map((customer) => (
                      <option
                        key={customer.userId}
                        value={customer.userId}
                      >
                        {customer.name} ({customer.email})
                      </option>
                    ))}
                  </select>

                  {!membersQuery.isLoading &&
                    customers.length === 0 && (
                      <p className="mt-2 text-xs text-amber-400">
                        No customers are available in this organization.
                      </p>
                    )}
                </label>
              )}

              <label className="block text-sm">
                <span className="mb-2 block text-slate-400">
                  Initial message
                </span>

                <textarea
                  value={initialMessage}
                  onChange={(event) =>
                    setInitialMessage(event.target.value)
                  }
                  rows={5}
                  placeholder="Describe the issue in more detail..."
                  className="w-full resize-none rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
                />
              </label>

              {createError && (
                <p className="rounded-lg border border-red-900 bg-red-950/40 px-3 py-2 text-sm text-red-400">
                  {createError}
                </p>
              )}

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  disabled={createTicketMutation.isPending}
                  onClick={() => {
                    setIsCreateModalOpen(false)
                    setCreateError(null)
                  }}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800 disabled:opacity-50"
                >
                  Cancel
                </button>

                <button
                  type="button"
                  disabled={
                    createTicketMutation.isPending ||
                    !subject.trim() ||
                    (isStaffRole &&
                      (!customerId || customers.length === 0))
                  }
                  onClick={() => createTicketMutation.mutate()}
                  className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {createTicketMutation.isPending
                    ? 'Creating...'
                    : 'Create Ticket'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="mt-6">
        <p className="text-slate-400">
          Tickets:{' '}
          <strong className="text-white">{tickets.length}</strong>
        </p>

        <div className="mt-4 space-y-3">
          {tickets.map((ticket) => (
            <Link
              key={ticket.id}
              to={`/app/tickets/${ticket.id}`}
              className="block rounded-lg border border-slate-800 bg-slate-900 p-4 hover:bg-slate-800"
            >
              <h2 className="font-medium">{ticket.subject}</h2>

              <div className="mt-2 flex flex-wrap gap-4 text-sm text-slate-400">
                <span>Status: {ticket.status}</span>
                <span>Priority: {ticket.priority}</span>
                <span>
                  Category: {ticket.category ?? 'Uncategorized'}
                </span>
              </div>
            </Link>
          ))}

          {tickets.length === 0 && (
            <p className="text-slate-400">
              No tickets match the current filters.
            </p>
          )}

          {hasNextPage && (
            <button
              type="button"
              disabled={isFetchingNextPage}
              onClick={() => fetchNextPage()}
              className="w-full rounded-lg border border-slate-700 px-4 py-3 text-sm text-slate-300 hover:bg-slate-800 disabled:opacity-50"
            >
              {isFetchingNextPage ? 'Loading...' : 'Load more'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

export default TicketsPage
