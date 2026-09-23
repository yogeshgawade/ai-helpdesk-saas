import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  getNotifications,
  markNotificationAsRead,
  type Notification,
} from '../api/notifications'
import { useOrganizations } from '../features/organizations/OrganizationContext'

function getNotificationText(notification: Notification): string {
  const subject =
    typeof notification.payload.ticketSubject === 'string'
      ? notification.payload.ticketSubject
      : 'a ticket'

  if (notification.type === 'TICKET_ASSIGNED') {
    return `Ticket assigned to you: ${subject}`
  }

  if (notification.type === 'SLA_FIRST_RESPONSE_BREACHED') {
    return `First response SLA breached: ${subject}`
  }

  if (notification.type === 'SLA_RESOLUTION_BREACHED') {
    return `Resolution SLA breached: ${subject}`
  }

  return 'You have a new notification'
}

function getTicketId(notification: Notification): string | null {
  const ticketId = notification.payload.ticketId

  return typeof ticketId === 'string' ? ticketId : null
}

function getOrganizationId(notification: Notification): string | null {
  const organizationId = notification.payload.organizationId

  return typeof organizationId === 'string'
    ? organizationId
    : null
}

function formatNotificationTime(createdAt: string): string {
  return new Date(createdAt).toLocaleString()
}

export function NotificationBell() {
  const [open, setOpen] = useState(false)

  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { activeOrganizationId } = useOrganizations()

  const notificationsQuery = useQuery({
    queryKey: ['notifications'],
    queryFn: () => getNotifications(0, 20),
    staleTime: 30_000,
  })

  const markAsReadMutation = useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['notifications'],
      })
    },
  })

  const notifications = notificationsQuery.data?.content ?? []

  const unreadCount = notifications.filter(
    (notification) => notification.readAt === null,
  ).length

  function handleNotificationClick(notification: Notification) {
    if (notification.readAt === null) {
      markAsReadMutation.mutate(notification.id)
    }

    const ticketId = getTicketId(notification)
    const organizationId = getOrganizationId(notification)

    if (!ticketId || !organizationId) {
      return
    }

    setOpen(false)

    if (organizationId === activeOrganizationId) {
      navigate(`/app/tickets/${ticketId}`)
      return
    }

    navigate('/app/tickets', {
      state: {
        targetTicketId: ticketId,
        targetOrganizationId: organizationId,
      },
    })
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="relative rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-white hover:bg-slate-700"
        aria-label="Notifications"
      >
        🔔

        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex min-h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-xs font-semibold text-white">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-96 overflow-hidden rounded-xl border border-slate-700 bg-slate-900 shadow-xl">
          <div className="flex items-center justify-between border-b border-slate-700 px-4 py-3">
            <h2 className="font-semibold text-white">Notifications</h2>

            {unreadCount > 0 && (
              <span className="text-xs text-slate-400">
                {unreadCount} unread
              </span>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {notificationsQuery.isLoading && (
              <div className="px-4 py-6 text-sm text-slate-400">
                Loading notifications...
              </div>
            )}

            {notificationsQuery.isError && (
              <div className="px-4 py-6 text-sm text-red-400">
                Failed to load notifications.
              </div>
            )}

            {!notificationsQuery.isLoading &&
              !notificationsQuery.isError &&
              notifications.length === 0 && (
                <div className="px-4 py-8 text-center text-sm text-slate-400">
                  No notifications yet.
                </div>
              )}

            {notifications.map((notification) => (
              <button
                key={notification.id}
                type="button"
                onClick={() => handleNotificationClick(notification)}
                className={`block w-full border-b border-slate-800 px-4 py-3 text-left hover:bg-slate-800 ${
                  notification.readAt === null
                    ? 'bg-slate-800/60'
                    : 'bg-slate-900'
                }`}
              >
                <div className="flex gap-3">
                  {notification.readAt === null && (
                    <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-blue-400" />
                  )}

                  <div className="min-w-0">
                    <p className="text-sm text-white">
                      {getNotificationText(notification)}
                    </p>

                    <p className="mt-1 text-xs text-slate-500">
                      {formatNotificationTime(notification.createdAt)}
                    </p>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
