import { ChevronsUpDown, Check } from 'lucide-react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useOrganizations } from '../features/organizations/OrganizationContext'

interface OrganizationSwitcherProps {
  onChange?: (organizationId: string) => void
}

function OrganizationSwitcher({
  onChange,
}: OrganizationSwitcherProps) {
  const {
    organizations,
    activeOrganizationId,
    setActiveOrganizationId,
  } = useOrganizations()

  const navigate = useNavigate()
  const location = useLocation()
  const [open, setOpen] = useState(false)

  const activeOrganization =
    organizations.find(
      (organization) => organization.id === activeOrganizationId,
    ) ?? null

  function handleSelect(organizationId: string) {
    if (location.pathname.startsWith('/app/tickets/')) {
      navigate('/app/tickets', { replace: true })
      onChange?.(organizationId)
      setOpen(false)
      return
    }

    setActiveOrganizationId(organizationId)
    onChange?.(organizationId)
    setOpen(false)
  }

  return (
    <div className="relative min-w-0">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="flex w-full items-center gap-2 rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2 text-left transition-colors hover:border-[var(--app-border-strong)]"
        aria-expanded={open}
        aria-haspopup="listbox"
      >
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-indigo-500/15 text-sm font-semibold text-indigo-400">
          {activeOrganization?.name.charAt(0).toUpperCase() ?? 'O'}
        </span>

        <span className="min-w-0 flex-1">
          <span className="block truncate text-sm font-medium text-[var(--app-text)]">
            {activeOrganization?.name ?? 'Select organization'}
          </span>

          {activeOrganization && (
            <span className="block truncate text-xs text-[var(--app-text-muted)]">
              {activeOrganization.role}
            </span>
          )}
        </span>

        <ChevronsUpDown
          className="h-4 w-4 shrink-0 text-[var(--app-text-muted)]"
          aria-hidden="true"
        />
      </button>

      {open && (
        <>
          <button
            type="button"
            aria-label="Close organization menu"
            className="fixed inset-0 z-30 cursor-default"
            onClick={() => setOpen(false)}
          />

          <div
            className="absolute left-0 top-full z-40 mt-2 w-72 overflow-hidden rounded-xl border border-[var(--app-border)] bg-[var(--app-surface)] p-1 shadow-xl"
            role="listbox"
          >
            <div className="px-3 py-2 text-xs font-semibold uppercase tracking-wider text-[var(--app-text-subtle)]">
              Organizations
            </div>

            {organizations.map((organization) => {
              const selected =
                organization.id === activeOrganizationId

              return (
                <button
                  key={organization.id}
                  type="button"
                  role="option"
                  aria-selected={selected}
                  onClick={() => handleSelect(organization.id)}
                  className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left hover:bg-[var(--app-surface-muted)]"
                >
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-indigo-500/15 text-sm font-semibold text-indigo-400">
                    {organization.name.charAt(0).toUpperCase()}
                  </span>

                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium text-[var(--app-text)]">
                      {organization.name}
                    </span>
                    <span className="block text-xs text-[var(--app-text-muted)]">
                      {organization.role}
                    </span>
                  </span>

                  {selected && (
                    <Check
                      className="h-4 w-4 text-indigo-500"
                      aria-hidden="true"
                    />
                  )}
                </button>
              )
            })}

            {organizations.length === 0 && (
              <p className="px-3 py-4 text-sm text-[var(--app-text-muted)]">
                No organizations available.
              </p>
            )}
          </div>
        </>
      )}
    </div>
  )
}

export default OrganizationSwitcher
