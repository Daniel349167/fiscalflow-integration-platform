<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { api } from './api'
import StatusBadge from './components/StatusBadge.vue'
import type {
  CreateDocumentPayload,
  Dashboard,
  DocumentDetail,
  DocumentStatus,
  FiscalDocument,
  Provider,
} from './types'

const documents = ref<FiscalDocument[]>([])
const dashboard = ref<Dashboard>({ total: 0, accepted: 0, rejected: 0, retryPending: 0, deadLetter: 0 })
const selected = ref<DocumentDetail | null>(null)
const statusFilter = ref<'ALL' | DocumentStatus>('ALL')
const query = ref('')
const loading = ref(true)
const actionLoading = ref(false)
const error = ref('')
const showCreate = ref(false)
let poller: number | undefined

const form = ref<CreateDocumentPayload>({
  externalReference: '',
  documentType: 'INVOICE',
  provider: 'DEMO_FAST',
  customerTaxId: '20123456789',
  customerName: '',
  totalAmount: 100,
  currency: 'PEN',
})

const filteredDocuments = computed(() => {
  const normalized = query.value.trim().toLowerCase()
  return documents.value.filter((document) => {
    const matchesStatus = statusFilter.value === 'ALL' || document.status === statusFilter.value
    const matchesQuery =
      !normalized ||
      document.externalReference.toLowerCase().includes(normalized) ||
      document.customerName.toLowerCase().includes(normalized) ||
      document.customerTaxId.includes(normalized)
    return matchesStatus && matchesQuery
  })
})

const acceptanceRate = computed(() => {
  const completed = dashboard.value.accepted + dashboard.value.rejected
  return completed === 0 ? '0%' : `${Math.round((dashboard.value.accepted / completed) * 100)}%`
})

async function loadAll(silent = false) {
  if (!silent) loading.value = true
  try {
    const [nextDocuments, nextDashboard] = await Promise.all([api.listDocuments(), api.dashboard()])
    documents.value = nextDocuments
    dashboard.value = nextDashboard
    if (selected.value) {
      selected.value = await api.detail(selected.value.document.id)
    } else if (nextDocuments.length > 0) {
      selected.value = await api.detail(nextDocuments[0].id)
    }
    error.value = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo conectar con la API'
  } finally {
    loading.value = false
  }
}

async function selectDocument(id: string) {
  actionLoading.value = true
  try {
    selected.value = await api.detail(id)
    error.value = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo cargar el documento'
  } finally {
    actionLoading.value = false
  }
}

async function submitSelected() {
  if (!selected.value) return
  await runAction(() => api.submit(selected.value!.document.id))
}

async function retrySelected() {
  if (!selected.value) return
  await runAction(() => api.retry(selected.value!.document.id))
}

async function runAction(action: () => Promise<FiscalDocument>) {
  actionLoading.value = true
  try {
    const document = await action()
    selected.value = await api.detail(document.id)
    await loadAll(true)
    error.value = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'La operación no pudo completarse'
  } finally {
    actionLoading.value = false
  }
}

async function createDocument() {
  actionLoading.value = true
  try {
    const document = await api.create(form.value, crypto.randomUUID())
    showCreate.value = false
    resetForm()
    await loadAll(true)
    selected.value = await api.detail(document.id)
    error.value = ''
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'No se pudo crear el documento'
  } finally {
    actionLoading.value = false
  }
}

function resetForm() {
  form.value = {
    externalReference: '',
    documentType: 'INVOICE',
    provider: 'DEMO_FAST',
    customerTaxId: '20123456789',
    customerName: '',
    totalAmount: 100,
    currency: 'PEN',
  }
}

function formatMoney(value: number, currency: string) {
  return new Intl.NumberFormat('es-PE', { style: 'currency', currency }).format(value)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function providerLabel(provider: Provider) {
  return provider === 'DEMO_FAST' ? 'Demo Fast' : 'Demo Strict'
}

function eventLabel(event: string) {
  return event.replaceAll('_', ' ').toLowerCase()
}

onMounted(() => {
  loadAll()
  poller = window.setInterval(() => loadAll(true), 2500)
})

onUnmounted(() => window.clearInterval(poller))
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="product-mark">
        <span class="logo">FF</span>
        <div><strong>FiscalFlow</strong><small>Integration Hub</small></div>
      </div>

      <nav aria-label="Navegación del producto">
        <a class="nav-item active" href="#workspace"><span>01</span> Operación</a>
        <a class="nav-item" href="#architecture"><span>02</span> Arquitectura</a>
        <a class="nav-item" href="/swagger-ui.html" target="_blank"><span>03</span> OpenAPI ↗</a>
      </nav>

      <div class="sidebar-context">
        <p class="overline">Entorno</p>
        <strong>Sandbox local</strong>
        <p>Proveedores simulados. Ningún comprobante se envía a una entidad real.</p>
        <div class="live-indicator"><i></i> API conectada</div>
      </div>
    </aside>

    <main>
      <header class="topbar">
        <div>
          <p class="breadcrumbs">Portfolio / FiscalFlow / <strong>Operación</strong></p>
          <h1>Control de integraciones</h1>
        </div>
        <button class="primary-action" type="button" @click="showCreate = true">
          <span>+</span> Nuevo comprobante
        </button>
      </header>

      <section class="metrics" aria-label="Resumen de procesamiento">
        <article><span>Total procesados</span><strong>{{ dashboard.total }}</strong><small>últimos 100 registros</small></article>
        <article><span>Tasa de aceptación</span><strong>{{ acceptanceRate }}</strong><small>{{ dashboard.accepted }} aceptados</small></article>
        <article><span>Reintentos activos</span><strong>{{ dashboard.retryPending }}</strong><small>backoff exponencial</small></article>
        <article class="risk"><span>Dead-letter</span><strong>{{ dashboard.deadLetter }}</strong><small>requiere intervención</small></article>
      </section>

      <div v-if="error" class="error-banner" role="alert"><strong>Atención:</strong> {{ error }}</div>

      <section id="workspace" class="workspace">
        <div class="records-panel">
          <div class="panel-header">
            <div><p class="overline">Bandeja operativa</p><h2>Comprobantes</h2></div>
            <span class="record-count">{{ filteredDocuments.length }} registros</span>
          </div>

          <div class="filters">
            <label class="search-field">
              <span class="sr-only">Buscar comprobantes</span>
              <input v-model="query" type="search" placeholder="Referencia, cliente o RUC" />
            </label>
            <select v-model="statusFilter" aria-label="Filtrar por estado">
              <option value="ALL">Todos los estados</option>
              <option value="DRAFT">Borrador</option>
              <option value="QUEUED">En cola</option>
              <option value="PROCESSING">Procesando</option>
              <option value="RETRY_PENDING">Reintento</option>
              <option value="ACCEPTED">Aceptado</option>
              <option value="REJECTED">Rechazado</option>
              <option value="DEAD_LETTER">Dead letter</option>
            </select>
          </div>

          <div class="table-wrap">
            <table>
              <thead><tr><th>Referencia</th><th>Cliente</th><th>Proveedor</th><th>Importe</th><th>Estado</th></tr></thead>
              <tbody>
                <tr v-if="loading"><td colspan="5" class="empty-state">Cargando operación…</td></tr>
                <tr v-else-if="filteredDocuments.length === 0"><td colspan="5" class="empty-state">No hay registros para este filtro.</td></tr>
                <tr
                  v-for="document in filteredDocuments"
                  :key="document.id"
                  :class="{ selected: selected?.document.id === document.id }"
                >
                  <td><button type="button" @click="selectDocument(document.id)"><strong>{{ document.externalReference }}</strong><small>{{ formatDate(document.createdAt) }}</small></button></td>
                  <td><span>{{ document.customerName }}</span><small>{{ document.customerTaxId }}</small></td>
                  <td>{{ providerLabel(document.provider) }}</td>
                  <td class="amount">{{ formatMoney(document.totalAmount, document.currency) }}</td>
                  <td><StatusBadge :status="document.status" /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <aside class="inspector" aria-label="Detalle del comprobante seleccionado">
          <template v-if="selected">
            <div class="inspector-head">
              <p class="overline">Detalle de integración</p>
              <h2>{{ selected.document.externalReference }}</h2>
              <StatusBadge :status="selected.document.status" />
            </div>

            <dl class="document-facts">
              <div><dt>Proveedor</dt><dd>{{ providerLabel(selected.document.provider) }}</dd></div>
              <div><dt>Importe</dt><dd>{{ formatMoney(selected.document.totalAmount, selected.document.currency) }}</dd></div>
              <div><dt>Intentos</dt><dd>{{ selected.document.attemptCount }}</dd></div>
              <div><dt>Versión</dt><dd>v{{ selected.document.version }}</dd></div>
            </dl>

            <div class="status-detail">
              <span>Último resultado</span>
              <p>{{ selected.document.statusDetail ?? 'Sin resultado del proveedor' }}</p>
              <code v-if="selected.document.providerReference">{{ selected.document.providerReference }}</code>
            </div>

            <div class="actions-row">
              <button
                v-if="selected.document.status === 'DRAFT'"
                class="primary-action full"
                type="button"
                :disabled="actionLoading"
                @click="submitSelected"
              >Enviar al proveedor</button>
              <button
                v-if="selected.document.status === 'DEAD_LETTER'"
                class="secondary-action full"
                type="button"
                :disabled="actionLoading"
                @click="retrySelected"
              >Reintentar manualmente</button>
            </div>

            <div class="timeline">
              <div class="timeline-title"><h3>Audit timeline</h3><span>{{ selected.timeline.length }} eventos</span></div>
              <ol>
                <li v-for="event in [...selected.timeline].reverse()" :key="event.id">
                  <i aria-hidden="true"></i>
                  <div><strong>{{ eventLabel(event.eventType) }}</strong><p>{{ event.message }}</p><time>{{ formatDate(event.occurredAt) }}</time></div>
                </li>
              </ol>
            </div>
          </template>
          <div v-else class="empty-inspector">Selecciona un comprobante para revisar su trazabilidad.</div>
        </aside>
      </section>

      <section id="architecture" class="architecture-strip">
        <div><p class="overline">Flujo de consistencia</p><h2>Un commit, múltiples responsabilidades.</h2></div>
        <div class="flow" aria-label="Flujo de arquitectura">
          <span>API idempotente</span><i>→</i><span>PostgreSQL</span><i>→</i><span>Outbox</span><i>→</i><span>Provider adapter</span><i>→</i><span>Audit trail</span>
        </div>
      </section>
    </main>

    <div v-if="showCreate" class="modal-backdrop" @click.self="showCreate = false">
      <form class="modal" @submit.prevent="createDocument">
        <div class="modal-head"><div><p class="overline">Nueva operación</p><h2>Registrar comprobante</h2></div><button type="button" aria-label="Cerrar" @click="showCreate = false">×</button></div>
        <p class="modal-note">La creación es idempotente y no envía el documento hasta confirmar desde el inspector.</p>
        <div class="form-grid">
          <label><span>Referencia externa</span><input v-model="form.externalReference" required maxlength="80" placeholder="INV-2026-004" /></label>
          <label><span>Tipo</span><select v-model="form.documentType"><option value="INVOICE">Factura</option><option value="RECEIPT">Boleta</option><option value="CREDIT_NOTE">Nota de crédito</option></select></label>
          <label><span>Proveedor</span><select v-model="form.provider"><option value="DEMO_FAST">Demo Fast</option><option value="DEMO_STRICT">Demo Strict</option></select></label>
          <label><span>RUC del cliente</span><input v-model="form.customerTaxId" required pattern="\d{11}" maxlength="11" /></label>
          <label class="wide"><span>Razón social</span><input v-model="form.customerName" required maxlength="160" placeholder="Cliente Demo SAC" /></label>
          <label><span>Importe</span><input v-model.number="form.totalAmount" required type="number" min="0.01" step="0.01" /></label>
          <label><span>Moneda</span><select v-model="form.currency"><option value="PEN">PEN</option><option value="USD">USD</option></select></label>
        </div>
        <div class="modal-actions"><button type="button" class="secondary-action" @click="showCreate = false">Cancelar</button><button class="primary-action" :disabled="actionLoading">{{ actionLoading ? 'Registrando…' : 'Crear borrador' }}</button></div>
      </form>
    </div>
  </div>
</template>
