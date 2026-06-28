import type { CreateDocumentPayload, Dashboard, DocumentDetail, FiscalDocument } from './types'

class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => ({ detail: response.statusText }))
    throw new ApiError(problem.detail ?? 'Request failed', response.status)
  }
  return response.json() as Promise<T>
}

export const api = {
  listDocuments: () => request<FiscalDocument[]>('/api/v1/documents'),
  dashboard: () => request<Dashboard>('/api/v1/dashboard'),
  detail: (id: string) => request<DocumentDetail>(`/api/v1/documents/${id}`),
  create: (payload: CreateDocumentPayload, idempotencyKey: string) =>
    request<FiscalDocument>('/api/v1/documents', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify(payload),
    }),
  submit: (id: string) =>
    request<FiscalDocument>(`/api/v1/documents/${id}/submit`, { method: 'POST' }),
  retry: (id: string) =>
    request<FiscalDocument>(`/api/v1/documents/${id}/retry`, { method: 'POST' }),
}

export { ApiError }
