import type { ReactNode } from 'react'

interface EmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
  icon?: ReactNode
}

function EmptyState({
  title,
  description,
  action,
  icon,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-[var(--app-border-strong)] bg-[var(--app-surface)] px-6 py-12 text-center">
      {icon && (
        <div className="mb-4 rounded-full bg-[var(--app-primary-soft)] p-3 text-[var(--app-primary)]">
          {icon}
        </div>
      )}

      <h2 className="text-base font-semibold text-[var(--app-text)]">
        {title}
      </h2>

      {description && (
        <p className="mt-2 max-w-md text-sm text-[var(--app-text-muted)]">
          {description}
        </p>
      )}

      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export default EmptyState
