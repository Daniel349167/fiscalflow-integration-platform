<script setup lang="ts">
import type { DocumentStatus } from '../types'

defineProps<{ status: DocumentStatus }>()

function statusLabel(status: DocumentStatus): string {
  return {
    DRAFT: 'Borrador',
    QUEUED: 'En cola',
    PROCESSING: 'Procesando',
    RETRY_PENDING: 'Reintento',
    ACCEPTED: 'Aceptado',
    REJECTED: 'Rechazado',
    DEAD_LETTER: 'Dead letter',
  }[status]
}
</script>

<template>
  <span class="status-badge" :class="`status-${status.toLowerCase()}`">
    <i aria-hidden="true"></i>{{ statusLabel(status) }}
  </span>
</template>
