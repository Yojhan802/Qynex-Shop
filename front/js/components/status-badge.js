const LABELS = {
  ACTIVE: 'Activo',
  INACTIVE: 'Inactivo',
  BLOCKED: 'Bloqueado',
};

const CLASSES = {
  ACTIVE: 'badge-success',
  INACTIVE: 'badge-neutral',
  BLOCKED: 'badge-danger',
};

export function statusBadge(status) {
  return `<span class="badge ${CLASSES[status] || 'badge-neutral'}">${LABELS[status] || status}</span>`;
}
