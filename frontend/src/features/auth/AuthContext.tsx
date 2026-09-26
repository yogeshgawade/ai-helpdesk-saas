import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { login as loginApi, register as registerApi } from '../../api/auth'
import type { LoginRequest, RegisterRequest } from '../../types/auth'
import { queryClient } from '../../lib/query-client'

interface User {
  id: string
  email: string
  name: string
}

interface AuthContextValue {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  login: (request: LoginRequest) => Promise<void>
  register: (request: RegisterRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

const TOKEN_KEY = 'auth_token'
const USER_KEY = 'auth_user'
const ACTIVE_ORGANIZATION_KEY = 'active_organization_id'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem(TOKEN_KEY),
  )

  const [user, setUser] = useState<User | null>(() => {
    const storedUser = localStorage.getItem(USER_KEY)

    if (!storedUser) {
      return null
    }

    try {
      return JSON.parse(storedUser) as User
    } catch {
      localStorage.removeItem(USER_KEY)
      return null
    }
  })

  useEffect(() => {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }, [token])

  useEffect(() => {
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }, [user])

  async function login(request: LoginRequest) {
    const response = await loginApi(request)

    queryClient.clear()
    localStorage.removeItem(ACTIVE_ORGANIZATION_KEY)
    setToken(response.accessToken)
    setUser(response.user)
  }

  async function register(request: RegisterRequest) {
    const response = await registerApi(request)

    queryClient.clear()
    localStorage.removeItem(ACTIVE_ORGANIZATION_KEY)
    setToken(response.accessToken)
    setUser(response.user)
  }

  function logout() {
    queryClient.clear()
    localStorage.removeItem(ACTIVE_ORGANIZATION_KEY)
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: token !== null,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}
