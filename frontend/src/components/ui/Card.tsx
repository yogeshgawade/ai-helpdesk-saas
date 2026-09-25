import type { HTMLAttributes, ReactNode } from 'react'

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
  interactive?: boolean
}

function Card({
  children,
  interactive = false,
  className = '',
  ...props
}: CardProps) {
  return (
    <div
      className={[
        'rounded-xl border border-[var(--app-border)]',
        'bg-[var(--app-surface)] shadow-sm',
        interactive
          ? 'transition-shadow hover:border-[var(--app-border-strong)] hover:shadow-md'
          : '',
        className,
      ].join(' ')}
      {...props}
    >
      {children}
    </div>
  )
}

export default Card
