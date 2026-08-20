const currencyFormatter = new Intl.NumberFormat('es-PE', {
  style: 'currency',
  currency: 'PEN',
  minimumFractionDigits: 2,
});

const compactFormatter = new Intl.NumberFormat('es-PE', {
  notation: 'compact',
  maximumFractionDigits: 1,
});

const integerFormatter = new Intl.NumberFormat('es-PE');

export function formatCurrency(value) {
  return currencyFormatter.format(value);
}

export function formatCompact(value) {
  return compactFormatter.format(value);
}

export function formatInteger(value) {
  return integerFormatter.format(value);
}

export function formatDateLong(isoDate) {
  return new Intl.DateTimeFormat('es-PE', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date(isoDate));
}

export function formatDateTime(isoDate) {
  if (!isoDate) return '—';
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(isoDate));
}
