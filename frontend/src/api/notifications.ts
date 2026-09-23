import { apiClient } from './client'

export interface Notification {
  id: string
  userId: string
  type: string
  payload: Record<string, unknown>
  readAt: string | null
  createdAt: string
}

export interface NotificationPage {
  content: Notification[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export async function getNotifications(
  page = 0,
  size = 20,
): Promise<NotificationPage> {
  const response = await apiClient.get<NotificationPage>(
    '/api/notifications',
    {
      params: {
        page,
        size,
      },
    },
  )

  return response.data
}

export async function markNotificationAsRead(
  notificationId: string,
): Promise<void> {
  await apiClient.patch(
    `/api/notifications/${notificationId}/read`,
  )
}
