import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogOut, User } from 'lucide-react'
import { useAuth } from '../features/auth/AuthContext'
import { useOrganizations } from '../features/organizations/OrganizationContext'

function UserMenu() {
  const [open, setOpen] = useState(false)
  const { user, logout } = useAuth()
  const { activeOrganization } = useOrganizations()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  const initials = user?.name
    ? user.name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : 'U'

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="flex items-center gap-3 rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2 text-sm transition-colors hover:border-[var(--app-border-strong)]"
        aria-label="User menu"
      >
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-600 text-xs font-semibold text-white">
          {initials}
        </div>
        <div className="hidden min-w-0 text-left sm:block">
          <p className="truncate text-sm font-medium text-[var(--app-text)]">
            {user?.name ?? 'User'}
          </p>
          <p className="truncate text-xs text-[var(--app-text-muted)]">
            {activeOrganization?.name ?? 'No organization'}
          </p>
        </div>
      </button>

      {open && (
        <>
          <div
            className="fixed inset-0 z-40"
            onClick={() => setOpen(false)}
          />
          <div className="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-xl border border-[var(--app-border)] bg-[var(--app-surface)] shadow-xl">
            <div className="border-b border-[var(--app-border)] px-4 py-3">
              <p className="text-sm font-medium text-[var(--app-text)]">
                {user?.name ?? 'User'}
              </p>
              <p className="mt-0.5 truncate text-xs text-[var(--app-text-muted)]">
                {user?.email ?? 'user@example.com'}
              </p>
            </div>

            <div className="p-2">
              <button
                type="button"
                onClick={() => {
                  setOpen(false)
                  navigate('/app/settings')
                }}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-[var(--app-text-muted)] transition-colors hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]"
              >
                <User className="h-4 w-4" />
                Settings
              </button>

              <button
                type="button"
                onClick={handleLogout}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-red-400 transition-colors hover:bg-red-500/10 hover:text-red-300"
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

export default UserMenu
