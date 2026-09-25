import type { HTMLAttributes } from 'react'

function Skeleton({
  className = '',
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden="true"
      className={[
        'animate-pulse rounded-md bg-slate-200 dark:bg-slate-800',
        className,
      ].join(' ')}
      {...props}
    />
  )
}

export default Skeleton
