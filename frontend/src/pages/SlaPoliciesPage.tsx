import { useEffect, useState } from 'react'
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
    event: React.FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault()

    if (!activeOrganizationId) {
      return
    }

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
          activeOrganizationId,
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
          activeOrganizationId,
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
    if (!activeOrganizationId) {
      return
    }

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
        activeOrganizationId,
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

  const existingPriorities = new Set(
    policies
      .filter((policy) => policy.id !== editingPolicy?.id)
      .map((policy) => policy.priority),
  )

  if (!activeOrganizationId) {
    return (
      <div className="p-6">
        <h1 className="text-2xl font-semibold">
          SLA Policies
        </h1>

        <p className="mt-4 text-slate-400">
          Select an organization to manage SLA policies.
        </p>
      </div>
    )
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">
            SLA Policies
          </h1>

          <p className="mt-1 text-sm text-slate-400">
            Configure response and resolution targets for{' '}
            {activeOrganization?.name ?? 'this organization'}.
          </p>
        </div>

        <button
          type="button"
          onClick={openCreateForm}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500"
        >
          Add SLA Policy
        </button>
      </div>

      {error && (
        <div className="mt-6 rounded-lg border border-red-900 bg-red-950/40 px-4 py-3 text-sm text-red-300">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="mt-8 text-slate-400">
          Loading SLA policies...
        </div>
      ) : policies.length === 0 ? (
        <div className="mt-8 rounded-xl border border-slate-800 bg-slate-900 p-8 text-center">
          <h2 className="text-lg font-medium">
            No SLA policies configured
          </h2>

          <p className="mt-2 text-sm text-slate-400">
            Create policies to automatically assign response
            and resolution deadlines to tickets.
          </p>

          <button
            type="button"
            onClick={openCreateForm}
            className="mt-5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500"
          >
            Create your first policy
          </button>
        </div>
      ) : (
        <div className="mt-8 overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-slate-800 bg-slate-950">
              <tr>
                <th className="px-5 py-4 font-medium text-slate-300">
                  Priority
                </th>
                <th className="px-5 py-4 font-medium text-slate-300">
                  Policy
                </th>
                <th className="px-5 py-4 font-medium text-slate-300">
                  First Response
                </th>
                <th className="px-5 py-4 font-medium text-slate-300">
                  Resolution
                </th>
                <th className="px-5 py-4 text-right font-medium text-slate-300">
                  Actions
                </th>
              </tr>
            </thead>

            <tbody>
              {policies.map((policy) => (
                <tr
                  key={policy.id}
                  className="border-b border-slate-800 last:border-b-0"
                >
                  <td className="px-5 py-4">
                    <span className="rounded-full bg-slate-800 px-3 py-1 text-xs font-medium">
                      {policy.priority}
                    </span>
                  </td>

                  <td className="px-5 py-4 font-medium">
                    {policy.name}
                  </td>

                  <td className="px-5 py-4 text-slate-300">
                    {formatDuration(
                      policy.firstResponseMinutes,
                    )}
                  </td>

                  <td className="px-5 py-4 text-slate-300">
                    {formatDuration(
                      policy.resolutionMinutes,
                    )}
                  </td>

                  <td className="px-5 py-4">
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        onClick={() =>
                          openEditForm(policy)
                        }
                        className="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-300 hover:bg-slate-800"
                      >
                        Edit
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          handleDelete(policy)
                        }
                        disabled={
                          deletingPolicyId === policy.id
                        }
                        className="rounded-lg border border-red-900 px-3 py-1.5 text-xs text-red-300 hover:bg-red-950/50 disabled:opacity-50"
                      >
                        {deletingPolicyId === policy.id
                          ? 'Deleting...'
                          : 'Delete'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isFormOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-lg rounded-xl border border-slate-700 bg-slate-900 p-6 shadow-xl">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold">
                  {editingPolicy
                    ? 'Edit SLA Policy'
                    : 'Create SLA Policy'}
                </h2>

                <p className="mt-1 text-sm text-slate-400">
                  Set the response and resolution targets.
                </p>
              </div>

              <button
                type="button"
                onClick={closeForm}
                disabled={isSaving}
                className="text-xl text-slate-400 hover:text-white"
              >
                ×
              </button>
            </div>

            <form
              onSubmit={handleSubmit}
              className="mt-6 space-y-5"
            >
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-300">
                  Policy name
                </label>

                <input
                  value={form.name}
                  onChange={(event) =>
                    updateForm(
                      'name',
                      event.target.value,
                    )
                  }
                  placeholder="Standard Support"
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium text-slate-300">
                  Priority
                </label>

                <select
                  value={form.priority}
                  onChange={(event) =>
                    updateForm(
                      'priority',
                      event.target.value as TicketPriority,
                    )
                  }
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
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
                      {priority}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="mb-2 block text-sm font-medium text-slate-300">
                    First response (minutes)
                  </label>

                  <input
                    type="number"
                    min="1"
                    value={form.firstResponseMinutes}
                    onChange={(event) =>
                      updateForm(
                        'firstResponseMinutes',
                        Number(event.target.value),
                      )
                    }
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="mb-2 block text-sm font-medium text-slate-300">
                    Resolution (minutes)
                  </label>

                  <input
                    type="number"
                    min="1"
                    value={form.resolutionMinutes}
                    onChange={(event) =>
                      updateForm(
                        'resolutionMinutes',
                        Number(event.target.value),
                      )
                    }
                    className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 border-t border-slate-800 pt-5">
                <button
                  type="button"
                  onClick={closeForm}
                  disabled={isSaving}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-300 hover:bg-slate-800"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={isSaving}
                  className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:opacity-50"
                >
                  {isSaving
                    ? 'Saving...'
                    : editingPolicy
                      ? 'Save Changes'
                      : 'Create Policy'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default SlaPoliciesPage
