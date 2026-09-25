import { useState } from 'react'
import type { FormEvent } from 'react'
import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  AlertCircle,
  ArrowLeft,
  Bot,
  Clock3,
  FileText,
  MessageSquare,
  Send,
  Sparkles,
  UserRound,
} from 'lucide-react'
import { Link, useLocation, useParams } from 'react-router-dom'
import {
  createTicketMessage,
  getTicket,
  getTicketMessages,
  updateTicket,
} from '../api/tickets'
import { getOrganizationMembers } from '../api/organizations'
import { generateKnowledgeBaseAnswer } from '../api/knowledgeBase'
import type { RagResponse } from '../api/knowledgeBase'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import type {
  TicketPriority,
  TicketStatus,
} from '../features/tickets/types'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Select,
  Skeleton,
  Spinner,
  Textarea,
} from '../components/ui'

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

function TicketDetailPage() {
  const { ticketId } = useParams<{ ticketId: string }>()
  const { activeOrganizationId, activeOrganization } =
    useOrganizations()
  const location = useLocation()
  const queryClient = useQueryClient()

  const isTicketDetailRoute =
    location.pathname.startsWith('/app/tickets/') &&
    ticketId !== undefined

  const [body, setBody] = useState('')
  const [internalNote, setInternalNote] = useState(false)
  const [ragQuery, setRagQuery] = useState('')
  const [ragAnswer, setRagAnswer] = useState<RagResponse | null>(null)
  const [ragError, setRagError] = useState<string | null>(null)

  const ticketQuery = useQuery({
    queryKey: ['ticket', activeOrganizationId, ticketId],
    queryFn: () => getTicket(activeOrganizationId!, ticketId!),
    enabled:
      isTicketDetailRoute &&
      activeOrganizationId !== null &&
      ticketId !== undefined,
  })

  const messagesQuery = useQuery({
    queryKey: ['ticket-messages', activeOrganizationId, ticketId],
    queryFn: () =>
      getTicketMessages(activeOrganizationId!, ticketId!),
    enabled:
      isTicketDetailRoute &&
      activeOrganizationId !== null &&
      ticketId !== undefined &&
      ticketQuery.data?.organizationId === activeOrganizationId,
  })

  const membersQuery = useQuery({
    queryKey: ['organization-members', activeOrganizationId],
    queryFn: () =>
      getOrganizationMembers(activeOrganizationId!),
    enabled: activeOrganizationId !== null,
  })

  const ragMutation = useMutation({
    mutationFn: (query: string) =>
      generateKnowledgeBaseAnswer(
        activeOrganizationId!,
        query,
      ),
    onSuccess: (result) => {
      setRagAnswer(result)
      setRagError(null)
    },
    onError: (error) => {
      setRagAnswer(null)
      setRagError(
        error instanceof Error
          ? error.message
          : 'Failed to generate an AI answer.',
      )
    },
  })

  const updateTicketMutation = useMutation({
    mutationFn: (
      request: Parameters<typeof updateTicket>[2],
    ) =>
      updateTicket(
        activeOrganizationId!,
        ticketId!,
        request,
      ),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['ticket', activeOrganizationId, ticketId],
      })

      await queryClient.invalidateQueries({
        queryKey: ['tickets', activeOrganizationId],
      })
    },
  })

  const createMessageMutation = useMutation({
    mutationFn: () =>
      createTicketMessage(
        activeOrganizationId!,
        ticketId!,
        {
          body: body.trim(),
          internalNote,
        },
      ),
    onSuccess: async () => {
      setBody('')
      setInternalNote(false)

      await queryClient.invalidateQueries({
        queryKey: [
          'ticket-messages',
          activeOrganizationId,
          ticketId,
        ],
      })

      await queryClient.invalidateQueries({
        queryKey: ['ticket', activeOrganizationId, ticketId],
      })
    },
  })

  if (ticketQuery.isLoading || messagesQuery.isLoading) {
    return <TicketDetailSkeleton />
  }

  if (ticketQuery.isError || !ticketQuery.data) {
    return (
      <ErrorState
        title="Ticket could not be loaded"
        description="The ticket may have been deleted or you may no longer have access to it."
        action={
          <Link to="/app/tickets">
            <Button
              variant="secondary"
              size="sm"
              icon={<ArrowLeft className="h-4 w-4" />}
            >
              Back to tickets
            </Button>
          </Link>
        }
      />
    )
  }

  const ticket = ticketQuery.data
  const messages = messagesQuery.data ?? []

  const canCreateInternalNote =
    activeOrganization?.role === 'OWNER' ||
    activeOrganization?.role === 'ADMIN' ||
    activeOrganization?.role === 'AGENT'

  const canSend =
    body.trim().length > 0 &&
    !createMessageMutation.isPending

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (canSend) {
      createMessageMutation.mutate()
    }
  }

  function updateStatus(value: string) {
    updateTicketMutation.mutate({
      status: value as TicketStatus,
    })
  }

  function updatePriority(value: string) {
    updateTicketMutation.mutate({
      priority: value as TicketPriority,
    })
  }

  function updateCategory(value: string) {
    updateTicketMutation.mutate({
      category: value || null,
    })
  }

  function updateAssignee(value: string) {
    updateTicketMutation.mutate({
      assignedAgentId: value || null,
    })
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link
          to="/app/tickets"
          className="inline-flex items-center gap-2 text-sm text-[var(--app-text-muted)] transition-colors hover:text-[var(--app-text)]"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to tickets
        </Link>

        <div className="flex items-center gap-2">
          <StatusBadge status={ticket.status} />
          <PriorityBadge priority={ticket.priority} />
        </div>
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="min-w-0 space-y-6">
          <Card className="p-5 sm:p-6">
            <div className="flex items-start gap-4">
              <div className="rounded-xl bg-indigo-500/15 p-3 text-indigo-500">
                <MessageSquare className="h-6 w-6" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="font-mono text-xs text-[var(--app-text-subtle)]">
                  Ticket {ticket.id.slice(0, 8)}
                </p>

                <h1 className="mt-1 text-xl font-semibold tracking-tight text-[var(--app-text)] sm:text-2xl">
                  {ticket.subject}
                </h1>

                <div className="mt-3 flex flex-wrap items-center gap-3 text-sm text-[var(--app-text-muted)]">
                  <span className="inline-flex items-center gap-1.5">
                    <Clock3 className="h-4 w-4" />
                    Updated {formatDate(ticket.updatedAt)}
                  </span>

                  {ticket.category && (
                    <span className="inline-flex items-center gap-1.5">
                      <FileText className="h-4 w-4" />
                      {formatCategory(ticket.category)}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {ticket.aiSummary && (
              <div className="mt-6 rounded-lg border border-indigo-500/20 bg-indigo-500/5 p-4">
                <div className="flex items-center gap-2 text-sm font-medium text-indigo-400">
                  <Sparkles className="h-4 w-4" />
                  AI summary
                </div>

                <p className="mt-2 text-sm leading-6 text-[var(--app-text-muted)]">
                  {ticket.aiSummary}
                </p>
              </div>
            )}
          </Card>

          <Card className="p-5 sm:p-6">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="flex items-center gap-2 text-lg font-semibold text-[var(--app-text)]">
                  <MessageSquare className="h-5 w-5 text-indigo-500" />
                  Conversation
                </h2>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  {messages.length} message
                  {messages.length === 1 ? '' : 's'}
                </p>
              </div>
            </div>

            {messages.length === 0 ? (
              <div className="mt-5">
                <EmptyState
                  title="No messages yet"
                  description="Send the first reply to start this conversation."
                  icon={<MessageSquare className="h-5 w-5" />}
                />
              </div>
            ) : (
              <div className="mt-6 space-y-4">
                {messages.map((message) => (
                  <MessageCard
                    key={message.id}
                    internalNote={message.internalNote}
                    aiGenerated={message.aiGenerated}
                    authorId={message.authorId}
                    createdAt={message.createdAt}
                    body={message.body}
                  />
                ))}
              </div>
            )}
          </Card>

          <ReplyComposer
            body={body}
            internalNote={internalNote}
            canCreateInternalNote={canCreateInternalNote}
            canSend={canSend}
            isPending={createMessageMutation.isPending}
            isError={createMessageMutation.isError}
            onBodyChange={setBody}
            onInternalNoteChange={setInternalNote}
            onSubmit={handleSubmit}
          />
        </div>

        <aside className="min-w-0 space-y-6">
          <Card className="p-5">
            <h2 className="text-base font-semibold text-[var(--app-text)]">
              Ticket details
            </h2>

            <div className="mt-5 space-y-4">
              <Select
                label="Status"
                value={ticket.status}
                disabled={updateTicketMutation.isPending}
                onChange={(event) =>
                  updateStatus(event.target.value)
                }
              >
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="PENDING">Pending</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </Select>

              <Select
                label="Priority"
                value={ticket.priority}
                disabled={updateTicketMutation.isPending}
                onChange={(event) =>
                  updatePriority(event.target.value)
                }
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </Select>

              <Select
                label="Category"
                value={ticket.category ?? ''}
                disabled={updateTicketMutation.isPending}
                onChange={(event) =>
                  updateCategory(event.target.value)
                }
              >
                <option value="">Uncategorized</option>
                <option value="BILLING">Billing</option>
                <option value="TECHNICAL">Technical</option>
                <option value="ACCOUNT">Account</option>
                <option value="GENERAL">General</option>
                <option value="REFUND">Refund</option>
              </Select>

              <Select
                label="Assignee"
                value={ticket.assignedAgentId ?? ''}
                disabled={
                  membersQuery.isLoading ||
                  updateTicketMutation.isPending
                }
                onChange={(event) =>
                  updateAssignee(event.target.value)
                }
              >
                <option value="">Unassigned</option>

                {(membersQuery.data ?? [])
                  .filter((member) => member.role === 'AGENT')
                  .map((member) => (
                    <option key={member.userId} value={member.userId}>
                      {member.name}
                    </option>
                  ))}
              </Select>
            </div>

            {updateTicketMutation.isPending && (
              <div className="mt-4 flex items-center gap-2 text-xs text-[var(--app-text-muted)]">
                <Spinner size="sm" />
                Saving changes...
              </div>
            )}

            {updateTicketMutation.isError && (
              <div className="mt-4 flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-500">
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                Failed to update ticket. Please try again.
              </div>
            )}
          </Card>

          <KnowledgeBaseAssistant
            ragQuery={ragQuery}
            ragAnswer={ragAnswer}
            ragError={ragError}
            isPending={ragMutation.isPending}
            internalNote={internalNote}
            onQueryChange={setRagQuery}
            onGenerate={() => {
              const query = ragQuery.trim()

              if (query) {
                ragMutation.mutate(query)
              }
            }}
            onInsert={() => {
              if (ragAnswer) {
                setBody(ragAnswer.answer)
                setInternalNote(false)
              }
            }}
          />

          <Card className="p-5">
            <div className="flex items-start gap-3">
              <UserRound className="mt-0.5 h-5 w-5 text-[var(--app-text-muted)]" />

              <div className="min-w-0">
                <h2 className="text-base font-semibold text-[var(--app-text)]">
                  Customer
                </h2>
                <p className="mt-2 break-all font-mono text-xs text-[var(--app-text-muted)]">
                  {ticket.customerId}
                </p>
              </div>
            </div>
          </Card>
        </aside>
      </div>
    </div>
  )
}

interface MessageCardProps {
  internalNote: boolean
  aiGenerated: boolean
  authorId: string
  createdAt: string
  body: string
}

function MessageCard({
  internalNote,
  aiGenerated,
  authorId,
  createdAt,
  body,
}: MessageCardProps) {
  return (
    <article
      className={[
        'rounded-xl border p-4',
        internalNote
          ? 'border-amber-500/30 bg-amber-500/5'
          : 'border-[var(--app-border)] bg-[var(--app-surface-muted)]',
      ].join(' ')}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-center gap-3">
          <div
            className={[
              'flex h-8 w-8 shrink-0 items-center justify-center rounded-full',
              internalNote
                ? 'bg-amber-500/15 text-amber-500'
                : 'bg-indigo-500/15 text-indigo-500',
            ].join(' ')}
          >
            {internalNote ? (
              <AlertCircle className="h-4 w-4" />
            ) : (
              <UserRound className="h-4 w-4" />
            )}
          </div>

          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-sm font-medium text-[var(--app-text)]">
                {internalNote ? 'Internal note' : 'Message'}
              </p>

              {aiGenerated && (
                <Badge tone="purple">
                  <Bot className="h-3 w-3" />
                  AI generated
                </Badge>
              )}
            </div>

            <p className="mt-0.5 truncate font-mono text-[11px] text-[var(--app-text-subtle)]">
              {authorId.slice(0, 8)}
            </p>
          </div>
        </div>

        <time className="shrink-0 text-xs text-[var(--app-text-subtle)]">
          {formatDate(createdAt)}
        </time>
      </div>

      <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-[var(--app-text)]">
        {body}
      </p>
    </article>
  )
}

interface ReplyComposerProps {
  body: string
  internalNote: boolean
  canCreateInternalNote: boolean
  canSend: boolean
  isPending: boolean
  isError: boolean
  onBodyChange: (value: string) => void
  onInternalNoteChange: (value: boolean) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

function ReplyComposer({
  body,
  internalNote,
  canCreateInternalNote,
  canSend,
  isPending,
  isError,
  onBodyChange,
  onInternalNoteChange,
  onSubmit,
}: ReplyComposerProps) {
  return (
    <Card
      className={[
        'p-5 sm:p-6',
        internalNote ? 'border-amber-500/40' : '',
      ].join(' ')}
    >
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-[var(--app-text)]">
            {internalNote ? 'Internal note' : 'Reply to customer'}
          </h2>
          <p className="mt-1 text-sm text-[var(--app-text-muted)]">
            {internalNote
              ? 'Only staff members can see this message.'
              : 'Send a public response to the customer.'}
          </p>
        </div>

        <Send className="h-5 w-5 text-indigo-500" />
      </div>

      {canCreateInternalNote && (
        <div className="mt-5 inline-flex rounded-lg bg-[var(--app-surface-muted)] p-1">
          <button
            type="button"
            onClick={() => onInternalNoteChange(false)}
            className={[
              'rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
              !internalNote
                ? 'bg-[var(--app-surface)] text-[var(--app-text)] shadow-sm'
                : 'text-[var(--app-text-muted)] hover:text-[var(--app-text)]',
            ].join(' ')}
          >
            Public reply
          </button>

          <button
            type="button"
            onClick={() => onInternalNoteChange(true)}
            className={[
              'rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
              internalNote
                ? 'bg-amber-500 text-white shadow-sm'
                : 'text-[var(--app-text-muted)] hover:text-[var(--app-text)]',
            ].join(' ')}
          >
            Internal note
          </button>
        </div>
      )}

      {internalNote && (
        <div className="mt-4 flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 p-3 text-sm text-amber-500">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          This message will only be visible to staff.
        </div>
      )}

      <form onSubmit={onSubmit} className="mt-4">
        <Textarea
          value={body}
          onChange={(event) => onBodyChange(event.target.value)}
          placeholder={
            internalNote
              ? 'Write an internal note...'
              : 'Write a public reply...'
          }
          maxLength={10000}
          rows={6}
          disabled={isPending}
        />

        <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <span className="text-xs text-[var(--app-text-subtle)]">
            {body.length}/10000
          </span>

          <Button
            type="submit"
            disabled={!canSend}
            loading={isPending}
            icon={<Send className="h-4 w-4" />}
          >
            {internalNote ? 'Add internal note' : 'Send reply'}
          </Button>
        </div>

        {isError && (
          <p className="mt-3 text-sm text-red-500" role="alert">
            Failed to send message. Please try again.
          </p>
        )}
      </form>
    </Card>
  )
}

interface KnowledgeBaseAssistantProps {
  ragQuery: string
  ragAnswer: RagResponse | null
  ragError: string | null
  isPending: boolean
  internalNote: boolean
  onQueryChange: (value: string) => void
  onGenerate: () => void
  onInsert: () => void
}

function KnowledgeBaseAssistant({
  ragQuery,
  ragAnswer,
  ragError,
  isPending,
  internalNote,
  onQueryChange,
  onGenerate,
  onInsert,
}: KnowledgeBaseAssistantProps) {
  return (
    <Card className="overflow-hidden">
      <div className="border-b border-[var(--app-border)] bg-indigo-500/5 p-5">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-indigo-500/15 p-2 text-indigo-500">
            <Sparkles className="h-5 w-5" />
          </div>

          <div>
            <h2 className="font-semibold text-[var(--app-text)]">
              AI response assistant
            </h2>
            <p className="mt-1 text-sm text-[var(--app-text-muted)]">
              Generate an answer grounded in your knowledge base.
            </p>
          </div>
        </div>
      </div>

      <div className="p-5">
        <Textarea
          value={ragQuery}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="Ask about a refund policy, account process, or product issue..."
          rows={4}
          disabled={isPending}
        />

        <div className="mt-3 flex justify-end">
          <Button
            size="sm"
            onClick={onGenerate}
            disabled={isPending || !ragQuery.trim()}
            loading={isPending}
            icon={<Sparkles className="h-4 w-4" />}
          >
            Generate answer
          </Button>
        </div>

        {ragError && (
          <div className="mt-4 flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-500">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            {ragError}
          </div>
        )}

        {ragAnswer && (
          <div className="mt-5 rounded-lg border border-indigo-500/20 bg-indigo-500/5 p-4">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-sm font-semibold text-[var(--app-text)]">
                Suggested answer
              </h3>

              {!internalNote && (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={onInsert}
                  icon={<FileText className="h-4 w-4" />}
                >
                  Insert into reply
                </Button>
              )}
            </div>

            <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-[var(--app-text)]">
              {ragAnswer.answer}
            </p>

            {ragAnswer.citations.length > 0 && (
              <div className="mt-5 border-t border-[var(--app-border)] pt-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-[var(--app-text-subtle)]">
                  Sources
                </p>

                <div className="mt-3 space-y-2">
                  {ragAnswer.citations.map((citation) => (
                    <div
                      key={`${citation.chunk_id}-${citation.chunk_index}`}
                      className="flex items-center gap-3 rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] p-3"
                    >
                      <FileText className="h-4 w-4 shrink-0 text-indigo-500" />

                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-[var(--app-text)]">
                          {citation.document_title}
                        </p>
                        <p className="mt-1 text-xs text-[var(--app-text-muted)]">
                          Knowledge base chunk {citation.chunk_index + 1}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Card>
  )
}

function StatusBadge({ status }: { status: TicketStatus }) {
  const tone =
    status === 'RESOLVED'
      ? 'success'
      : status === 'PENDING'
        ? 'warning'
        : status === 'CLOSED'
          ? 'neutral'
          : status === 'IN_PROGRESS'
            ? 'purple'
            : 'info'

  return (
    <Badge tone={tone} dot>
      {statusLabels[status]}
    </Badge>
  )
}

function PriorityBadge({
  priority,
}: {
  priority: TicketPriority
}) {
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
      {priorityLabels[priority]}
    </Badge>
  )
}

function TicketDetailSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-5 w-32" />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <Card className="p-6">
            <Skeleton className="h-8 w-3/4" />
            <Skeleton className="mt-4 h-4 w-1/2" />
            <Skeleton className="mt-8 h-24 w-full" />
          </Card>

          <Card className="p-6">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="mt-6 h-32 w-full" />
            <Skeleton className="mt-4 h-32 w-full" />
          </Card>

          <Card className="p-6">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="mt-5 h-36 w-full" />
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="p-6">
            <Skeleton className="h-6 w-32" />
            <Skeleton className="mt-6 h-10 w-full" />
            <Skeleton className="mt-4 h-10 w-full" />
            <Skeleton className="mt-4 h-10 w-full" />
          </Card>

          <Card className="p-6">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="mt-5 h-28 w-full" />
          </Card>
        </div>
      </div>
    </div>
  )
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

function formatCategory(category: string) {
  return category
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (character) =>
      character.toUpperCase(),
    )
}

export default TicketDetailPage
