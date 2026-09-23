import { useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  deleteKnowledgeBaseDocument,
  getKnowledgeBaseDocuments,
  uploadKnowledgeBaseDocument,
} from '../api/knowledgeBase'
import { useOrganizations } from '../features/organizations/OrganizationContext'

function KnowledgeBasePage() {
  const { activeOrganizationId } = useOrganizations()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const [title, setTitle] = useState('')
  const [sourceType, setSourceType] = useState('FILE')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [uploadError, setUploadError] = useState<string | null>(null)

  const documentsQuery = useQuery({
    queryKey: ['knowledge-base-documents', activeOrganizationId],
    queryFn: () =>
      getKnowledgeBaseDocuments(activeOrganizationId!),
    enabled: activeOrganizationId !== null,
  })

  const uploadMutation = useMutation({
    mutationFn: () => {
      if (!activeOrganizationId || !selectedFile) {
        throw new Error('Organization and file are required')
      }

      return uploadKnowledgeBaseDocument(
        activeOrganizationId,
        title.trim(),
        sourceType,
        selectedFile,
      )
    },
    onSuccess: async () => {
      setTitle('')
      setSelectedFile(null)
      setUploadError(null)

      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }

      await queryClient.invalidateQueries({
        queryKey: ['knowledge-base-documents', activeOrganizationId],
      })
    },
    onError: (error) => {
      setUploadError(
        error instanceof Error
          ? error.message
          : 'Failed to upload document.',
      )
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (documentId: string) => {
      if (!activeOrganizationId) {
        throw new Error('Organization is required')
      }

      return deleteKnowledgeBaseDocument(
        activeOrganizationId,
        documentId,
      )
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['knowledge-base-documents', activeOrganizationId],
      })
    },
  })

  function handleFileChange(
    event: React.ChangeEvent<HTMLInputElement>,
  ) {
    const file = event.target.files?.[0] ?? null

    setSelectedFile(file)

    if (file && !title.trim()) {
      setTitle(file.name.replace(/\.[^/.]+$/, ''))
    }
  }

  function handleUpload() {
    setUploadError(null)

    if (!title.trim()) {
      setUploadError('Please enter a document title.')
      return
    }

    if (!selectedFile) {
      setUploadError('Please select a file.')
      return
    }

    uploadMutation.mutate()
  }

  function formatDate(value: string) {
    return new Date(value).toLocaleString()
  }

  function statusClass(status: string) {
    if (status === 'READY') {
      return 'text-emerald-400'
    }

    if (status === 'FAILED') {
      return 'text-red-400'
    }

    return 'text-amber-400'
  }

  if (documentsQuery.isLoading) {
    return <div className="p-6">Loading knowledge base...</div>
  }

  if (documentsQuery.isError) {
    return (
      <div className="p-6 text-red-400">
        Failed to load knowledge base documents.
      </div>
    )
  }

  const documents = documentsQuery.data ?? []

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold">Knowledge Base</h1>

      <p className="mt-2 text-slate-400">
        Upload documents that the AI can use when answering
        customer questions.
      </p>

      <div className="mt-6 rounded-xl border border-slate-800 bg-slate-900 p-5">
        <h2 className="text-lg font-medium">Upload document</h2>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label className="text-sm">
            <span className="mb-2 block text-slate-400">
              Title
            </span>

            <input
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Refund Policy"
              disabled={uploadMutation.isPending}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
            />
          </label>

          <label className="text-sm">
            <span className="mb-2 block text-slate-400">
              Source type
            </span>

            <select
              value={sourceType}
              onChange={(event) => setSourceType(event.target.value)}
              disabled={uploadMutation.isPending}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-blue-500 disabled:opacity-50"
            >
              <option value="FILE">File</option>
            </select>
          </label>
        </div>

        <label className="mt-4 block text-sm">
          <span className="mb-2 block text-slate-400">
            File
          </span>

          <input
            ref={fileInputRef}
            type="file"
            onChange={handleFileChange}
            disabled={uploadMutation.isPending}
            className="block w-full text-sm text-slate-400 file:mr-4 file:rounded-lg file:border-0 file:bg-slate-800 file:px-4 file:py-2 file:text-sm file:text-white hover:file:bg-slate-700 disabled:opacity-50"
          />
        </label>

        {selectedFile && (
          <p className="mt-3 text-sm text-slate-400">
            Selected: {selectedFile.name}
          </p>
        )}

        {uploadError && (
          <p className="mt-3 text-sm text-red-400">
            {uploadError}
          </p>
        )}

        {uploadMutation.isSuccess && (
          <p className="mt-3 text-sm text-emerald-400">
            Document uploaded successfully.
          </p>
        )}

        <button
          type="button"
          onClick={handleUpload}
          disabled={
            uploadMutation.isPending ||
            !selectedFile ||
            !title.trim()
          }
          className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {uploadMutation.isPending
            ? 'Uploading...'
            : 'Upload document'}
        </button>
      </div>

      <div className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-medium">Documents</h2>

          <span className="text-sm text-slate-400">
            {documents.length} document
            {documents.length === 1 ? '' : 's'}
          </span>
        </div>

        {documents.length === 0 ? (
          <div className="mt-4 rounded-xl border border-dashed border-slate-700 p-8 text-center text-slate-400">
            No documents have been uploaded yet.
          </div>
        ) : (
          <div className="mt-4 space-y-3">
            {documents.map((document) => (
              <div
                key={document.id}
                className="rounded-xl border border-slate-800 bg-slate-900 p-4"
              >
                <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                  <div className="min-w-0">
                    <h3 className="truncate font-medium text-white">
                      {document.title}
                    </h3>

                    <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-400">
                      <span>
                        Source: {document.sourceType}
                      </span>

                      <span>
                        Created: {formatDate(document.createdAt)}
                      </span>

                      <span className={statusClass(document.status)}>
                        Status: {document.status}
                      </span>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      deleteMutation.mutate(document.id)
                    }
                    disabled={
                      deleteMutation.isPending &&
                      deleteMutation.variables === document.id
                    }
                    className="shrink-0 rounded-lg border border-red-900 px-3 py-2 text-sm text-red-400 hover:bg-red-950 disabled:opacity-50"
                  >
                    {deleteMutation.isPending &&
                    deleteMutation.variables === document.id
                      ? 'Deleting...'
                      : 'Delete'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default KnowledgeBasePage
