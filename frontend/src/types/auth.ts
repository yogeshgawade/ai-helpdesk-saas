export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  name: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  user: {
    id: string
    email: string
    name: string
  }
}
