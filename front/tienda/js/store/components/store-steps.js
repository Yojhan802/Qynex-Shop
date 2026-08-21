const PASOS = ['Carrito', 'Datos y pago'];

/** Indicador de pasos del flujo de compra — `activo` es 1-based (1 = Carrito, 2 = Datos y pago). */
export function renderStoreSteps(container, activo) {
  if (!container) return;
  container.innerHTML = PASOS.map(
    (label, index) => `
      ${index > 0 ? '<span class="store-step-sep"></span>' : ''}
      <span class="store-step" data-active="${index + 1 === activo}">
        <span class="store-step-dot">${index + 1}</span>
        <span>${label}</span>
      </span>
    `
  ).join('');
}
