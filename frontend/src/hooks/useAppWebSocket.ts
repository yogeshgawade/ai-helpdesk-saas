import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import type { Ticket, TicketMessage } from '../features/tickets/types'
import type { Notification } from '../api/notifications'
import { apiClient } from '../api/client'

interface WebSocketEvent {
  type: string
  organizationId: string
  data?: Ticket | TicketMessage | Notification
}

const WS_BASE_URL =
  import.meta.env.VITE_WS_BASE_URL ?? 'ws://localhost:8080'

export function useAppWebSocket(
  organizationId: string | null,
) {
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!organizationId) {
      return
    }

    let socket: WebSocket | null = null
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null
    let stopped = false

    async function connect() {
      if (stopped) {
        return
      }

      try {
        const response = await apiClient.post<{ ticket: string }>(
          '/api/auth/websocket-ticket',
        )

        if (stopped) {
          return
        }

        const ticket = response.data.ticket

        socket = new WebSocket(
          `${WS_BASE_URL}/ws?ticket=${encodeURIComponent(ticket)}`,
        )

        socket.onopen = () => {
          socket?.send(
            JSON.stringify({
              type: 'subscribe',
              organizationId,
            }),
          )
        }

        socket.onmessage = (event) => {
          let message: WebSocketEvent

          try {
            message = JSON.parse(event.data) as WebSocketEvent
          } catch {
            return
          }

          if (message.type === 'notification.created') {
            queryClient.invalidateQueries({
              queryKey: ['notifications'],
            })

            return
          }

          if (message.organizationId !== organizationId) {
            return
          }

          if (message.type === 'ticket.updated') {
            queryClient.invalidateQueries({
              queryKey: ['ticket', organizationId],
            })

            queryClient.invalidateQueries({
              queryKey: ['tickets', organizationId],
            })

            return
          }

          if (message.type === 'message.created') {
            const data = message.data as TicketMessage | undefined

            if (!data) {
              return
            }

            queryClient.invalidateQueries({
              queryKey: [
                'ticket-messages',
                organizationId,
                data.ticketId,
              ],
            })
          }
        }

        socket.onclose = () => {
          if (stopped) {
            return
          }

          reconnectTimer = setTimeout(connect, 2000)
        }

        socket.onerror = () => {
          socket?.close()
        }
      } catch {
        if (stopped) {
          return
        }

        reconnectTimer = setTimeout(connect, 2000)
      }
    }

    connect()

    return () => {
      stopped = true

      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
      }

      socket?.close()
    }
  }, [organizationId, queryClient])
}
