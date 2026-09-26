import {
  Menu,
  Moon,
  Search,
  Sun,
} from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { NotificationBell } from '../components/NotificationBell'
import UserMenu from '../components/UserMenu'
import { useTheme } from '../features/theme/ThemeContext'

interface TopBarProps {
  onMenuClick: () => void
}

const pageTitles: Record<string, string> = {
  '/app/dashboard': 'Dashboard',
  '/app/tickets': 'Tickets',
  '/app/knowledge-base': 'Knowledge Base',
  '/app/sla-policies': 'SLA Policies',
  '/app/analytics': 'Analytics',
  '/app/members': 'Members',
  '/app/settings': 'Settings',
}

function TopBar({ onMenuClick }: TopBarProps) {
  const location = useLocation()
  const { theme, toggleTheme } = useTheme()
  const isTicketDetail = /^\/app\/tickets\/[^/]+$/.test(
    location.pathname,
  )

  const pageTitle =
    pageTitles[location.pathname] ??
    (isTicketDetail ? 'Ticket details' : 'AI Helpdesk')

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between border-b border-[var(--app-border)] bg-[var(--app-bg)]/90 px-4 backdrop-blur-xl sm:px-6">
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          onClick={onMenuClick}
          className="rounded-lg p-2 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)] lg:hidden"
          aria-label="Open navigation"
        >
          <Menu className="h-5 w-5" />
        </button>

        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-[var(--app-text)]">
            {pageTitle}
          </p>
          <p className="hidden text-xs text-[var(--app-text-muted)] sm:block">
            Manage your support workspace
          </p>
        </div>
      </div>

      <div className="flex items-center gap-1 sm:gap-2">
        <button
          type="button"
          className="hidden items-center gap-2 rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2 text-sm text-[var(--app-text-muted)] transition-colors hover:border-[var(--app-border-strong)] hover:text-[var(--app-text)] md:flex"
          aria-label="Search"
        >
          <Search className="h-4 w-4" />
          <span>Search</span>
          <kbd className="ml-3 rounded border border-[var(--app-border)] px-1.5 py-0.5 text-[10px]">
            /
          </kbd>
        </button>

        <button
          type="button"
          onClick={toggleTheme}
          className="rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] p-2 text-[var(--app-text-muted)] transition-colors hover:border-[var(--app-border-strong)] hover:text-[var(--app-text)]"
          aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
        >
          {theme === 'dark' ? (
            <Sun className="h-5 w-5" />
          ) : (
            <Moon className="h-5 w-5" />
          )}
        </button>

        <div className="relative">
          <NotificationBell />
        </div>

        <UserMenu />
      </div>
    </header>
  )
}

export default TopBar
