import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  Mail,
  Plus,
  Search,
  ShieldCheck,
  UserPlus,
  Users,
  X,
} from 'lucide-react'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import { getOrganizationMembers } from '../api/organizations'
import {
  addOrganizationMember,
  searchAvailableOrganizationUsers,
  updateOrganizationMemberRole,
  type AddMemberRequest,
} from '../api/members'
import type {
  OrganizationMember,
  OrganizationRole,
} from '../features/organizations/types'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  PageHeader,
  Select,
  Skeleton,
} from '../components/ui'

interface SearchUser {
  userId: string
  name: string
  email: string
}

function MembersPage() {
  const { activeOrganizationId, activeOrganization } =
    useOrganizations()
  const queryClient = useQueryClient()

  const [searchQuery, setSearchQuery] = useState('')
  const [selectedUser, setSelectedUser] =
    useState<SearchUser | null>(null)
  const [role, setRole] =
    useState<OrganizationRole>('AGENT')
  const [error, setError] = useState('')

  const membersQuery = useQuery({
    queryKey: ['organization-members', activeOrganizationId],
    queryFn: () => getOrganizationMembers(activeOrganizationId!),
    enabled: activeOrganizationId !== null,
  })

  const [debouncedSearchQuery, setDebouncedSearchQuery] =
    useState('')

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
        queryKey: [
          'organization-members',
          activeOrganizationId,
        ],
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
        queryKey: [
          'organization-members',
          activeOrganizationId,
        ],
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
      <EmptyState
        title="Select an organization"
        description="Choose an organization from the sidebar to manage members."
        icon={<Users className="h-6 w-6" />}
      />
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

  const members = membersQuery.data ?? []

  return (
    <div>
      <PageHeader
        eyebrow={activeOrganization?.name}
        title="Members"
        description="Manage access, roles, and membership for your organization."
        actions={
          <Badge tone="neutral">
            <Users className="h-3.5 w-3.5" />
            {members.length} member{members.length === 1 ? '' : 's'}
          </Badge>
        }
      />

      {canAddMembers && (
        <Card className="mb-6 p-5 sm:p-6">
          <div className="flex items-start gap-3">
            <div className="rounded-lg bg-indigo-500/15 p-2.5 text-indigo-500">
              <UserPlus className="h-5 w-5" />
            </div>

            <div>
              <h2 className="font-semibold text-[var(--app-text)]">
                Add a member
              </h2>
              <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                Search for an existing user and assign their workspace role.
              </p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="mt-6">
            {!selectedUser ? (
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--app-text-subtle)]" />

                <input
                  type="text"
                  value={searchQuery}
                  onChange={(event) => {
                    setSearchQuery(event.target.value)
                    setError('')
                  }}
                  placeholder="Search by name or email..."
                  className="h-11 w-full rounded-lg border border-[var(--app-border)] bg-[var(--app-surface-muted)] pl-9 pr-3 text-sm text-[var(--app-text)] outline-none placeholder:text-[var(--app-text-subtle)] focus:border-indigo-400 focus:ring-2 focus:ring-indigo-400/20"
                  aria-label="Search available users"
                />

                {searchQuery.trim().length > 0 &&
                  searchQuery.trim().length < 2 && (
                    <p className="mt-2 text-xs text-[var(--app-text-muted)]">
                      Type at least 2 characters.
                    </p>
                  )}

                {debouncedSearchQuery.length >= 2 && (
                  <div className="mt-2 overflow-hidden rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] shadow-lg">
                    {searchQueryResult.isLoading && (
                      <div className="space-y-3 p-4">
                        <Skeleton className="h-4 w-40" />
                        <Skeleton className="h-3 w-56" />
                      </div>
                    )}

                    {searchQueryResult.isError && (
                      <div className="p-4 text-sm text-red-500">
                        Failed to search users.
                      </div>
                    )}

                    {!searchQueryResult.isLoading &&
                      !searchQueryResult.isError &&
                      (searchQueryResult.data ?? []).length === 0 && (
                        <div className="p-4 text-sm text-[var(--app-text-muted)]">
                          No available users found.
                        </div>
                      )}

                    {(searchQueryResult.data ?? []).map((user) => (
                      <button
                        key={user.userId}
                        type="button"
                        onClick={() => handleSelectUser(user)}
                        className="flex w-full items-center gap-3 border-b border-[var(--app-border)] p-4 text-left last:border-b-0 hover:bg-[var(--app-surface-muted)]"
                      >
                        <MemberAvatar name={user.name} />

                        <span className="min-w-0">
                          <span className="block truncate text-sm font-medium text-[var(--app-text)]">
                            {user.name}
                          </span>
                          <span className="mt-1 flex items-center gap-1.5 truncate text-xs text-[var(--app-text-muted)]">
                            <Mail className="h-3 w-3" />
                            {user.email}
                          </span>
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center justify-between gap-3 rounded-xl border border-indigo-500/30 bg-indigo-500/5 p-4">
                <div className="flex min-w-0 items-center gap-3">
                  <MemberAvatar name={selectedUser.name} />

                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-[var(--app-text)]">
                      {selectedUser.name}
                    </p>
                    <p className="mt-1 truncate text-xs text-[var(--app-text-muted)]">
                      {selectedUser.email}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleClearSelection}
                  className="rounded-lg p-2 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]"
                  aria-label="Change selected user"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            )}

            <div className="mt-4 grid gap-4 sm:grid-cols-[minmax(0,1fr)_auto]">
              <Select
                label="Role"
                value={role}
                onChange={(event) =>
                  setRole(
                    event.target.value as OrganizationRole,
                  )
                }
              >
                {roleOptions.map((option) => (
                  <option key={option} value={option}>
                    {formatRole(option)}
                  </option>
                ))}
              </Select>

              <div className="flex items-end">
                <Button
                  type="submit"
                  loading={addMemberMutation.isPending}
                  disabled={selectedUser === null}
                  icon={<Plus className="h-4 w-4" />}
                >
                  Add member
                </Button>
              </div>
            </div>
          </form>

          {error && (
            <div
              role="alert"
              className="mt-4 rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-500"
            >
              {error}
            </div>
          )}
        </Card>
      )}

      <Card className="overflow-hidden">
        <div className="border-b border-[var(--app-border)] p-5 sm:p-6">
          <div className="flex items-start gap-3">
            <div className="rounded-lg bg-slate-500/15 p-2.5 text-slate-500">
              <ShieldCheck className="h-5 w-5" />
            </div>

            <div>
              <h2 className="font-semibold text-[var(--app-text)]">
                Organization members
              </h2>
              <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                Role changes take effect across the workspace.
              </p>
            </div>
          </div>
        </div>

        {membersQuery.isLoading && <MemberListSkeleton />}

        {membersQuery.isError && (
          <div className="p-5">
            <ErrorState
              title="Members could not be loaded"
              description="Check your connection and try again."
              action={
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => membersQuery.refetch()}
                >
                  Try again
                </Button>
              }
            />
          </div>
        )}

        {!membersQuery.isLoading &&
          !membersQuery.isError &&
          members.length === 0 && (
            <div className="p-5">
              <EmptyState
                title="No members yet"
                description="Add a member to begin collaborating in this workspace."
                icon={<Users className="h-5 w-5" />}
              />
            </div>
          )}

        {!membersQuery.isLoading &&
          !membersQuery.isError &&
          members.length > 0 && (
            <div className="divide-y divide-[var(--app-border)]">
              {members.map((member) => (
                <MemberRow
                  key={member.membershipId}
                  member={member}
                  canEdit={canAddMembers}
                  isUpdating={
                    updateRoleMutation.isPending &&
                    updateRoleMutation.variables?.membershipId ===
                      member.membershipId
                  }
                  canManageOwner={member.role === 'OWNER'}
                  isOwner={activeOrganization?.role === 'OWNER'}
                  onRoleChange={(nextRole) =>
                    updateRoleMutation.mutate({
                      membershipId: member.membershipId,
                      role: nextRole,
                    })
                  }
                />
              ))}
            </div>
          )}
      </Card>
    </div>
  )
}

function MemberRow({
  member,
  canEdit,
  isUpdating,
  canManageOwner,
  isOwner,
  onRoleChange,
}: {
  member: OrganizationMember
  canEdit: boolean
  isUpdating: boolean
  canManageOwner: boolean
  isOwner: boolean
  onRoleChange: (role: OrganizationRole) => void
}) {
  return (
    <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 items-center gap-3">
        <MemberAvatar name={member.name} />

        <div className="min-w-0">
          <p className="truncate text-sm font-medium text-[var(--app-text)]">
            {member.name}
          </p>
          <p className="mt-1 flex items-center gap-1.5 truncate text-xs text-[var(--app-text-muted)]">
            <Mail className="h-3 w-3" />
            {member.email}
          </p>
        </div>
      </div>

      {canEdit && !canManageOwner ? (
        <Select
          value={member.role}
          disabled={isUpdating}
          onChange={(event) =>
            onRoleChange(
              event.target.value as OrganizationRole,
            )
          }
          aria-label={`Role for ${member.name}`}
          className="w-full sm:w-40"
        >
          <option value="AGENT">Agent</option>
          <option value="CUSTOMER">Customer</option>
          {isOwner && <option value="ADMIN">Admin</option>}
        </Select>
      ) : (
        <Badge tone={member.role === 'OWNER' ? 'purple' : 'neutral'}>
          {formatRole(member.role)}
        </Badge>
      )}
    </div>
  )
}

function MemberAvatar({ name }: { name: string }) {
  return (
    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-indigo-500/15 text-sm font-semibold text-indigo-500">
      {name.charAt(0).toUpperCase()}
    </span>
  )
}

function MemberListSkeleton() {
  return (
    <div className="divide-y divide-[var(--app-border)]">
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="flex items-center gap-3 p-5">
          <Skeleton className="h-10 w-10 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-36" />
            <Skeleton className="h-3 w-52" />
          </div>
          <Skeleton className="h-9 w-28" />
        </div>
      ))}
    </div>
  )
}

function formatRole(role: OrganizationRole) {
  return role.charAt(0) + role.slice(1).toLowerCase()
}

export default MembersPage
