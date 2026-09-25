import { apiClient } from './client'
import type { TicketMessage } from '../features/tickets/types'

export interface ResponseAssistantCitation {
  document_id: string
  document_title: string
  chunk_id: string
  chunk_index: number
}

export interface ResponseAssistantComplete {
  generationId: string
  model: string
  citations: ResponseAssistantCitation[]
}

interface SseEvent {
  event: string
  data: string
}

function parseSseEvent(block: string): SseEvent | null {
  const lines = block.split('\n')

  let event = 'message'
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart())
    }
  }

  if (dataLines.length === 0) {
    return null
  }

  return {
    event,
    data: dataLines.join('\n'),
  }
}

export async function streamResponseAssistant(
  organizationId: string,
  ticketId: string,
  onChunk: (chunk: string) => void,
  onComplete: (result: ResponseAssistantComplete) => void,
): Promise<void> {
  const token = localStorage.getItem('auth_token')

  if (!token) {
    throw new Error('Authentication token not found.')
  }

  const baseUrl =
    import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

  const response = await fetch(
    `${baseUrl}/api/orgs/${organizationId}/tickets/${ticketId}/ai/stream-response`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream',
      },
    },
  )

  if (!response.ok) {
    let message = `Failed to generate AI response (${response.status}).`

    try {
      const errorBody = await response.text()

      if (errorBody.trim()) {
        message = errorBody
      }
    } catch {
      // Keep the default error message.
    }

    throw new Error(message)
  }

  if (!response.body) {
    throw new Error('AI response stream is not available.')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()

  let buffer = ''

  try {
    while (true) {
      const { value, done } = await reader.read()

      if (done) {
        break
      }

      buffer += decoder.decode(value, { stream: true })

      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() ?? ''

      for (const block of blocks) {
        const event = parseSseEvent(block)

        if (!event) {
          continue
        }

        if (event.event === 'chunk') {
          onChunk(event.data)
          continue
        }

        if (event.event === 'done') {
          const result =
            JSON.parse(event.data) as ResponseAssistantComplete

          onComplete(result)
          continue
        }

        if (event.event === 'error') {
          throw new Error(event.data || 'AI streaming failed.')
        }
      }
    }

    if (buffer.trim()) {
      const event = parseSseEvent(buffer)

      if (event?.event === 'chunk') {
        onChunk(event.data)
      } else if (event?.event === 'done') {
        const result =
          JSON.parse(event.data) as ResponseAssistantComplete

        onComplete(result)
      } else if (event?.event === 'error') {
        throw new Error(event.data || 'AI streaming failed.')
      }
    }
  } finally {
    reader.releaseLock()
  }
}

export async function approveResponseAssistant(
  organizationId: string,
  ticketId: string,
  generationId: string,
  body: string,
): Promise<TicketMessage> {
  const response = await apiClient.post<TicketMessage>(
    `/api/orgs/${organizationId}/tickets/${ticketId}/ai/approve-response`,
    {
      generationId,
      body,
    },
  )

  return response.data
}
