import { apiClient } from './client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from '../types/auth'

export async function login(
  request: LoginRequest,
): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>(
    '/api/auth/login',
    request,
  )

  return response.data
}

export async function register(
  request: RegisterRequest,
): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>(
    '/api/auth/register',
    request,
  )

  return response.data
}
