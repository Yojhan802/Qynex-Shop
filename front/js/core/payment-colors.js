const PAYMENT_COLORS = {
  EFECTIVO: 'var(--chart-series-1)',
  YAPE: 'var(--chart-series-3)',
  TARJETA: 'var(--chart-series-4)',
  PLIN: 'var(--chart-series-7)',
  TRANSFERENCIA: 'var(--chart-series-5)',
};
const FALLBACK_COLORS = ['var(--chart-series-2)', 'var(--chart-series-6)', 'var(--chart-series-8)'];

/** Color categórico validado (skill dataviz) para un método de pago, por nombre. */
export function colorForPaymentMethod(label, index) {
  return PAYMENT_COLORS[label.toUpperCase()] || FALLBACK_COLORS[index % FALLBACK_COLORS.length];
}
