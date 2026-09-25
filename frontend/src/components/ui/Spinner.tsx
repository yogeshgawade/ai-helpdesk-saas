interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  label?: string
}

const sizeClasses = {
  sm: 'h-4 w-4 border-2',
  md: 'h-6 w-6 border-2',
  lg: 'h-9 w-9 border-[3px]',
}

function Spinner({
  size = 'md',
  label = 'Loading',
}: SpinnerProps) {
  return (
    <span
      className={[
        'inline-block animate-spin rounded-full border-indigo-500',
        'border-t-transparent',
        sizeClasses[size],
      ].join(' ')}
      role="status"
      aria-label={label}
    />
  )
}

export default Spinner
