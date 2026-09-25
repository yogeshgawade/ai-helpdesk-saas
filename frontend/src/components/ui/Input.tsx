import type { InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  hint?: string
  error?: string
}

function Input({
  label,
  hint,
  error,
  id,
  className = '',
  ...props
}: InputProps) {
  const inputId = id ?? props.name

  return (
    <div className="space-y-1.5">
      {label && (
        <label
          htmlFor={inputId}
          className="block text-sm font-medium text-[var(--app-text)]"
        >
          {label}
        </label>
      )}

      <input
        id={inputId}
        className={[
          'w-full rounded-lg border bg-[var(--app-surface)] px-3 py-2.5',
          'text-sm text-[var(--app-text)] shadow-sm transition-colors',
          'border-[var(--app-border)] placeholder:text-[var(--app-text-subtle)]',
          'focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-400/20',
          error
            ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
            : '',
          className,
        ].join(' ')}
        {...props}
      />

      {error && (
        <p className="text-xs text-red-500" role="alert">
          {error}
        </p>
      )}

      {!error && hint && (
        <p className="text-xs text-[var(--app-text-muted)]">{hint}</p>
      )}
    </div>
  )
}

export default Input
