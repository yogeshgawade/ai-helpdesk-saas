import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  AlertCircle,
  Clock3,
  Plus,
  ShieldCheck,
  Trash2,
} from 'lucide-react'
import {
  createSlaPolicy,
  deleteSlaPolicy,
  getSlaPolicies,
  updateSlaPolicy,
  type SlaPolicy,
  type SlaPolicyRequest,
  type TicketPriority,
} from '../api/slaPolicies'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import {
  Button,
  Card,
  EmptyState,
  ErrorState,
  PageHeader,
  Skeleton,
} from '../components/ui'

const EMPTY_FORM: SlaPolicyRequest = {
  name: '',
  firstResponseMinutes: 60,
  resolutionMinutes: 240,
  priority: 'MEDIUM',
}

function SlaPoliciesPage() {
  const { activeOrganizationId } = useOrganizations()
  const queryClient = useQueryClient()

  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingPolicy, setEditingPolicy] = useState<SlaPolicy | null>(null)
  const [form, setForm] = useState<SlaPolicyRequest>(EMPTY_FORM)
  const [isDeleting, setIsDeleting] = useState<SlaPolicy | null>(null)
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})

  const {
    data: policies = [],
    isLoading,
    error,
  } = useQuery({
    queryKey: ['sla-policies', activeOrganizationId],
    queryFn: () => getSlaPolicies(activeOrganizationId!),
    enabled: !!activeOrganizationId,
  })

  const createMutation = useMutation({
    mutationFn: (request: SlaPolicyRequest) =>
      createSlaPolicy(activeOrganizationId!, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sla-policies', activeOrganizationId] })
      handleCloseModal()
    },
    onError: (err: Error) => {
      setFormErrors({ submit: err.message || 'Failed to create policy' })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: string; request: SlaPolicyRequest }) =>
      updateSlaPolicy(activeOrganizationId!, id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sla-policies', activeOrganizationId] })
      handleCloseModal()
    },
    onError: (err: Error) => {
      setFormErrors({ submit: err.message || 'Failed to update policy' })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (policyId: string) =>
      deleteSlaPolicy(activeOrganizationId!, policyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sla-policies', activeOrganizationId] })
      setIsDeleting(null)
    },
  })

  function handleOpenCreate() {
    setEditingPolicy(null)
    setForm(EMPTY_FORM)
    setFormErrors({})
    setIsFormOpen(true)
  }

  function handleOpenEdit(policy: SlaPolicy) {
    setEditingPolicy(policy)
    setForm({
      name: policy.name,
      firstResponseMinutes: policy.firstResponseMinutes,
      resolutionMinutes: policy.resolutionMinutes,
      priority: policy.priority,
    })
    setFormErrors({})
    setIsFormOpen(true)
  }

  function handleCloseModal() {
    setIsFormOpen(false)
    setEditingPolicy(null)
    setForm(EMPTY_FORM)
    setFormErrors({})
  }

  function handleDeleteClick(policy: SlaPolicy) {
    setIsDeleting(policy)
  }

  function handleConfirmDelete() {
    if (isDeleting) {
      deleteMutation.mutate(isDeleting.id)
    }
  }

  function validateForm(): boolean {
    const errors: Record<string, string> = {}

    if (!form.name.trim()) {
      errors.name = 'Policy name is required'
    }

    if (form.firstResponseMinutes < 1) {
      errors.firstResponseMinutes = 'First response time must be at least 1 minute'
    }

    if (form.resolutionMinutes < 1) {
      errors.resolutionMinutes = 'Resolution time must be at least 1 minute'
    }

    if (form.firstResponseMinutes >= form.resolutionMinutes) {
      errors.resolutionMinutes = 'Resolution time must be greater than first response time'
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!validateForm()) {
      return
    }

    if (editingPolicy) {
      updateMutation.mutate({
        id: editingPolicy.id,
        request: form,
      })
    } else {
      createMutation.mutate(form)
    }
  }

  if (!activeOrganizationId) {
    return (
      <EmptyState
        title="Select an organization"
        description="Choose an organization from the sidebar to view SLA policies."
        icon={<ShieldCheck className="h-6 w-6" />}
      />
    )
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="SLA Policies"
          description="Loading policies..."
        />
        <Card className="p-5">
          <div className="space-y-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="flex items-center gap-4">
                <Skeleton className="h-6 w-20" />
                <Skeleton className="h-4 w-40 flex-1" />
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-4 w-24" />
              </div>
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
          title="SLA Policies"
          description="Define response and resolution time targets"
        />
        <ErrorState
          title="Failed to load SLA policies"
          description="Please try again later or refresh the page."
        />
      </div>
    )
  }

  const hasPolicies = policies.length > 0

  return (
    <div className="space-y-6">
      <PageHeader
        title="SLA Policies"
        description="Define response and resolution time targets for each priority level"
        actions={
          <Button
            variant="primary"
            size="sm"
            onClick={handleOpenCreate}
            disabled={!hasPolicies && createMutation.isPending}
          >
            <Plus className="mr-2 h-4 w-4" />
            Add Policy
          </Button>
        }
      />

      {!hasPolicies ? (
        <Card className="p-8">
          <EmptyState
            title="No SLA policies yet"
            description="Create your first SLA policy to define response and resolution time targets."
            icon={<ShieldCheck className="h-6 w-6" />}
            action={
              <Button
                variant="primary"
                size="sm"
                onClick={handleOpenCreate}
              >
                <Plus className="mr-2 h-4 w-4" />
                Create Policy
              </Button>
            }
          />
        </Card>
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="border-b border-[var(--app-border)] bg-[var(--app-surface-muted)]">
                <tr>
                  <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-[var(--app-text-muted)]">
                    Priority
                  </th>
                  <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-[var(--app-text-muted)]">
                    Policy Name
                  </th>
                  <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-[var(--app-text-muted)]">
                    First Response
                  </th>
                  <th className="px-5 py-3 text-left text-xs font-semibold uppercase tracking-wide text-[var(--app-text-muted)]">
                    Resolution
                  </th>
                  <th className="px-5 py-3 text-right text-xs font-semibold uppercase tracking-wide text-[var(--app-text-muted)]">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--app-border)]">
                {policies.map((policy) => (
                  <tr
                    key={policy.id}
                    className="group hover:bg-[var(--app-surface-muted)]"
                  >
                    <td className="px-5 py-4">
                      <PriorityBadge priority={policy.priority} />
                    </td>
                    <td className="px-5 py-4 text-sm font-medium text-[var(--app-text)]">
                      {policy.name}
                    </td>
                    <td className="px-5 py-4 text-sm text-[var(--app-text-muted)]">
                      <div className="flex items-center gap-1.5">
                        <Clock3 className="h-4 w-4" />
                        {formatDuration(policy.firstResponseMinutes)}
                      </div>
                    </td>
                    <td className="px-5 py-4 text-sm text-[var(--app-text-muted)]">
                      <div className="flex items-center gap-1.5">
                        <Clock3 className="h-4 w-4" />
                        {formatDuration(policy.resolutionMinutes)}
                      </div>
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleOpenEdit(policy)}
                          disabled={updateMutation.isPending}
                        >
                          <PencilIcon className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteClick(policy)}
                          disabled={deleteMutation.isPending}
                          className="text-red-400 hover:text-red-300"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Create/Edit Modal */}
      {isFormOpen && (
        <Modal
          title={editingPolicy ? 'Edit SLA Policy' : 'Create SLA Policy'}
          onClose={handleCloseModal}
        >
          <form onSubmit={handleSubmit} className="space-y-5">
            {formErrors.submit && (
              <div className="rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-400">
                {formErrors.submit}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-[var(--app-text)]">
                Policy Name
              </label>
              <input
                type="text"
                value={form.name}
                onChange={(e) =>
                  setForm({ ...form, name: e.target.value })
                }
                className={`mt-1.5 w-full rounded-lg border bg-[var(--app-surface)] px-3 py-2 text-sm text-[var(--app-text)] outline-none transition-colors focus:ring-2 focus:ring-indigo-500 ${
                  formErrors.name
                    ? 'border-red-500/50'
                    : 'border-[var(--app-border)] focus:border-indigo-500'
                }`}
                placeholder="e.g., Standard Support"
                autoFocus
              />
              {formErrors.name && (
                <p className="mt-1.5 text-xs text-red-400">
                  {formErrors.name}
                </p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-[var(--app-text)]">
                Priority
              </label>
              <select
                value={form.priority}
                onChange={(e) =>
                  setForm({ ...form, priority: e.target.value as TicketPriority })
                }
                className="mt-1.5 w-full rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2 text-sm text-[var(--app-text)] outline-none transition-colors focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-[var(--app-text)]">
                  First Response (minutes)
                </label>
                <input
                  type="number"
                  min="1"
                  value={form.firstResponseMinutes}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      firstResponseMinutes: Number(e.target.value),
                    })
                  }
                  className={`mt-1.5 w-full rounded-lg border bg-[var(--app-surface)] px-3 py-2 text-sm text-[var(--app-text)] outline-none transition-colors focus:ring-2 focus:ring-indigo-500 ${
                    formErrors.firstResponseMinutes
                      ? 'border-red-500/50'
                      : 'border-[var(--app-border)] focus:border-indigo-500'
                  }`}
                />
                {formErrors.firstResponseMinutes && (
                  <p className="mt-1.5 text-xs text-red-400">
                    {formErrors.firstResponseMinutes}
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-[var(--app-text)]">
                  Resolution (minutes)
                </label>
                <input
                  type="number"
                  min="1"
                  value={form.resolutionMinutes}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      resolutionMinutes: Number(e.target.value),
                    })
                  }
                  className={`mt-1.5 w-full rounded-lg border bg-[var(--app-surface)] px-3 py-2 text-sm text-[var(--app-text)] outline-none transition-colors focus:ring-2 focus:ring-indigo-500 ${
                    formErrors.resolutionMinutes
                      ? 'border-red-500/50'
                      : 'border-[var(--app-border)] focus:border-indigo-500'
                  }`}
                />
                {formErrors.resolutionMinutes && (
                  <p className="mt-1.5 text-xs text-red-400">
                    {formErrors.resolutionMinutes}
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-4">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleCloseModal}
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="sm"
                disabled={
                  createMutation.isPending || updateMutation.isPending
                }
              >
                {editingPolicy ? 'Save Changes' : 'Create Policy'}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Delete Confirmation Modal */}
      {isDeleting && (
        <Modal
          title="Delete SLA Policy"
          onClose={() => setIsDeleting(null)}
        >
          <div className="space-y-5">
            <div className="flex items-start gap-3">
              <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-amber-400" />
              <div>
                <p className="text-sm font-medium text-[var(--app-text)]">
                  Are you sure you want to delete this policy?
                </p>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  This action cannot be undone. Tickets using this policy will need to be reassigned.
                </p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => setIsDeleting(null)}
                disabled={deleteMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant="danger"
                size="sm"
                onClick={handleConfirmDelete}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}

function Modal({
  title,
  onClose,
  children,
}: {
  title: string
  onClose: () => void
  children: React.ReactNode
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/60"
        onClick={onClose}
      />
      <div className="relative z-10 w-full max-w-lg rounded-xl border border-[var(--app-border)] bg-[var(--app-surface)] p-6 shadow-2xl">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-[var(--app-text)]">
            {title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-2 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]"
            aria-label="Close modal"
          >
            <XIcon className="h-5 w-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

function PriorityBadge({
  priority,
}: {
  priority: TicketPriority
}) {
  const styles: Record<TicketPriority, string> = {
    LOW: 'bg-slate-500/10 text-slate-400',
    MEDIUM: 'bg-blue-500/10 text-blue-400',
    HIGH: 'bg-amber-500/10 text-amber-400',
    URGENT: 'bg-red-500/10 text-red-400',
  }

  return (
    <span
      className={`inline-flex items-center rounded px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${styles[priority]}`}
    >
      {priority}
    </span>
  )
}

function formatDuration(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} min`
  }

  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60

  if (remainingMinutes === 0) {
    if (hours >= 24) {
      const days = hours / 24
      return `${days} ${days === 1 ? 'day' : 'days'}`
    }
    return `${hours} ${hours === 1 ? 'hour' : 'hours'}`
  }

  if (hours >= 24) {
    const days = Math.floor(hours / 24)
    const remainingHours = hours % 24
    return `${days} ${days === 1 ? 'day' : 'days'} ${remainingHours}h ${remainingMinutes}m`
  }

  return `${hours}h ${remainingMinutes}m`
}

function PencilIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
      <path d="m15 5 4 4" />
    </svg>
  )
}

function XIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d="M18 6 6 18" />
      <path d="m6 6 12 12" />
    </svg>
  )
}

export default SlaPoliciesPage
