import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    setError('')
    setIsSubmitting(true)

    try {
      await login({ email, password })

      const from = location.state?.from?.pathname ?? '/app/dashboard'
      navigate(from, { replace: true })
    } catch {
      setError('Invalid email or password.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-white">
            AI Helpdesk
          </h1>
          <p className="mt-2 text-slate-400">
            Sign in to your account
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="rounded-xl border border-slate-800 bg-slate-900 p-6 shadow-xl"
        >
          <div className="space-y-5">
            <div>
              <label
                htmlFor="email"
                className="mb-2 block text-sm font-medium text-slate-200"
              >
                Email
              </label>

              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                autoComplete="email"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-white outline-none placeholder:text-slate-500 focus:border-slate-500"
                placeholder="you@example.com"
              />
            </div>

            <div>
              <label
                htmlFor="password"
                className="mb-2 block text-sm font-medium text-slate-200"
              >
                Password
              </label>

              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                autoComplete="current-password"
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-white outline-none placeholder:text-slate-500 focus:border-slate-500"
                placeholder="Enter your password"
              />
            </div>

            {error && (
              <div
                role="alert"
                className="rounded-lg border border-red-900 bg-red-950/50 px-3 py-2.5 text-sm text-red-300"
              >
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full rounded-lg bg-white px-4 py-2.5 font-medium text-slate-950 transition hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </button>
          </div>

          <p className="mt-6 text-center text-sm text-slate-400">
            Don't have an account?{' '}
            <Link
              to="/register"
              className="font-medium text-white hover:underline"
            >
              Create one
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}

export default LoginPage
