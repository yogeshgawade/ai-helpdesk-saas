import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
  AlertCircle,
  Clock3,
  Pencil,
  Plus,
  ShieldCheck,
  Trash2,
  X,
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
import { getApiErrorMessage } from '../lib/api-error'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  Input,
  PageHeader,
  Select,
  Skeleton,
  Spinner,
} from '../components/ui'

const PRIORITIES: TicketPriority[] = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'URGENT',
]

const EMPTY_FORM: SlaPolicyRequest = {
  name: '',
  firstResponseMinutes: 60,
  resolutionMinutes: 480,
  priority: 'MEDIUM',
}

function formatDuration(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} min`
  }

  const hours = minutes / 60

  if (hours < 24 && Number.isInteger(hours)) {
    return `${hours} hr`
  }

  if (hours < 24) {
    return `${minutes} min`
  }

  const days = hours / 24

  if (Number.isInteger(days)) {
    return `${days} day${days === 1 ? '' : 's'}`
  }

  return `${minutes} min`
}

function SlaPoliciesPage() {
  const { activeOrganizationId, activeOrganization } =
    useOrganizations()

  const [policies, setPolicies] = useState<SlaPolicy[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingPolicy, setEditingPolicy] =
    useState<SlaPolicy | null>(null)
  const [form, setForm] =
    useState<SlaPolicyRequest>(EMPTY_FORM)
  const [isSaving, setIsSaving] = useState(false)
  const [deletingPolicyId, setDeletingPolicyId] =
    useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadPolicies() {
      if (!activeOrganizationId) {
        if (!cancelled) {
          setPolicies([])
          setIsLoading(false)
        }
        return
      }

      if (!cancelled) {
        setIsLoading(true)
        setError(null)
      }

      try {
        const data = await getSlaPolicies(activeOrganizationId)

        if (!cancelled) {
          setPolicies(data)
        }
      } catch {
        if (!cancelled) {
          setError('Failed to load SLA policies.')
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void loadPolicies()

    return () => {
      cancelled = true
    }
  }, [activeOrganizationId])

  if (!activeOrganizationId) {
    return (
      <EmptyState
        title="Select an organization"
        description="Choose an organization from the sidebar to manage SLA policies."
        icon={<ShieldCheck className="h-6 w-6" />}
      />
    )
  }

  const organizationId = activeOrganizationId

  const existingPriorities = new Set(
    policies
      .filter((policy) => policy.id !== editingPolicy?.id)
      .map((policy) => policy.priority),
  )

  function openCreateForm() {
    setEditingPolicy(null)
    setForm(EMPTY_FORM)
    setIsFormOpen(true)
    setError(null)
  }

  function openEditForm(policy: SlaPolicy) {
    setEditingPolicy(policy)
    setForm({
      name: policy.name,
      firstResponseMinutes: policy.firstResponseMinutes,
      resolutionMinutes: policy.resolutionMinutes,
      priority: policy.priority,
    })
    setIsFormOpen(true)
    setError(null)
  }

  function closeForm() {
    if (isSaving) {
      return
    }

    setIsFormOpen(false)
    setEditingPolicy(null)
    setForm(EMPTY_FORM)
  }

  function updateForm(
    field: keyof SlaPolicyRequest,
    value: string | number,
  ) {
    setForm((current) => ({
      ...current,
      [field]: value,
    }))
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    if (!form.name.trim()) {
      setError('Policy name is required.')
      return
    }

    if (
      form.firstResponseMinutes <= 0 ||
      form.resolutionMinutes <= 0
    ) {
      setError('SLA durations must be greater than zero.')
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      if (editingPolicy) {
        const updated = await updateSlaPolicy(
          organizationId,
          editingPolicy.id,
          {
            ...form,
            name: form.name.trim(),
          },
        )

        setPolicies((current) =>
          current.map((policy) =>
            policy.id === updated.id ? updated : policy,
          ),
        )
      } else {
        const created = await createSlaPolicy(
          organizationId,
          {
            ...form,
            name: form.name.trim(),
          },
        )

        setPolicies((current) => [...current, created])
      }

      closeForm()
    } catch (requestError: unknown) {
      setError(
        getApiErrorMessage(
          requestError,
          'Failed to save SLA policy.',
        ),
      )
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(policy: SlaPolicy) {
    const confirmed = window.confirm(
      `Delete the "${policy.name}" SLA policy?`,
    )

    if (!confirmed) {
      return
    }

    setDeletingPolicyId(policy.id)
    setError(null)

    try {
      await deleteSlaPolicy(
        organizationId,
        policy.id,
      )

      setPolicies((current) =>
        current.filter(
          (currentPolicy) =>
            currentPolicy.id !== policy.id,
        ),
      )
    } catch (requestError: unknown) {
      setError(
        getApiErrorMessage(
          requestError,
          'Failed to delete SLA policy.',
        ),
      )
    } finally {
      setDeletingPolicyId(null)
    }
  }

  return (
    <div>
      <PageHeader
        eyebrow={activeOrganization?.name}
        title="SLA Policies"
        description="Configure response and resolution targets for each ticket priority."
        actions={
          <Button
            onClick={openCreateForm}
            icon={<Plus className="h-4 w-4" />}
          >
            Add policy
          </Button>
        }
      />

      {error && (
        <div
          role="alert"
          className="mb-6 flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-500"
        >
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          {error}
        </div>
      )}

      {isLoading ? (
        <PolicyListSkeleton />
      ) : policies.length === 0 ? (
        <Card className="p-5">
          <EmptyState
            title="No SLA policies configured"
            description="Create policies to automatically assign response and resolution deadlines to tickets."
            icon={<ShieldCheck className="h-6 w-6" />}
            action={
              <Button
                size="sm"
                onClick={openCreateForm}
                icon={<Plus className="h-4 w-4" />}
              >
                Create your first policy
              </Button>
            }
          />
        </Card>
      ) : (
        <Card className="overflow-hidden">
          <div className="border-b border-[var(--app-border)] p-5">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-indigo-500/15 p-2.5 text-indigo-500">
                <Clock3 className="h-5 w-5" />
              </div>

              <div>
                <h2 className="font-semibold text-[var(--app-text)]">
                  Active policies
                </h2>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  {policies.length} configured priority target
                  {policies.length === 1 ? '' : 's'}.
                </p>
              </div>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full min-w-[680px] text-left text-sm">
              <thead className="border-b border-[var(--app-border)] bg-[var(--app-surface-muted)]">
                <tr className="text-xs uppercase tracking-wide text-[var(--app-text-subtle)]">
                  <th className="px-5 py-3 font-semibold">
                    Priority
                  </th>
                  <th className="px-5 py-3 font-semibold">
                    Policy
                  </th>
                  <th className="px-5 py-3 font-semibold">
                    First response
                  </th>
                  <th className="px-5 py-3 font-semibold">
                    Resolution
                  </th>
                  <th className="px-5 py-3 text-right font-semibold">
                    Actions
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[var(--app-border)]">
                {policies.map((policy) => (
                  <tr
                    key={policy.id}
                    className="transition-colors hover:bg-[var(--app-surface-muted)]"
                  >
                    <td className="px-5 py-4">
                      <PriorityBadge priority={policy.priority} />
                    </td>

                    <td className="px-5 py-4 font-medium text-[var(--app-text)]">
                      {policy.name}
                    </td>

                    <td className="px-5 py-4 text-[var(--app-text-muted)]">
                      {formatDuration(policy.firstResponseMinutes)}
                    </td>

                    <td className="px-5 py-4 text-[var(--app-text-muted)]">
                      {formatDuration(policy.resolutionMinutes)}
                    </td>

                    <td className="px-5 py-4">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => openEditForm(policy)}
                          icon={<Pencil className="h-4 w-4" />}
                        >
                          <span className="sr-only sm:not-sr-only">
                            Edit
                          </span>
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(policy)}
                          disabled={
                            deletingPolicyId === policy.id
                          }
                          icon={
                            deletingPolicyId === policy.id ? (
                              <Spinner size="sm" />
                            ) : (
                              <Trash2 className="h-4 w-4 text-red-500" />
                            )
                          }
                        >
                          <span className="sr-only sm:not-sr-only">
                            Delete
                          </span>
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

      {isFormOpen && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/70 p-0 sm:items-center sm:p-4"
          role="presentation"
          onMouseDown={(event) => {
            if (event.currentTarget === event.target) {
              closeForm()
            }
          }}
        >
          <div
            className="w-full max-w-lg rounded-t-2xl border border-[var(--app-border)] bg-[var(--app-surface)] p-5 shadow-2xl sm:rounded-2xl sm:p-6"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sla-form-title"
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2
                  id="sla-form-title"
                  className="text-lg font-semibold text-[var(--app-text)]"
                >
                  {editingPolicy
                    ? 'Edit SLA policy'
                    : 'Create SLA policy'}
                </h2>

                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  Set response and resolution targets.
                </p>
              </div>

              <button
                type="button"
                onClick={closeForm}
                disabled={isSaving}
                className="rounded-lg p-2 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]"
                aria-label="Close policy form"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form
              onSubmit={handleSubmit}
              className="mt-6 space-y-5"
            >
              <Input
                label="Policy name"
                value={form.name}
                onChange={(event) =>
                  updateForm('name', event.target.value)
                }
                placeholder="Standard Support"
                disabled={isSaving}
              />

              <Select
                label="Priority"
                value={form.priority}
                onChange={(event) =>
                  updateForm(
                    'priority',
                    event.target.value as TicketPriority,
                  )
                }
                disabled={isSaving}
              >
                {PRIORITIES.map((priority) => (
                  <option
                    key={priority}
                    value={priority}
                    disabled={
                      !editingPolicy &&
                      existingPriorities.has(priority)
                    }
                  >
                    {formatPriority(priority)}
                  </option>
                ))}
              </Select>

              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="First response"
                  type="number"
                  min="1"
                  hint="Minutes"
                  value={form.firstResponseMinutes}
                  onChange={(event) =>
                    updateForm(
                      'firstResponseMinutes',
                      Number(event.target.value),
                    )
                  }
                  disabled={isSaving}
                />

                <Input
                  label="Resolution"
                  type="number"
                  min="1"
                  hint="Minutes"
                  value={form.resolutionMinutes}
                  onChange={(event) =>
                    updateForm(
                      'resolutionMinutes',
                      Number(event.target.value),
                    )
                  }
                  disabled={isSaving}
                />
              </div>

              <div className="flex flex-col-reverse gap-2 border-t border-[var(--app-border)] pt-5 sm:flex-row sm:justify-end">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={closeForm}
                  disabled={isSaving}
                >
                  Cancel
                </Button>

                <Button
                  type="submit"
                  loading={isSaving}
                >
                  {editingPolicy
                    ? 'Save changes'
                    : 'Create policy'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
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
    <Badge tone={tone} dot>
      {formatPriority(priority)}
    </Badge>
  )
}

function PolicyListSkeleton() {
  return (
    <Card className="overflow-hidden">
      <div className="divide-y divide-[var(--app-border)]">
        {Array.from({ length: 4 }).map((_, index) => (
          <div
            key={index}
            className="flex items-center gap-4 p-5"
          >
            <Skeleton className="h-6 w-20" />
            <Skeleton className="h-4 w-40 flex-1" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-4 w-24" />
          </div>
        ))}
      </div>
    </Card>
  )
}

function formatPriority(priority: TicketPriority) {
  return priority.charAt(0) + priority.slice(1).toLowerCase()
}

export default SlaPoliciesPage
