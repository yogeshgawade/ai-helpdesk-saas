import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Lock, Mail } from 'lucide-react'
import { useAuth } from '../features/auth/AuthContext'
import { Button, Card } from '../components/ui'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  function validateForm(): boolean {
    const errors: Record<string, string> = {}

    if (!email.trim()) {
      errors.email = 'Email is required'
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Please enter a valid email address'
    }

    if (!password) {
      errors.password = 'Password is required'
    } else if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setError('')
    setFieldErrors({})

    if (!validateForm()) {
      return
    }

    setIsSubmitting(true)

    try {
      await login({ email, password })

      const from = location.state?.from?.pathname ?? '/app/dashboard'
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid email or password')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--app-bg)] px-4 py-12 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md p-8">
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-600">
            <Lock className="h-6 w-6 text-white" />
          </div>
          <h1 className="mt-4 text-2xl font-bold text-[var(--app-text)]">
            Welcome back
          </h1>
          <p className="mt-2 text-sm text-[var(--app-text-muted)]">
            Sign in to your account to continue
          </p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-5">
          {error && (
            <div className="rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-400" role="alert">
              {error}
            </div>
          )}

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-[var(--app-text)]">
              Email address
            </label>
            <div className="mt-1.5 relative">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                <Mail className="h-5 w-5 text-[var(--app-text-muted)]" />
              </div>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  if (fieldErrors.email) {
                    setFieldErrors({ ...fieldErrors, email: '' })
                  }
                }}
                className={`block w-full rounded-lg border bg-[var(--app-surface)] py-2.5 pl-10 pr-3 text-sm text-[var(--app-text)] placeholder-[var(--app-text-muted)] outline-none transition-colors focus:ring-2 focus:ring-indigo-500 ${
                  fieldErrors.email
                    ? 'border-red-500/50 focus:border-red-500'
                    : 'border-[var(--app-border)] focus:border-indigo-500'
                }`}
                placeholder="you@example.com"
              />
            </div>
            {fieldErrors.email && (
              <p className="mt-1.5 text-xs text-red-400">{fieldErrors.email}</p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-[var(--app-text)]">
              Password
            </label>
            <div className="mt-1.5 relative">
              <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                <Lock className="h-5 w-5 text-[var(--app-text-muted)]" />
              </div>
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  if (fieldErrors.password) {
                    setFieldErrors({ ...fieldErrors, password: '' })
                  }
                }}
                className={`block w-full rounded-lg border bg-[var(--app-surface)] py-2.5 pl-10 pr-10 text-sm text-[var(--app-text)] placeholder-[var(--app-text-muted)] outline-none transition-colors focus:ring-2 focus:ring-indigo-500 ${
                  fieldErrors.password
                    ? 'border-red-500/50 focus:border-red-500'
                    : 'border-[var(--app-border)] focus:border-indigo-500'
                }`}
                placeholder="••••••••"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute inset-y-0 right-0 flex items-center pr-3 text-[var(--app-text-muted)] hover:text-[var(--app-text)]"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? (
                  <EyeOff className="h-5 w-5" />
                ) : (
                  <Eye className="h-5 w-5" />
                )}
              </button>
            </div>
            {fieldErrors.password && (
              <p className="mt-1.5 text-xs text-red-400">{fieldErrors.password}</p>
            )}
          </div>

          <div>
            <Button
              type="submit"
              variant="primary"
              size="md"
              className="w-full"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </Button>
          </div>

          <div className="text-center">
            <p className="text-sm text-[var(--app-text-muted)]">
              Don't have an account?{' '}
              <Link
                to="/register"
                className="font-medium text-indigo-500 hover:text-indigo-400"
              >
                Sign up
              </Link>
            </p>
          </div>
        </form>
      </Card>
    </div>
  )
}

export default LoginPage
