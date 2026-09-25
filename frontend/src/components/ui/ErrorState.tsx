import { AlertCircle } from 'lucide-react'
import type { ReactNode } from 'react'

interface ErrorStateProps {
  title?: string
  description?: string
  action?: ReactNode
}

function ErrorState({
  title = 'Something went wrong',
  description = 'We could not load this content. Please try again.',
  action,
}: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-red-200 bg-red-50 px-6 py-10 text-center dark:border-red-500/30 dark:bg-red-500/10">
      <AlertCircle
        className="mb-3 h-8 w-8 text-red-500"
        aria-hidden="true"
      />

      <h2 className="text-base font-semibold text-[var(--app-text)]">
        {title}
      </h2>

      <p className="mt-2 max-w-md text-sm text-[var(--app-text-muted)]">
        {description}
      </p>

      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export default ErrorState
