export type DocumentStatus =
  | 'DRAFT'
  | 'QUEUED'
  | 'PROCESSING'
  | 'RETRY_PENDING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'DEAD_LETTER'

export type Provider = 'DEMO_FAST' | 'DEMO_STRICT'
export type DocumentType = 'INVOICE' | 'RECEIPT' | 'CREDIT_NOTE'

export interface FiscalDocument {
  id: string
  externalReference: string
  documentType: DocumentType
  provider: Provider
  customerTaxId: string
  customerName: string
  totalAmount: number
  currency: string
  status: DocumentStatus
  providerReference: string | null
  statusDetail: string | null
  attemptCount: number
  nextAttemptAt: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface TimelineEvent {
  id: string
  eventType: string
  fromStatus: string | null
  toStatus: string | null
  message: string
  occurredAt: string
}

export interface DocumentDetail {
  document: FiscalDocument
  timeline: TimelineEvent[]
}

export interface Dashboard {
  total: number
  accepted: number
  rejected: number
  retryPending: number
  deadLetter: number
}

export interface CreateDocumentPayload {
  externalReference: string
  documentType: DocumentType
  provider: Provider
  customerTaxId: string
  customerName: string
  totalAmount: number
  currency: string
}
