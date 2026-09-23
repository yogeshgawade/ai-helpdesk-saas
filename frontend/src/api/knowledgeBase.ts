import { apiClient } from './client'

export interface KnowledgeBaseDocument {
  id: string
  organizationId: string
  title: string
  sourceType: string
  s3Key: string
  contentHash: string
  status: 'PROCESSING' | 'READY' | 'FAILED'
  createdAt: string
}

export async function getKnowledgeBaseDocuments(
  organizationId: string,
): Promise<KnowledgeBaseDocument[]> {
  const response = await apiClient.get<KnowledgeBaseDocument[]>(
    `/api/orgs/${organizationId}/kb/documents`,
  )

  return response.data
}

export async function uploadKnowledgeBaseDocument(
  organizationId: string,
  title: string,
  sourceType: string,
  file: File,
): Promise<KnowledgeBaseDocument> {
  const formData = new FormData()

  formData.append('title', title)
  formData.append('sourceType', sourceType)
  formData.append('file', file)

  const response = await apiClient.post<KnowledgeBaseDocument>(
    `/api/orgs/${organizationId}/kb/documents`,
    formData,
    {
      headers: {
        'Content-Type': undefined,
      },
    },
  )

  return response.data
}

export async function deleteKnowledgeBaseDocument(
  organizationId: string,
  documentId: string,
): Promise<void> {
  await apiClient.delete(
    `/api/orgs/${organizationId}/kb/documents/${documentId}`,
  )
}

export interface RagCitation {
  document_id: string
  document_title: string
  chunk_id: string
  chunk_index: number
}

export interface RagResponse {
  answer: string
  citations: RagCitation[]
}

export async function generateKnowledgeBaseAnswer(
  organizationId: string,
  query: string,
  limit = 5,
): Promise<RagResponse> {
  const response = await apiClient.post<RagResponse>(
    `/api/orgs/${organizationId}/kb/rag`,
    {
      query,
      limit,
    },
  )

  return response.data
}
