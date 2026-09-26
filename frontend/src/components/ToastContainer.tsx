import { X } from 'lucide-react'
import { useToast, type ToastType } from '../features/toast/ToastContext'

const toastStyles: Record<ToastType, string> = {
  success: 'border-emerald-500/20 bg-emerald-500/10',
  error: 'border-red-500/20 bg-red-500/10',
  info: 'border-blue-500/20 bg-blue-500/10',
  warning: 'border-amber-500/20 bg-amber-500/10',
}

const iconColors: Record<ToastType, string> = {
  success: 'text-emerald-400',
  error: 'text-red-400',
  info: 'text-blue-400',
  warning: 'text-amber-400',
}

export function ToastContainer() {
  const { toasts, dismissToast } = useToast()

  if (toasts.length === 0) {
    return null
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`flex min-w-[320px] max-w-md items-start gap-3 rounded-lg border p-4 shadow-lg ${toastStyles[toast.type]}`}
        >
          <div className={`mt-0.5 h-2 w-2 shrink-0 rounded-full ${iconColors[toast.type]} bg-current`} />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-[var(--app-text)]">{toast.title}</p>
            {toast.message && (
              <p className="mt-1 text-sm text-[var(--app-text-muted)]">{toast.message}</p>
            )}
          </div>
          <button
            type="button"
            onClick={() => dismissToast(toast.id)}
            className="shrink-0 rounded p-1 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]"
            aria-label="Dismiss"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ))}
    </div>
  )
}
