import axios, { type InternalAxiosRequestConfig } from 'axios'

const TOKEN_KEY = 'auth_token'
const USER_KEY = 'auth_user'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

const refreshClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  withCredentials: true,
})

interface RetriableRequest extends InternalAxiosRequestConfig {
  _retry?: boolean
}

let refreshInFlight: Promise<string> | null = null

export async function refreshAccessToken(): Promise<string> {
  if (!refreshInFlight) {
    refreshInFlight = refreshClient
      .post<{ accessToken: string }>('/api/auth/refresh')
      .then(({ data }) => {
        localStorage.setItem(TOKEN_KEY, data.accessToken)
        window.dispatchEvent(
          new CustomEvent('auth:token-refreshed', { detail: data.accessToken }),
        )
        return data.accessToken
      })
      .finally(() => {
        refreshInFlight = null
      })
  }

  return refreshInFlight
}

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config as RetriableRequest | undefined
    const isAuthEndpoint = [
      '/api/auth/login',
      '/api/auth/register',
      '/api/auth/refresh',
      '/api/auth/logout',
    ].some((path) => config?.url?.startsWith(path))

    if (error.response?.status === 401 && config && !config._retry && !isAuthEndpoint) {
      config._retry = true

      try {
        const token = await refreshAccessToken()
        config.headers.Authorization = `Bearer ${token}`
        return await apiClient.request(config)
      } catch {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    }

    return Promise.reject(error)
  },
)
