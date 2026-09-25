import {
  BarChart3,
  BookOpen,
  LayoutDashboard,
  LifeBuoy,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  Settings,
  ShieldCheck,
  Ticket,
  Users,
  X,
} from 'lucide-react'
import { useState } from 'react'
import { NavLink } from 'react-router-dom'
import OrganizationSwitcher from '../components/OrganizationSwitcher'

interface SidebarProps {
  mobileOpen: boolean
  onMobileClose: () => void
  onOrganizationChange?: (organizationId: string) => void
}

interface NavigationItem {
  label: string
  path: string
  icon: typeof LayoutDashboard
}

const workspaceItems: NavigationItem[] = [
  {
    label: 'Dashboard',
    path: '/app/dashboard',
    icon: LayoutDashboard,
  },
  {
    label: 'Tickets',
    path: '/app/tickets',
    icon: Ticket,
  },
  {
    label: 'Knowledge Base',
    path: '/app/knowledge-base',
    icon: BookOpen,
  },
  {
    label: 'Analytics',
    path: '/app/analytics',
    icon: BarChart3,
  },
]

const adminItems: NavigationItem[] = [
  {
    label: 'SLA Policies',
    path: '/app/sla-policies',
    icon: ShieldCheck,
  },
  {
    label: 'Members',
    path: '/app/members',
    icon: Users,
  },
]

function Sidebar({
  mobileOpen,
  onMobileClose,
  onOrganizationChange,
}: SidebarProps) {
  const [collapsed, setCollapsed] = useState(false)

  function renderNavigation(
    items: NavigationItem[],
    heading: string,
  ) {
    return (
      <div className="space-y-1">
        {!collapsed && (
          <p className="px-3 pb-2 pt-4 text-[10px] font-semibold uppercase tracking-widest text-[var(--app-text-subtle)]">
            {heading}
          </p>
        )}

        {items.map((item) => {
          const Icon = item.icon

          return (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={onMobileClose}
              className={({ isActive }) =>
                [
                  'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-[var(--app-primary-soft)] text-indigo-500'
                    : 'text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]',
                  collapsed ? 'justify-center' : '',
                ].join(' ')
              }
              title={collapsed ? item.label : undefined}
            >
              <Icon className="h-5 w-5 shrink-0" aria-hidden="true" />

              {!collapsed && <span>{item.label}</span>}
            </NavLink>
          )
        })}
      </div>
    )
  }

  return (
    <>
      {mobileOpen && (
        <button
          type="button"
          aria-label="Close navigation"
          className="fixed inset-0 z-40 bg-slate-950/60 lg:hidden"
          onClick={onMobileClose}
        />
      )}

      <aside
        className={[
          'fixed inset-y-0 left-0 z-50 flex flex-col border-r border-[var(--app-border)]',
          'bg-[var(--app-surface)] transition-all duration-200',
          collapsed ? 'w-20' : 'w-72',
          mobileOpen
            ? 'translate-x-0'
            : '-translate-x-full lg:translate-x-0',
        ].join(' ')}
      >
        <div
          className={[
            'flex h-16 items-center border-b border-[var(--app-border)] px-4',
            collapsed ? 'justify-center' : 'justify-between',
          ].join(' ')}
        >
          <NavLink
            to="/app/dashboard"
            onClick={onMobileClose}
            className="flex min-w-0 items-center gap-3"
          >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-lg shadow-indigo-600/20">
              <LifeBuoy className="h-5 w-5" />
            </span>

            {!collapsed && (
              <span className="truncate text-base font-semibold text-[var(--app-text)]">
                AI Helpdesk
              </span>
            )}
          </NavLink>

          <button
            type="button"
            onClick={() => setCollapsed((value) => !value)}
            className="hidden rounded-lg p-2 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)] lg:block"
            aria-label={
              collapsed
                ? 'Expand sidebar'
                : 'Collapse sidebar'
            }
          >
            {collapsed ? (
              <PanelLeftOpen className="h-4 w-4" />
            ) : (
              <PanelLeftClose className="h-4 w-4" />
            )}
          </button>

          <button
            type="button"
            onClick={onMobileClose}
            className="rounded-lg p-2 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] lg:hidden"
            aria-label="Close sidebar"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="border-b border-[var(--app-border)] p-3">
          {!collapsed && <OrganizationSwitcher onChange={onOrganizationChange} />}

          {collapsed && (
            <div className="flex justify-center">
              <OrganizationSwitcher onChange={onOrganizationChange} />
            </div>
          )}
        </div>

        <nav className="flex-1 overflow-y-auto px-3">
          {renderNavigation(workspaceItems, 'Workspace')}
          {renderNavigation(adminItems, 'Administration')}

          {!collapsed && (
            <div className="mt-4 border-t border-[var(--app-border)] pt-4">
              <NavLink
                to="/app/settings"
                onClick={onMobileClose}
                className={({ isActive }) =>
                  [
                    'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-[var(--app-primary-soft)] text-indigo-500'
                      : 'text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]',
                  ].join(' ')
                }
              >
                <Settings className="h-5 w-5" />
                Settings
              </NavLink>
            </div>
          )}
        </nav>

        {!collapsed && (
          <div className="border-t border-[var(--app-border)] px-4 py-3">
            <p className="text-xs text-[var(--app-text-subtle)]">
              AI-powered customer support
            </p>
          </div>
        )}

        <button
          type="button"
          className="absolute -right-3 top-20 hidden h-6 w-6 items-center justify-center rounded-full border border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--app-text-muted)] shadow-sm lg:flex"
          onClick={() => setCollapsed((value) => !value)}
          aria-label={
            collapsed ? 'Expand sidebar' : 'Collapse sidebar'
          }
        >
          {collapsed ? (
            <Menu className="h-3.5 w-3.5" />
          ) : (
            <PanelLeftClose className="h-3.5 w-3.5" />
          )}
        </button>
      </aside>
    </>
  )
}

export default Sidebar
