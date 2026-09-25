import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import { getOrganizationMembers } from '../api/organizations'
import {
  addOrganizationMember,
  searchAvailableOrganizationUsers,
  updateOrganizationMemberRole,
  type AddMemberRequest,
} from '../api/members'
import type { OrganizationRole } from '../features/organizations/types'

interface SearchUser {
  userId: string
  name: string
  email: string
}

function MembersPage() {
  const { activeOrganizationId, activeOrganization } = useOrganizations()
  const queryClient = useQueryClient()

  const [searchQuery, setSearchQuery] = useState('')
  const [selectedUser, setSelectedUser] = useState<SearchUser | null>(null)
  const [role, setRole] = useState<OrganizationRole>('AGENT')
  const [error, setError] = useState('')

  const membersQuery = useQuery({
    queryKey: ['organization-members', activeOrganizationId],
    queryFn: () => getOrganizationMembers(activeOrganizationId!),
    enabled: activeOrganizationId !== null,
  })

  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('')

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim())
    }, 300)

    return () => window.clearTimeout(timeout)
  }, [searchQuery])

  const searchQueryResult = useQuery({
    queryKey: [
      'available-organization-users',
      activeOrganizationId,
      debouncedSearchQuery,
    ],
    queryFn: () =>
      searchAvailableOrganizationUsers(
        activeOrganizationId!,
        debouncedSearchQuery,
      ),
    enabled:
      activeOrganizationId !== null &&
      debouncedSearchQuery.length >= 2 &&
      selectedUser === null,
  })

  const updateRoleMutation = useMutation({
    mutationFn: ({
      membershipId,
      role,
    }: {
      membershipId: string
      role: OrganizationRole
    }) =>
      updateOrganizationMemberRole(
        activeOrganizationId!,
        membershipId,
        role,
      ),
    onSuccess: async () => {
      setError('')

      await queryClient.invalidateQueries({
        queryKey: ['organization-members', activeOrganizationId],
      })
    },
    onError: (mutationError) => {
      setError(
        mutationError instanceof Error
          ? mutationError.message
          : 'Failed to update member role.',
      )
    },
  })

  const addMemberMutation = useMutation({
    mutationFn: () =>
      addOrganizationMember(activeOrganizationId!, {
        userId: selectedUser!.userId,
        role,
      } satisfies AddMemberRequest),
    onSuccess: async () => {
      setSearchQuery('')
      setDebouncedSearchQuery('')
      setSelectedUser(null)
      setRole('AGENT')
      setError('')

      await queryClient.invalidateQueries({
        queryKey: ['organization-members', activeOrganizationId],
      })
    },
    onError: (mutationError) => {
      setError(
        mutationError instanceof Error
          ? mutationError.message
          : 'Failed to add member.',
      )
    },
  })

  if (!activeOrganizationId) {
    return (
      <div className="p-6">
        <p className="text-slate-400">
          Select an organization to manage members.
        </p>
      </div>
    )
  }

  const canAddMembers =
    activeOrganization?.role === 'OWNER' ||
    activeOrganization?.role === 'ADMIN'

  const roleOptions: OrganizationRole[] =
    activeOrganization?.role === 'OWNER'
      ? ['ADMIN', 'AGENT', 'CUSTOMER']
      : ['AGENT', 'CUSTOMER']

  function handleSelectUser(user: SearchUser) {
    setSelectedUser(user)
    setSearchQuery('')
    setDebouncedSearchQuery('')
    setError('')
  }

  function handleClearSelection() {
    setSelectedUser(null)
    setSearchQuery('')
    setError('')
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!selectedUser) {
      setError('Select a user first.')
      return
    }

    setError('')
    addMemberMutation.mutate()
  }

  return (
    <div className="p-6">
      <div>
        <h1 className="text-2xl font-semibold">Members</h1>
        <p className="mt-1 text-sm text-slate-400">
          Manage members of {activeOrganization?.name ?? 'this organization'}.
        </p>
      </div>

      {canAddMembers && (
        <section className="mt-6 rounded-xl border border-slate-800 bg-slate-900 p-5">
          <h2 className="text-lg font-semibold">Add member</h2>
          <p className="mt-1 text-sm text-slate-400">
            Search for an existing user by name or email.
          </p>

          <form onSubmit={handleSubmit} className="mt-5">
            {!selectedUser ? (
              <div className="relative">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(event) => {
                    setSearchQuery(event.target.value)
                    setError('')
                  }}
                  placeholder="Search by name or email..."
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none placeholder:text-slate-500 focus:border-slate-500"
                />

                {searchQuery.trim().length > 0 &&
                  searchQuery.trim().length < 2 && (
                    <p className="mt-2 text-xs text-slate-500">
                      Type at least 2 characters.
                    </p>
                  )}

                {debouncedSearchQuery.length >= 2 && (
                  <div className="mt-2 overflow-hidden rounded-lg border border-slate-800 bg-slate-950">
                    {searchQueryResult.isLoading && (
                      <div className="px-4 py-3 text-sm text-slate-400">
                        Searching...
                      </div>
                    )}

                    {searchQueryResult.isError && (
                      <div className="px-4 py-3 text-sm text-red-400">
                        Failed to search users.
                      </div>
                    )}

                    {!searchQueryResult.isLoading &&
                      !searchQueryResult.isError &&
                      (searchQueryResult.data ?? []).length === 0 && (
                        <div className="px-4 py-3 text-sm text-slate-400">
                          No available users found.
                        </div>
                      )}

                    {(searchQueryResult.data ?? []).map((user) => (
                      <button
                        key={user.userId}
                        type="button"
                        onClick={() => handleSelectUser(user)}
                        className="block w-full border-b border-slate-800 px-4 py-3 text-left last:border-b-0 hover:bg-slate-900"
                      >
                        <p className="font-medium text-white">{user.name}</p>
                        <p className="mt-1 text-sm text-slate-400">
                          {user.email}
                        </p>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center justify-between rounded-lg border border-slate-700 bg-slate-950 px-4 py-3">
                <div>
                  <p className="font-medium text-white">
                    {selectedUser.name}
                  </p>
                  <p className="mt-1 text-sm text-slate-400">
                    {selectedUser.email}
                  </p>
                </div>

                <button
                  type="button"
                  onClick={handleClearSelection}
                  className="rounded-md px-3 py-1.5 text-sm text-slate-400 hover:bg-slate-800 hover:text-white"
                >
                  Change
                </button>
              </div>
            )}

            <div className="mt-4 grid gap-4 md:grid-cols-[1fr_auto]">
              <select
                value={role}
                onChange={(event) =>
                  setRole(event.target.value as OrganizationRole)
                }
                className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none focus:border-slate-500"
              >
                {roleOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>

              <button
                type="submit"
                disabled={
                  addMemberMutation.isPending ||
                  selectedUser === null
                }
                className="rounded-lg bg-white px-5 py-2.5 text-sm font-medium text-slate-950 transition hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {addMemberMutation.isPending
                  ? 'Adding...'
                  : 'Add member'}
              </button>
            </div>
          </form>

          {error && (
            <div
              role="alert"
              className="mt-4 rounded-lg border border-red-900 bg-red-950/50 px-3 py-2.5 text-sm text-red-300"
            >
              {error}
            </div>
          )}
        </section>
      )}

      <section className="mt-6 overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
        <div className="border-b border-slate-800 px-5 py-4">
          <h2 className="font-semibold">Organization members</h2>
        </div>

        {membersQuery.isLoading && (
          <div className="p-5 text-sm text-slate-400">
            Loading members...
          </div>
        )}

        {membersQuery.isError && (
          <div className="p-5 text-sm text-red-400">
            Failed to load organization members.
          </div>
        )}

        {!membersQuery.isLoading &&
          !membersQuery.isError &&
          (membersQuery.data ?? []).length === 0 && (
            <div className="p-5 text-sm text-slate-400">
              No members found.
            </div>
          )}

        {(membersQuery.data ?? []).length > 0 && (
          <div className="divide-y divide-slate-800">
            {(membersQuery.data ?? []).map((member) => (
              <div
                key={member.membershipId}
                className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="font-medium text-white">{member.name}</p>
                  <p className="mt-1 text-sm text-slate-400">
                    {member.email}
                  </p>
                </div>

                {canAddMembers ? (
                  <select
                    value={member.role}
                    disabled={
                      member.role === 'OWNER' ||
                      updateRoleMutation.isPending
                    }
                    onChange={(event) => {
                      updateRoleMutation.mutate({
                        membershipId: member.membershipId,
                        role: event.target.value as OrganizationRole,
                      })
                    }}
                    className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs font-medium text-slate-300 outline-none focus:border-slate-500 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {member.role === 'OWNER' ? (
                      <option value="OWNER">OWNER</option>
                    ) : (
                      <>
                        <option value="AGENT">AGENT</option>
                        <option value="CUSTOMER">CUSTOMER</option>
                        {activeOrganization?.role === 'OWNER' && (
                          <option value="ADMIN">ADMIN</option>
                        )}
                      </>
                    )}
                  </select>
                ) : (
                  <span className="w-fit rounded-full border border-slate-700 bg-slate-800 px-3 py-1 text-xs font-medium text-slate-300">
                    {member.role}
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export default MembersPage
