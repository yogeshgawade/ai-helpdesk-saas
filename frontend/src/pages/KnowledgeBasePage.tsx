import { useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import type { ReactNode } from 'react'
import {
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  AlertCircle,
  BookOpen,
  CheckCircle2,
  Clock3,
  FileText,
  Search,
  Sparkles,
  Trash2,
  UploadCloud,
  X,
} from 'lucide-react'
import {
  deleteKnowledgeBaseDocument,
  getKnowledgeBaseDocuments,
  uploadKnowledgeBaseDocument,
  generateKnowledgeBaseAnswer,
  type KnowledgeBaseDocument,
} from '../api/knowledgeBase'
import { useOrganizations } from '../features/organizations/OrganizationContext'
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Input,
  PageHeader,
  Select,
  Skeleton,
  Spinner,
  Textarea,
} from '../components/ui'

const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024

type StatusFilter =
  | 'ALL'
  | KnowledgeBaseDocument['status']

function KnowledgeBasePage() {
  const { activeOrganizationId, activeOrganization } =
    useOrganizations()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  const [title, setTitle] = useState('')
  const [sourceType, setSourceType] = useState('FILE')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] =
    useState<StatusFilter>('ALL')
  const [ragQuery, setRagQuery] = useState('')
  const [ragAnswer, setRagAnswer] = useState<{
    answer: string
    citations: Array<{
      document_id: string
      document_title: string
      chunk_id: string
      chunk_index: number
    }>
  } | null>(null)
  const [ragError, setRagError] = useState<string | null>(null)

  const documentsQuery = useQuery({
    queryKey: [
      'knowledge-base-documents',
      activeOrganizationId,
    ],
    queryFn: () =>
      getKnowledgeBaseDocuments(activeOrganizationId!),
    enabled: activeOrganizationId !== null,
  })

  const uploadMutation = useMutation({
    mutationFn: () => {
      if (!activeOrganizationId || !selectedFile) {
        throw new Error('Organization and file are required.')
      }

      return uploadKnowledgeBaseDocument(
        activeOrganizationId,
        title.trim(),
        sourceType,
        selectedFile,
      )
    },
    onSuccess: async () => {
      resetUploadForm()

      await queryClient.invalidateQueries({
        queryKey: [
          'knowledge-base-documents',
          activeOrganizationId,
        ],
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
        throw new Error('Organization is required.')
      }

      return deleteKnowledgeBaseDocument(
        activeOrganizationId,
        documentId,
      )
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: [
          'knowledge-base-documents',
          activeOrganizationId,
        ],
      })
    },
  })

  const documents = useMemo(
    () => documentsQuery.data ?? [],
    [documentsQuery.data],
  )

  const filteredDocuments = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase()

    return documents.filter((document) => {
      const matchesSearch =
        !normalizedSearch ||
        document.title.toLowerCase().includes(normalizedSearch) ||
        document.sourceType.toLowerCase().includes(normalizedSearch)

      const matchesStatus =
        statusFilter === 'ALL' ||
        document.status === statusFilter

      return matchesSearch && matchesStatus
    })
  }, [documents, search, statusFilter])

  const documentCounts = useMemo(
    () => ({
      total: documents.length,
      ready: documents.filter(
        (document) => document.status === 'READY',
      ).length,
      processing: documents.filter(
        (document) => document.status === 'PROCESSING',
      ).length,
      failed: documents.filter(
        (document) => document.status === 'FAILED',
      ).length,
    }),
    [documents],
  )

  function resetUploadForm() {
    setTitle('')
    setSelectedFile(null)
    setUploadError(null)

    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  function handleFileChange(
    event: ChangeEvent<HTMLInputElement>,
  ) {
    const file = event.target.files?.[0] ?? null

    setSelectedFile(file)
    setUploadError(null)

    if (file && !title.trim()) {
      setTitle(file.name.replace(/\.[^/.]+$/, ''))
    }
  }

  function validateUpload() {
    if (!title.trim()) {
      return 'Enter a document title.'
    }

    if (!selectedFile) {
      return 'Choose a file to upload.'
    }

    if (selectedFile.size > MAX_FILE_SIZE_BYTES) {
      return 'The file must be smaller than 20 MB.'
    }

    return null
  }

  function handleUpload() {
    const validationError = validateUpload()

    if (validationError) {
      setUploadError(validationError)
      return
    }

    uploadMutation.mutate()
  }

  function handleDelete(document: KnowledgeBaseDocument) {
    const confirmed = window.confirm(
      `Delete "${document.title}" from the knowledge base?`,
    )

    if (confirmed) {
      deleteMutation.mutate(document.id)
    }
  }

  function handleGenerateAnswer() {
    const query = ragQuery.trim()

    if (!activeOrganizationId || !query) {
      return
    }

    setRagAnswer(null)
    setRagError(null)

    generateKnowledgeBaseAnswer(
      activeOrganizationId,
      query,
    )
      .then((result) => {
        setRagAnswer(result)
      })
      .catch((error: unknown) => {
        setRagError(
          error instanceof Error
            ? error.message
            : 'Failed to generate an answer.',
        )
      })
  }

  if (!activeOrganizationId) {
    return (
      <EmptyState
        title="Select an organization"
        description="Choose an organization from the sidebar to manage its knowledge base."
        icon={<BookOpen className="h-6 w-6" />}
      />
    )
  }

  return (
    <div>
      <PageHeader
        eyebrow={activeOrganization?.name}
        title="Knowledge Base"
        description="Manage the documents your AI assistant uses to answer support questions."
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          label="Total documents"
          value={documentCounts.total}
          icon={<FileText className="h-5 w-5" />}
          tone="neutral"
        />
        <SummaryCard
          label="Ready"
          value={documentCounts.ready}
          icon={<CheckCircle2 className="h-5 w-5" />}
          tone="success"
        />
        <SummaryCard
          label="Processing"
          value={documentCounts.processing}
          icon={<Clock3 className="h-5 w-5" />}
          tone="warning"
        />
        <SummaryCard
          label="Failed"
          value={documentCounts.failed}
          icon={<AlertCircle className="h-5 w-5" />}
          tone="danger"
        />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="min-w-0 space-y-6">
          <Card className="p-5 sm:p-6">
            <div className="flex items-start gap-3">
              <div className="rounded-lg bg-indigo-500/15 p-2.5 text-indigo-500">
                <UploadCloud className="h-5 w-5" />
              </div>

              <div>
                <h2 className="font-semibold text-[var(--app-text)]">
                  Upload a document
                </h2>
                <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                  Add policies, guides, and product documentation for grounded AI answers.
                </p>
              </div>
            </div>

            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              <Input
                label="Document title"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="Refund policy"
                disabled={uploadMutation.isPending}
              />

              <Select
                label="Source type"
                value={sourceType}
                onChange={(event) => setSourceType(event.target.value)}
                disabled={uploadMutation.isPending}
              >
                <option value="FILE">File upload</option>
              </Select>
            </div>

            <div className="mt-4 rounded-xl border border-dashed border-[var(--app-border-strong)] bg-[var(--app-surface-muted)] p-5">
              <label className="flex cursor-pointer flex-col items-center justify-center text-center">
                <UploadCloud className="h-8 w-8 text-indigo-500" />

                <span className="mt-3 text-sm font-medium text-[var(--app-text)]">
                  Choose a knowledge-base file
                </span>

                <span className="mt-1 text-xs text-[var(--app-text-muted)]">
                  Maximum file size: 20 MB
                </span>

                <input
                  ref={fileInputRef}
                  type="file"
                  onChange={handleFileChange}
                  disabled={uploadMutation.isPending}
                  className="sr-only"
                />
              </label>

              {selectedFile && (
                <div className="mt-4 flex items-center justify-between gap-3 rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2.5">
                  <div className="flex min-w-0 items-center gap-2">
                    <FileText className="h-4 w-4 shrink-0 text-indigo-500" />
                    <span className="truncate text-sm text-[var(--app-text)]">
                      {selectedFile.name}
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      setSelectedFile(null)
                      if (fileInputRef.current) {
                        fileInputRef.current.value = ''
                      }
                    }}
                    className="rounded-md p-1 text-[var(--app-text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--app-text)]"
                    aria-label="Remove selected file"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </div>
              )}
            </div>

            {uploadError && (
              <p className="mt-3 text-sm text-red-500" role="alert">
                {uploadError}
              </p>
            )}

            <div className="mt-4 flex justify-end">
              <Button
                onClick={handleUpload}
                loading={uploadMutation.isPending}
                disabled={!selectedFile || !title.trim()}
                icon={<UploadCloud className="h-4 w-4" />}
              >
                Upload document
              </Button>
            </div>
          </Card>

          <Card className="overflow-hidden">
            <div className="border-b border-[var(--app-border)] p-5">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div>
                  <h2 className="font-semibold text-[var(--app-text)]">
                    Documents
                  </h2>
                  <p className="mt-1 text-sm text-[var(--app-text-muted)]">
                    {documents.length} document
                    {documents.length === 1 ? '' : 's'} in this workspace.
                  </p>
                </div>

                <div className="flex flex-col gap-2 sm:flex-row">
                  <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--app-text-subtle)]" />
                    <input
                      value={search}
                      onChange={(event) => setSearch(event.target.value)}
                      placeholder="Search documents"
                      className="h-10 w-full rounded-lg border border-[var(--app-border)] bg-[var(--app-surface-muted)] pl-9 pr-3 text-sm text-[var(--app-text)] outline-none focus:border-indigo-400 sm:w-52"
                    />
                  </div>

                  <Select
                    value={statusFilter}
                    onChange={(event) =>
                      setStatusFilter(
                        event.target.value as StatusFilter,
                      )
                    }
                    aria-label="Filter documents by status"
                  >
                    <option value="ALL">All statuses</option>
                    <option value="READY">Ready</option>
                    <option value="PROCESSING">Processing</option>
                    <option value="FAILED">Failed</option>
                  </Select>
                </div>
              </div>
            </div>

            {documentsQuery.isLoading && (
              <DocumentListSkeleton />
            )}

            {documentsQuery.isError && (
              <div className="p-5">
                <ErrorState
                  title="Documents could not be loaded"
                  description="Check your connection and try again."
                  action={
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => documentsQuery.refetch()}
                    >
                      Try again
                    </Button>
                  }
                />
              </div>
            )}

            {!documentsQuery.isLoading &&
              !documentsQuery.isError &&
              filteredDocuments.length === 0 && (
                <div className="p-5">
                  <EmptyState
                    title={
                      documents.length === 0
                        ? 'No documents yet'
                        : 'No matching documents'
                    }
                    description={
                      documents.length === 0
                        ? 'Upload your first document to give the AI assistant trusted context.'
                        : 'Try a different search or status filter.'
                    }
                    icon={<FileText className="h-6 w-6" />}
                  />
                </div>
              )}

            {!documentsQuery.isLoading &&
              !documentsQuery.isError &&
              filteredDocuments.length > 0 && (
                <div className="divide-y divide-[var(--app-border)]">
                  {filteredDocuments.map((document) => (
                    <DocumentRow
                      key={document.id}
                      document={document}
                      isDeleting={
                        deleteMutation.isPending &&
                        deleteMutation.variables === document.id
                      }
                      onDelete={() => handleDelete(document)}
                    />
                  ))}
                </div>
              )}
          </Card>
        </div>

        <KnowledgeBaseAnswerPanel
          query={ragQuery}
          answer={ragAnswer}
          error={ragError}
          onQueryChange={setRagQuery}
          onGenerate={handleGenerateAnswer}
          isGenerating={false}
        />
      </div>
    </div>
  )
}

interface SummaryCardProps {
  label: string
  value: number
  icon: ReactNode
  tone: 'neutral' | 'success' | 'warning' | 'danger'
}

function SummaryCard({
  label,
  value,
  icon,
  tone,
}: SummaryCardProps) {
  return (
    <Card className="p-4">
      <div className="flex items-center justify-between">
        <span className="text-sm text-[var(--app-text-muted)]">
          {label}
        </span>
        <span
          className={[
            'rounded-lg p-2',
            tone === 'success'
              ? 'bg-emerald-500/15 text-emerald-500'
              : tone === 'warning'
                ? 'bg-amber-500/15 text-amber-500'
                : tone === 'danger'
                  ? 'bg-red-500/15 text-red-500'
                  : 'bg-indigo-500/15 text-indigo-500',
          ].join(' ')}
        >
          {icon}
        </span>
      </div>

      <p className="mt-3 text-2xl font-semibold text-[var(--app-text)]">
        {value}
      </p>
    </Card>
  )
}

interface DocumentRowProps {
  document: KnowledgeBaseDocument
  isDeleting: boolean
  onDelete: () => void
}

function DocumentRow({
  document,
  isDeleting,
  onDelete,
}: DocumentRowProps) {
  return (
    <div className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 items-start gap-3">
        <div className="rounded-lg bg-indigo-500/15 p-2 text-indigo-500">
          <FileText className="h-5 w-5" />
        </div>

        <div className="min-w-0">
          <h3 className="truncate font-medium text-[var(--app-text)]">
            {document.title}
          </h3>

          <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-[var(--app-text-muted)]">
            <span>{document.sourceType}</span>
            <span>•</span>
            <span>{formatDate(document.createdAt)}</span>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 sm:justify-end">
        <DocumentStatusBadge status={document.status} />

        <Button
          variant="ghost"
          size="sm"
          onClick={onDelete}
          disabled={isDeleting}
          icon={
            isDeleting ? (
              <Spinner size="sm" />
            ) : (
              <Trash2 className="h-4 w-4" />
            )
          }
        >
          <span className="sr-only sm:not-sr-only">Delete</span>
        </Button>
      </div>
    </div>
  )
}

function DocumentStatusBadge({
  status,
}: {
  status: KnowledgeBaseDocument['status']
}) {
  if (status === 'READY') {
    return (
      <Badge tone="success" dot>
        Ready
      </Badge>
    )
  }

  if (status === 'FAILED') {
    return (
      <Badge tone="danger" dot>
        Failed
      </Badge>
    )
  }

  return (
    <Badge tone="warning" dot>
      Processing
    </Badge>
  )
}

interface KnowledgeBaseAnswerPanelProps {
  query: string
  answer: {
    answer: string
    citations: Array<{
      document_id: string
      document_title: string
      chunk_id: string
      chunk_index: number
    }>
  } | null
  error: string | null
  onQueryChange: (value: string) => void
  onGenerate: () => void
  isGenerating: boolean
}

function KnowledgeBaseAnswerPanel({
  query,
  answer,
  error,
  onQueryChange,
  onGenerate,
  isGenerating,
}: KnowledgeBaseAnswerPanelProps) {
  return (
    <Card className="h-fit overflow-hidden xl:sticky xl:top-24">
      <div className="border-b border-[var(--app-border)] bg-indigo-500/5 p-5">
        <div className="flex items-start gap-3">
          <div className="rounded-lg bg-indigo-500/15 p-2.5 text-indigo-500">
            <Sparkles className="h-5 w-5" />
          </div>

          <div>
            <h2 className="font-semibold text-[var(--app-text)]">
              Ask your knowledge base
            </h2>
            <p className="mt-1 text-sm text-[var(--app-text-muted)]">
              Generate an answer grounded in your uploaded documents.
            </p>
          </div>
        </div>
      </div>

      <div className="p-5">
        <Textarea
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="Ask about a refund, account process, or product policy..."
          rows={5}
          disabled={isGenerating}
        />

        <div className="mt-3 flex justify-end">
          <Button
            size="sm"
            onClick={onGenerate}
            disabled={isGenerating || !query.trim()}
            loading={isGenerating}
            icon={<Sparkles className="h-4 w-4" />}
          >
            Generate answer
          </Button>
        </div>

        {error && (
          <div className="mt-4 flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-500">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            {error}
          </div>
        )}

        {answer && (
          <div className="mt-5 rounded-lg border border-indigo-500/20 bg-indigo-500/5 p-4">
            <p className="whitespace-pre-wrap text-sm leading-6 text-[var(--app-text)]">
              {answer.answer}
            </p>

            {answer.citations.length > 0 && (
              <div className="mt-5 border-t border-[var(--app-border)] pt-4">
                <p className="text-xs font-semibold uppercase tracking-wide text-[var(--app-text-subtle)]">
                  Sources
                </p>

                <div className="mt-3 space-y-2">
                  {answer.citations.map((citation) => (
                    <div
                      key={`${citation.chunk_id}-${citation.chunk_index}`}
                      className="rounded-lg border border-[var(--app-border)] bg-[var(--app-surface)] p-3"
                    >
                      <p className="truncate text-sm font-medium text-[var(--app-text)]">
                        {citation.document_title}
                      </p>
                      <p className="mt-1 text-xs text-[var(--app-text-muted)]">
                        Chunk {citation.chunk_index + 1}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </Card>
  )
}

function DocumentListSkeleton() {
  return (
    <div className="divide-y divide-[var(--app-border)]">
      {Array.from({ length: 4 }).map((_, index) => (
        <div
          key={index}
          className="flex items-center gap-3 p-5"
        >
          <Skeleton className="h-10 w-10 rounded-lg" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-3 w-32" />
          </div>
          <Skeleton className="h-6 w-20" />
        </div>
      ))}
    </div>
  )
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString()
}

export default KnowledgeBasePage
