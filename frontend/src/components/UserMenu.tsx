import {
  LogOut,
  Moon,
  Sun,
} from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { useTheme } from '../features/theme/ThemeContext'

function UserMenu() {
  const { user, logout } = useAuth()
  const { theme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  function handleLogout() {
    setOpen(false)
    logout()
    navigate('/login', { replace: true })
  }

  const initials =
    user?.name
      ?.split(' ')
      .map((part) => part.charAt(0))
      .join('')
      .slice(0, 2)
      .toUpperCase() || 'U'

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="flex items-center gap-2 rounded-lg p-1.5 transition-colors hover:bg-[var(--app-surface-muted)]"
        aria-label="Open user menu"
        aria-expanded={open}
        aria-haspopup="menu"
      >
        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-indigo-600 text-xs font-semibold text-white">
          {initials}
        </span>

        <span className="hidden max-w-32 text-left lg:block">
          <span className="block truncate text-sm font-medium text-[var(--app-text)]">
            {user?.name ?? 'User'}
          </span>
          <span className="block truncate text-xs text-[var(--app-text-muted)]">
            {user?.email ?? ''}
          </span>
        </span>
      </button>

      {open && (
        <>
          <button
            type="button"
            aria-label="Close user menu"
            className="fixed inset-0 z-30 cursor-default"
            onClick={() => setOpen(false)}
          />

          <div
            className="absolute right-0 top-full z-40 mt-2 w-64 overflow-hidden rounded-xl border border-[var(--app-border)] bg-[var(--app-surface)] p-1 shadow-xl"
            role="menu"
          >
            <div className="border-b border-[var(--app-border)] px-3 py-3">
              <p className="truncate text-sm font-medium text-[var(--app-text)]">
                {user?.name ?? 'User'}
              </p>
              <p className="truncate text-xs text-[var(--app-text-muted)]">
                {user?.email ?? ''}
              </p>
            </div>

            <button
              type="button"
              role="menuitem"
              onClick={toggleTheme}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-[var(--app-text)] hover:bg-[var(--app-surface-muted)]"
            >
              {theme === 'dark' ? (
                <Sun className="h-4 w-4 text-[var(--app-text-muted)]" />
              ) : (
                <Moon className="h-4 w-4 text-[var(--app-text-muted)]" />
              )}

              {theme === 'dark'
                ? 'Switch to light mode'
                : 'Switch to dark mode'}
            </button>

            <button
              type="button"
              role="menuitem"
              onClick={handleLogout}
              className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-red-500 hover:bg-red-500/10"
            >
              <LogOut className="h-4 w-4" />
              Sign out
            </button>
          </div>
        </>
      )}
    </div>
  )
}

export default UserMenu
