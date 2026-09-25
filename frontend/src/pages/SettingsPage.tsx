import {
  Bell,
  Building2,
  Moon,
  ShieldCheck,
  Sun,
} from 'lucide-react'
import { useState } from 'react'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import { useTheme } from '../features/theme/ThemeContext'
import { Badge, Button, Card, PageHeader } from '../components/ui'

function SettingsPage() {
  const { activeOrganization } = useOrganizations()
  const { theme, toggleTheme } = useTheme()
  const [notificationsEnabled, setNotificationsEnabled] =
    useState(true)
  const [saved, setSaved] = useState(false)

  function handleSave() {
    setSaved(true)

    window.setTimeout(() => {
      setSaved(false)
    }, 2500)
  }

  return (
    <div>
      <PageHeader
        eyebrow="Workspace"
        title="Settings"
        description="Manage your workspace preferences and account experience."
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
        <div className="space-y-6">
          <Card className="p-5 sm:p-6">
            <div className="flex items-start gap-4">
              <div className="rounded-lg bg-indigo-500/15 p-2.5 text-indigo-500">
                <Building2 className="h-5 w-5" />
              </div>

              <div>
                <h2 className="font-semibold text-[var(--app-text)]">
                  Workspace
                </h2>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  Current organization information.
                </p>
              </div>
            </div>

            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              <div className="rounded-lg bg-[var(--app-surface-muted)] p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-[var(--app-text-subtle)]">
                  Organization
                </p>
                <p className="mt-2 font-medium text-[var(--app-text)]">
                  {activeOrganization?.name ?? 'No organization selected'}
                </p>
              </div>

              <div className="rounded-lg bg-[var(--app-surface-muted)] p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-[var(--app-text-subtle)]">
                  Your role
                </p>
                <div className="mt-2">
                  <Badge tone="purple">
                    {activeOrganization?.role ?? 'Unknown'}
                  </Badge>
                </div>
              </div>
            </div>
          </Card>

          <Card className="p-5 sm:p-6">
            <div className="flex items-start gap-4">
              <div className="rounded-lg bg-indigo-500/15 p-2.5 text-indigo-500">
                {theme === 'dark' ? (
                  <Moon className="h-5 w-5" />
                ) : (
                  <Sun className="h-5 w-5" />
                )}
              </div>

              <div>
                <h2 className="font-semibold text-[var(--app-text)]">
                  Appearance
                </h2>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  Choose how AI Helpdesk appears on your devices.
                </p>
              </div>
            </div>

            <div className="mt-6 flex items-center justify-between gap-4 rounded-lg border border-[var(--app-border)] p-4">
              <div>
                <p className="text-sm font-medium text-[var(--app-text)]">
                  Color theme
                </p>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  Currently using {theme} mode.
                </p>
              </div>

              <Button
                variant="secondary"
                size="sm"
                onClick={toggleTheme}
                icon={
                  theme === 'dark' ? (
                    <Sun className="h-4 w-4" />
                  ) : (
                    <Moon className="h-4 w-4" />
                  )
                }
              >
                Switch to {theme === 'dark' ? 'light' : 'dark'}
              </Button>
            </div>
          </Card>

          <Card className="p-5 sm:p-6">
            <div className="flex items-start gap-4">
              <div className="rounded-lg bg-indigo-500/15 p-2.5 text-indigo-500">
                <Bell className="h-5 w-5" />
              </div>

              <div>
                <h2 className="font-semibold text-[var(--app-text)]">
                  Notifications
                </h2>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  Control how notification updates are shown.
                </p>
              </div>
            </div>

            <label className="mt-6 flex cursor-pointer items-center justify-between gap-4 rounded-lg border border-[var(--app-border)] p-4">
              <div>
                <p className="text-sm font-medium text-[var(--app-text)]">
                  In-app notifications
                </p>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  Show ticket assignments and SLA alerts.
                </p>
              </div>

              <input
                type="checkbox"
                checked={notificationsEnabled}
                onChange={(event) =>
                  setNotificationsEnabled(event.target.checked)
                }
                className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
              />
            </label>
          </Card>

          <div className="flex items-center justify-end gap-3">
            {saved && (
              <p className="text-sm text-emerald-500" role="status">
                Settings saved.
              </p>
            )}

            <Button onClick={handleSave}>Save preferences</Button>
          </div>
        </div>

        <Card className="h-fit p-5 sm:p-6">
          <div className="flex items-start gap-3">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-emerald-500" />

            <div>
              <h2 className="font-semibold text-[var(--app-text)]">
                Workspace security
              </h2>
              <p className="mt-2 text-sm leading-6 text-[var(--app-text-muted)]">
                Your workspace is protected by organization-level access
                controls. Permissions are enforced by the backend for every
                request.
              </p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  )
}

export default SettingsPage
