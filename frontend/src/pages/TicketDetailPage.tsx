import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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

function TicketDetailPage() {
  const { ticketId } = useParams<{ ticketId: string }>()
  const { activeOrganizationId, activeOrganization } = useOrganizations()
  const location = useLocation()

  const isTicketDetailRoute =
    location.pathname.startsWith('/app/tickets/') && ticketId !== undefined

  const queryClient = useQueryClient()

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
    queryFn: () => getTicketMessages(activeOrganizationId!, ticketId!),
    enabled:
      isTicketDetailRoute &&
      activeOrganizationId !== null &&
      ticketId !== undefined &&
      ticketQuery.data?.organizationId === activeOrganizationId,
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
    mutationFn: (request: Parameters<typeof updateTicket>[2]) =>
      updateTicket(activeOrganizationId!, ticketId!, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['ticket', activeOrganizationId, ticketId],
      })

      await queryClient.invalidateQueries({
        queryKey: ['tickets', activeOrganizationId],
      })
    },
  })

  const membersQuery = useQuery({
    queryKey: ['organization-members', activeOrganizationId],
    queryFn: () => getOrganizationMembers(activeOrganizationId!),
    enabled: activeOrganizationId !== null,
  })

  const createMessageMutation = useMutation({
    mutationFn: () =>
      createTicketMessage(activeOrganizationId!, ticketId!, {
        body: body.trim(),
        internalNote,
      }),
    onSuccess: async () => {
      setBody('')
      setInternalNote(false)

      await queryClient.invalidateQueries({
        queryKey: ['ticket-messages', activeOrganizationId, ticketId],
      })

      await queryClient.invalidateQueries({
        queryKey: ['ticket', activeOrganizationId, ticketId],
      })
    },
  })

  if (ticketQuery.isLoading || messagesQuery.isLoading) {
    return <div className="p-6">Loading ticket...</div>
  }

  if (ticketQuery.isError || !ticketQuery.data) {
    return (
      <div className="p-6">
        <p className="text-red-400">Failed to load ticket.</p>
        <Link
          to="/app/tickets"
          className="mt-4 inline-block text-blue-400 hover:underline"
        >
          Back to tickets
        </Link>
      </div>
    )
  }

  const ticket = ticketQuery.data
  const messages = messagesQuery.data ?? []

  const canCreateInternalNote =
    activeOrganization?.role === 'OWNER' ||
    activeOrganization?.role === 'ADMIN' ||
    activeOrganization?.role === 'AGENT'

  const canSend = body.trim().length > 0 && !createMessageMutation.isPending

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!canSend) {
      return
    }

    createMessageMutation.mutate()
  }

  return (
    <div className="p-6">
      <Link
        to="/app/tickets"
        className="text-sm text-slate-400 hover:text-white"
      >
        ← Back to tickets
      </Link>

      <div className="mt-6">
        <h1 className="text-2xl font-semibold">{ticket.subject}</h1>

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Status</span>
            <select
              value={ticket.status}
              disabled={updateTicketMutation.isPending}
              onChange={(event) => {
                updateTicketMutation.mutate({
                  status: event.target.value as typeof ticket.status,
                })
              }}
              className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
            >
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Priority</span>
            <select
              value={ticket.priority}
              disabled={updateTicketMutation.isPending}
              onChange={(event) => {
                updateTicketMutation.mutate({
                  priority: event.target.value as typeof ticket.priority,
                })
              }}
              className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">Category</span>
            <select
              value={ticket.category ?? ''}
              disabled={updateTicketMutation.isPending}
              onChange={(event) => {
                updateTicketMutation.mutate({
                  category: event.target.value || null,
                })
              }}
              className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
            >
              <option value="">Uncategorized</option>
              <option value="BILLING">Billing</option>
              <option value="TECHNICAL">Technical</option>
              <option value="ACCOUNT">Account</option>
              <option value="GENERAL">General</option>
              <option value="REFUND">Refund</option>
            </select>
          </label>
        </div>

        {updateTicketMutation.isError && (
          <p className="mt-3 text-sm text-red-400">
            Failed to update ticket. Please try again.
          </p>
        )}

        <div className="mt-4 flex flex-wrap gap-4">
          <label className="flex items-center gap-2 text-sm">
            <span className="text-slate-400">Assignee</span>
            <select
              value={ticket.assignedAgentId ?? ''}
              disabled={
                membersQuery.isLoading ||
                updateTicketMutation.isPending
              }
              onChange={(event) => {
                updateTicketMutation.mutate({
                  assignedAgentId: event.target.value || null,
                })
              }}
              className="rounded border border-slate-700 bg-slate-900 px-3 py-2 text-sm"
            >
              <option value="">Unassigned</option>

              {(membersQuery.data ?? [])
                .filter((member) => member.role === 'AGENT')
                .map((member) => (
                  <option key={member.userId} value={member.userId}>
                    {member.name} ({member.email})
                  </option>
                ))}
            </select>
          </label>
        </div>

        <section className="mt-8 rounded-xl border border-slate-800 bg-slate-900 p-5">
          <div>
            <h2 className="text-lg font-semibold">
              AI Response Assistant
            </h2>

            <p className="mt-1 text-sm text-slate-400">
              Ask a question and generate an answer grounded in your
              knowledge base.
            </p>
          </div>

          <textarea
            value={ragQuery}
            onChange={(event) => setRagQuery(event.target.value)}
            placeholder="Ask something about your knowledge base..."
            disabled={ragMutation.isPending}
            rows={4}
            className="mt-4 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
          />

          <button
            type="button"
            disabled={
              ragMutation.isPending ||
              !ragQuery.trim() ||
              !activeOrganizationId
            }
            onClick={() => {
              const query = ragQuery.trim()

              if (!query) {
                return
              }

              ragMutation.mutate(query)
            }}
            className="mt-3 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {ragMutation.isPending
              ? 'Generating...'
              : 'Generate answer'}
          </button>

          {ragError && (
            <div className="mt-4 rounded-lg border border-red-900 bg-red-950/30 p-4 text-sm text-red-400">
              {ragError}
            </div>
          )}

          {ragAnswer && (
            <div className="mt-5 rounded-lg border border-slate-700 bg-slate-950 p-4">
              <h3 className="text-sm font-medium text-slate-300">
                Suggested answer
              </h3>

              <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-white">
                {ragAnswer.answer}
              </p>

              {!internalNote && (
                <button
                  type="button"
                  onClick={() => {
                    setBody(ragAnswer.answer)
                    setInternalNote(false)
                  }}
                  className="mt-4 rounded-lg border border-purple-700 bg-purple-950/30 px-4 py-2 text-sm font-medium text-purple-300 hover:bg-purple-950/50"
                >
                  Insert into reply
                </button>
              )}

              {ragAnswer.citations.length > 0 && (
                <div className="mt-5 border-t border-slate-800 pt-4">
                  <h3 className="text-sm font-medium text-slate-300">
                    Knowledge base sources
                  </h3>

                  <div className="mt-3 space-y-2">
                    {ragAnswer.citations.map((citation) => (
                      <div
                        key={`${citation.chunk_id}-${citation.chunk_index}`}
                        className="rounded-lg border border-slate-800 bg-slate-900 p-3"
                      >
                        <p className="text-sm font-medium text-white">
                          {citation.document_title}
                        </p>

                        <p className="mt-1 text-xs text-slate-500">
                          Chunk {citation.chunk_index + 1}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </section>

        {ticket.aiSummary && (
          <div className="mt-6 rounded-lg border border-slate-800 bg-slate-900 p-4">
            <p className="text-sm text-slate-400">AI Summary</p>
            <p className="mt-2">{ticket.aiSummary}</p>
          </div>
        )}

        <section className="mt-8">
          <h2 className="text-lg font-semibold">Conversation</h2>

          <div className="mt-4 space-y-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className="rounded-lg border border-slate-800 bg-slate-900 p-4"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">
                    {message.internalNote ? 'Internal note' : 'Message'}
                  </span>

                  <span className="text-xs text-slate-500">
                    {new Date(message.createdAt).toLocaleString()}
                  </span>
                </div>

                <p className="mt-3 whitespace-pre-wrap text-slate-200">
                  {message.body}
                </p>

                {message.aiGenerated && (
                  <span className="mt-3 inline-block text-xs text-purple-400">
                    AI generated
                  </span>
                )}
              </div>
            ))}

            {messages.length === 0 && (
              <p className="text-slate-400">
                No messages in this conversation.
              </p>
            )}
          </div>
        </section>

        <section className="mt-8">
          <h2 className="text-lg font-semibold">Reply</h2>

          <form onSubmit={handleSubmit} className="mt-4">
            {canCreateInternalNote && (
              <div className="mb-4 flex gap-2">
                <button
                  type="button"
                  onClick={() => setInternalNote(false)}
                  className={`rounded-lg px-4 py-2 text-sm ${
                    !internalNote
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  Public reply
                </button>

                <button
                  type="button"
                  onClick={() => setInternalNote(true)}
                  className={`rounded-lg px-4 py-2 text-sm ${
                    internalNote
                      ? 'bg-amber-600 text-white'
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  Internal note
                </button>
              </div>
            )}

            {internalNote && (
              <div className="mb-3 rounded-lg border border-amber-800 bg-amber-950/30 px-4 py-3 text-sm text-amber-300">
                This message will only be visible to staff.
              </div>
            )}

            <textarea
              value={body}
              onChange={(event) => setBody(event.target.value)}
              placeholder={
                internalNote
                  ? 'Write an internal note...'
                  : 'Write a public reply...'
              }
              maxLength={10000}
              rows={5}
              className="w-full resize-y rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-sm text-white outline-none placeholder:text-slate-500 focus:border-blue-500"
            />

            <div className="mt-3 flex items-center justify-between">
              <span className="text-xs text-slate-500">
                {body.length}/10000
              </span>

              <button
                type="submit"
                disabled={!canSend}
                className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {createMessageMutation.isPending ? 'Sending...' : 'Send'}
              </button>
            </div>

            {createMessageMutation.isError && (
              <p className="mt-3 text-sm text-red-400">
                Failed to send message. Please try again.
              </p>
            )}
          </form>
        </section>
      </div>
    </div>
  )
}

export default TicketDetailPage
