import axios from 'axios'

interface ApiErrorBody {
  message?: string
  detail?: string
  title?: string
}

export function getApiErrorMessage(
  error: unknown,
  fallback: string,
): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    return (
      error.response?.data?.message ??
      error.response?.data?.detail ??
      error.response?.data?.title ??
      fallback
    )
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}
