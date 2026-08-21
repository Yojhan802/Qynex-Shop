import { requireSession } from '../core/auth.js';
import { api, ApiError } from '../core/api.js';
import { renderShell, actualizarEstadoCaja } from '../components/shell.js';
import { fetchCurrentSession } from '../core/cash-session.js';
import { openAbrirCajaModal } from '../components/abrir-caja.js';
import { createCustomerPicker } from '../components/customer-picker.js';
import { openPagoModal } from '../components/pago-modal.js';
import { openModal, closeModal } from '../components/modal.js';
import { showToast } from '../components/toast.js';
import { formatCurrency, formatDateTime } from '../core/format.js';
import { debounce } from '../core/debounce.js';
import { renderPagination } from '../components/pagination.js';
import { imprimirTicket } from '../components/ticket.js';
import { statusBadge } from '../components/status-badge.js';

const SALE_STATUS_LABELS = { COMPLETED: 'Completada', CANCELLED: 'Anulada', PARTIALLY_RETURNED: 'Parcialmente devuelta', RETURNED: 'Devuelta' };
const SALE_STATUS_CLASSES = { COMPLETED: 'badge-success', CANCELLED: 'badge-danger', PARTIALLY_RETURNED: 'badge-warning', RETURNED: 'badge-neutral' };

const session = requireSession();
let cart = [];
let cashSession = null;
let customerPicker = null;
let permissions = new Set(session?.user.permissions ?? []);
let historialPage = 0;
let devolucionesPage = 0;
let promotoresCache = [];

if (session) {
  renderShell('ventas');
  init();
}

async function init() {
  document.querySelectorAll('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      const activo = tab.dataset.tab;
      document.querySelectorAll('.tab').forEach((t) => t.setAttribute('aria-selected', String(t.dataset.tab === activo)));
      document.querySelector('#panel-nueva').hidden = activo !== 'nueva';
      document.querySelector('#panel-historial').hidden = activo !== 'historial';
      document.querySelector('#panel-devoluciones').hidden = activo !== 'devoluciones';
      document.querySelector('#panel-promotores').hidden = activo !== 'promotores';
      if (activo === 'historial') cargarHistorial();
      if (activo === 'devoluciones') cargarDevoluciones();
      if (activo === 'promotores') cargarPromotores();
    });
  });
  document.querySelector('#btn-filtrar-historial').addEventListener('click', () => {
    historialPage = 0;
    cargarHistorial();
  });
  document.querySelector('#btn-nuevo-promotor').addEventListener('click', () => abrirFormularioPromotor(null));

  const ventaId = new URLSearchParams(window.location.search).get('ventaId');
  if (ventaId) {
    document.querySelectorAll('.tab').forEach((t) => t.setAttribute('aria-selected', String(t.dataset.tab === 'historial')));
    document.querySelector('#panel-nueva').hidden = true;
    document.querySelector('#panel-historial').hidden = false;
    cargarHistorial();
    verDetalleVenta(Number(ventaId));
  }

  cashSession = await fetchCurrentSession();
  if (!cashSession) {
    document.querySelector('#pos-blocked').hidden = false;
    document.querySelector('#btn-abrir-caja-pos').addEventListener('click', () => {
      openAbrirCajaModal({
        onOpened: async (sesion) => {
          cashSession = sesion;
          document.querySelector('#pos-blocked').hidden = true;
          document.querySelector('#pos-screen').hidden = false;
          actualizarEstadoCaja();
          iniciarPantalla();
        },
      });
    });
    return;
  }
  document.querySelector('#pos-screen').hidden = false;
  iniciarPantalla();
}

function iniciarPantalla() {
  customerPicker = createCustomerPicker();
  document.querySelector('#pos-customer-mount').appendChild(customerPicker.root);

  const scanInput = document.querySelector('#pos-scan-input');
  scanInput.addEventListener('input', debounce(() => {
    const value = scanInput.value.trim();
    if (value.length >= 2) buscar(value);
  }, 300));
  scanInput.addEventListener('keydown', async (event) => {
    if (event.key !== 'Enter') return;
    event.preventDefault();
    const value = scanInput.value.trim();
    if (!value) return;
    try {
      const variante = await api.get(`/variants/barcode/${encodeURIComponent(value)}`);
      agregarAlCarrito(variante);
      scanInput.value = '';
      mostrarResultadosVacios();
    } catch {
      buscar(value);
    }
  });
  scanInput.focus();

  document.querySelector('#btn-vaciar-carrito').addEventListener('click', () => {
    cart = [];
    renderCart();
  });
  document.querySelector('#btn-cobrar').addEventListener('click', cobrar);

  renderCart();
}

async function buscar(query) {
  const resultsBox = document.querySelector('#pos-results');
  try {
    const resultados = await api.get('/variants/search', { query: { q: query } });
    if (resultados.length === 0) {
      resultsBox.innerHTML = `<div class="empty-state"><span>Sin resultados para "${query}"</span></div>`;
      return;
    }
    resultsBox.innerHTML = `
      <div class="table-scroll">
        <table class="data-table">
          <thead><tr><th>Producto</th><th>SKU</th><th>Precio</th><th>Stock</th><th></th></tr></thead>
          <tbody>
            ${resultados
              .map(
                (v) => `
              <tr class="${v.stock === 0 ? '' : 'clickable'}" data-id="${v.variantId}" style="${v.stock === 0 ? 'opacity:.5;' : ''}">
                <td class="table-cell-primary">${v.productName} <span class="table-cell-muted">${v.colorName} / ${v.sizeName}</span></td>
                <td class="mono">${v.sku}</td>
                <td class="mono">${formatCurrency(v.effectivePrice)}</td>
                <td>${v.stock}</td>
                <td>${v.stock > 0 ? '<span class="badge badge-info">Agregar</span>' : '<span class="badge badge-danger">Sin stock</span>'}</td>
              </tr>
            `
              )
              .join('')}
          </tbody>
        </table>
      </div>
    `;
    resultsBox.querySelectorAll('tr[data-id].clickable').forEach((row) => {
      const variante = resultados.find((v) => String(v.variantId) === row.dataset.id);
      if (variante) row.addEventListener('click', () => agregarAlCarrito(variante));
    });
  } catch (error) {
    resultsBox.innerHTML = `<div class="empty-state"><span>${error instanceof ApiError ? error.message : 'Error al buscar'}</span></div>`;
  }
}

function mostrarResultadosVacios() {
  document.querySelector('#pos-results').innerHTML = `
    <div class="empty-state">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>
      <span>Escanea un producto o escribe para buscar.</span>
    </div>
  `;
}

function agregarAlCarrito(variante) {
  if (variante.status !== 'ACTIVE') {
    showToast({ type: 'warning', title: 'Variante inactiva', message: 'Esta variante no está disponible para la venta.' });
    return;
  }
  const existente = cart.find((item) => item.variantId === variante.variantId);
  const enCarrito = existente?.quantity ?? 0;
  if (enCarrito + 1 > variante.stock) {
    showToast({ type: 'warning', title: 'Stock insuficiente', message: `Solo hay ${variante.stock} unidades disponibles.` });
    return;
  }
  if (existente) {
    existente.quantity += 1;
  } else {
    cart.push({
      variantId: variante.variantId,
      productName: variante.productName,
      colorName: variante.colorName,
      sizeName: variante.sizeName,
      sku: variante.sku,
      unitPrice: variante.effectivePrice,
      stock: variante.stock,
      quantity: 1,
    });
  }
  renderCart();
}

function cambiarCantidad(variantId, delta) {
  const item = cart.find((i) => i.variantId === variantId);
  if (!item) return;
  const nueva = item.quantity + delta;
  if (nueva <= 0) {
    cart = cart.filter((i) => i.variantId !== variantId);
  } else if (nueva > item.stock) {
    showToast({ type: 'warning', title: 'Stock insuficiente', message: `Solo hay ${item.stock} unidades disponibles.` });
    return;
  } else {
    item.quantity = nueva;
  }
  renderCart();
}

function quitarDelCarrito(variantId) {
  cart = cart.filter((i) => i.variantId !== variantId);
  renderCart();
}

function renderCart() {
  const container = document.querySelector('#cart-items');
  container.innerHTML = cart.length
    ? cart
        .map(
          (item) => `
      <div style="display:flex; gap:var(--space-3); padding:var(--space-3) 0; border-bottom:1px solid var(--color-border);">
        <div style="flex:1; min-width:0;">
          <div style="font-weight:600; font-size:var(--font-size-sm);">${item.productName}</div>
          <div class="table-cell-muted mono">${item.colorName} / ${item.sizeName}</div>
          <div style="display:flex; align-items:center; gap:var(--space-2); margin-top:var(--space-2);">
            <button class="btn btn-ghost btn-sm" type="button" data-qty-down="${item.variantId}" style="width:28px; padding:0;">−</button>
            <span class="mono" style="min-width:24px; text-align:center;">${item.quantity}</span>
            <button class="btn btn-ghost btn-sm" type="button" data-qty-up="${item.variantId}" style="width:28px; padding:0;">+</button>
          </div>
        </div>
        <div style="text-align:right; display:flex; flex-direction:column; align-items:flex-end; justify-content:space-between;">
          <button class="btn btn-ghost btn-sm" type="button" data-remove="${item.variantId}" aria-label="Quitar">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 6l12 12M18 6L6 18" stroke-linecap="round"/></svg>
          </button>
          <span class="mono" style="font-weight:600;">${formatCurrency(item.unitPrice * item.quantity)}</span>
        </div>
      </div>
    `
        )
        .join('')
    : `<div class="empty-state" style="padding: var(--space-8) 0;"><span>El carrito está vacío</span></div>`;

  container.querySelectorAll('[data-qty-up]').forEach((btn) => btn.addEventListener('click', () => cambiarCantidad(Number(btn.dataset.qtyUp), 1)));
  container.querySelectorAll('[data-qty-down]').forEach((btn) => btn.addEventListener('click', () => cambiarCantidad(Number(btn.dataset.qtyDown), -1)));
  container.querySelectorAll('[data-remove]').forEach((btn) => btn.addEventListener('click', () => quitarDelCarrito(Number(btn.dataset.remove))));

  const subtotal = cart.reduce((acc, item) => acc + item.unitPrice * item.quantity, 0);
  document.querySelector('#cart-subtotal').textContent = formatCurrency(subtotal);
  document.querySelector('#cart-discount').textContent = formatCurrency(0);
  document.querySelector('#cart-total').textContent = formatCurrency(subtotal);
  document.querySelector('#btn-cobrar').disabled = cart.length === 0;
}

async function cobrar() {
  const subtotal = cart.reduce((acc, item) => acc + item.unitPrice * item.quantity, 0);
  const cliente = customerPicker.getSelected();

  await openPagoModal({
    total: subtotal,
    onConfirm: async ({ payments, promoterId }) => {
      const request = {
        customerId: cliente?.id ?? null,
        promoterId,
        cashSessionId: cashSession.id,
        discountAmount: 0,
        notes: null,
        items: cart.map((item) => ({ variantId: item.variantId, quantity: item.quantity, discountAmount: 0 })),
        payments,
      };
      const venta = await api.post('/sales', request);
      mostrarTicket(venta);
      cart = [];
      customerPicker.clear();
      renderCart();
      cashSession = await fetchCurrentSession();
      actualizarEstadoCaja();
    },
  });
}

function mostrarTicket(venta) {
  const modal = openModal({
    title: '¡Venta registrada!',
    subtitle: venta.saleNumber,
    maxWidth: '380px',
    body: `
      <div style="display:flex; flex-direction:column; gap:var(--space-2); font-size:var(--font-size-sm);">
        ${venta.items
          .map(
            (item) => `
          <div style="display:flex; justify-content:space-between;">
            <span>${item.quantity} × ${item.productName} (${item.colorName}/${item.sizeName})</span>
            <span class="mono">${formatCurrency(item.subtotal)}</span>
          </div>
        `
          )
          .join('')}
        <div style="display:flex; justify-content:space-between; font-weight:700; font-size:var(--font-size-lg); margin-top:var(--space-3); padding-top:var(--space-3); border-top:1px solid var(--color-border);">
          <span>Total</span><span class="mono">${formatCurrency(venta.total)}</span>
        </div>
      </div>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-imprimir>Imprimir ticket</button>
      <button class="btn btn-dark btn-block" type="button" data-close>Nueva venta</button>
    `,
  });
  modal.footer.querySelector('[data-imprimir]').addEventListener('click', () => imprimirTicket(venta));
  modal.footer.querySelector('[data-close]').addEventListener('click', () => {
    closeModal();
    document.querySelector('#pos-scan-input').focus();
  });
  showToast({ type: 'success', title: 'Venta registrada', message: `${venta.saleNumber} · ${formatCurrency(venta.total)}` });
}

async function cargarHistorial() {
  const body = document.querySelector('#historial-body');
  try {
    const from = document.querySelector('#hist-from').value;
    const to = document.querySelector('#hist-to').value;
    const page = await api.get('/sales', {
      query: {
        status: document.querySelector('#hist-status').value || undefined,
        from: from ? `${from}T00:00:00` : undefined,
        to: to ? `${to}T23:59:59` : undefined,
        page: historialPage,
        size: 20,
        sort: 'createdAt,desc',
      },
    });

    body.innerHTML = page.content.length
      ? page.content
          .map(
            (v) => `
        <tr>
          <td class="table-cell-primary mono">${v.saleNumber}</td>
          <td class="table-cell-muted">${formatDateTime(v.createdAt)}</td>
          <td>${v.customerName ?? '—'}</td>
          <td>${v.sellerName}</td>
          <td class="mono">${formatCurrency(v.total)}</td>
          <td><span class="badge ${SALE_STATUS_CLASSES[v.status] ?? 'badge-neutral'}">${SALE_STATUS_LABELS[v.status] ?? v.status}</span></td>
          <td>
            <button class="btn btn-ghost btn-sm" type="button" data-ver="${v.id}">Ver</button>
          </td>
        </tr>
      `
          )
          .join('')
      : `<tr><td colspan="7"><div class="empty-state"><span>No se encontraron ventas.</span></div></td></tr>`;

    body.querySelectorAll('[data-ver]').forEach((btn) => {
      btn.addEventListener('click', () => verDetalleVenta(Number(btn.dataset.ver)));
    });

    renderPagination(document.querySelector('#historial-pagination'), page, (p) => {
      historialPage = p;
      cargarHistorial();
    });
  } catch (error) {
    body.innerHTML = `<tr><td colspan="7"><div class="empty-state"><span>${error instanceof ApiError ? error.message : 'Error al cargar el historial'}</span></div></td></tr>`;
  }
}

async function verDetalleVenta(saleId) {
  let venta;
  try {
    venta = await api.get(`/sales/${saleId}`);
  } catch (error) {
    showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo cargar la venta' });
    return;
  }

  const puedeAnular = venta.status === 'COMPLETED' && permissions.has('VENTAS_ANULAR');
  const puedeDevolver = (venta.status === 'COMPLETED' || venta.status === 'PARTIALLY_RETURNED') && permissions.has('VENTAS_DEVOLVER');

  const modal = openModal({
    title: venta.saleNumber,
    subtitle: `${formatDateTime(venta.createdAt)} · Vendedor: ${venta.sellerName}${venta.customerName ? ` · Cliente: ${venta.customerName}` : ''}${venta.promoterName ? ` · Promotor: ${venta.promoterName}` : ''}`,
    maxWidth: '480px',
    body: `
      <div style="display:flex; flex-direction:column; gap:var(--space-2); font-size:var(--font-size-sm);">
        ${venta.items
          .map(
            (item) => `
          <div style="display:flex; justify-content:space-between;">
            <span>${item.quantity} × ${item.productName} (${item.colorName}/${item.sizeName})</span>
            <span class="mono">${formatCurrency(item.subtotal)}</span>
          </div>
        `
          )
          .join('')}
        <div style="display:flex; justify-content:space-between; font-weight:700; font-size:var(--font-size-lg); margin-top:var(--space-3); padding-top:var(--space-3); border-top:1px solid var(--color-border);">
          <span>Total</span><span class="mono">${formatCurrency(venta.total)}</span>
        </div>
        <div style="margin-top: var(--space-3); padding-top: var(--space-3); border-top:1px solid var(--color-border);">
          <div style="font-weight:600; margin-bottom: var(--space-2);">Pagos</div>
          ${venta.payments.map((p) => `<div style="display:flex; justify-content:space-between;"><span>${p.paymentMethodName}${p.reference ? ` (${p.reference})` : ''}</span><span class="mono">${formatCurrency(p.amount)}</span></div>`).join('')}
        </div>
        ${
          venta.status !== 'COMPLETED'
            ? `<div class="alert alert-warning" style="margin-top: var(--space-3);"><span class="alert-message">${
                venta.status === 'CANCELLED'
                  ? `Anulada${venta.cancelledByUsername ? ` por ${venta.cancelledByUsername}` : ''}: ${venta.cancellationReason ?? ''}`
                  : SALE_STATUS_LABELS[venta.status] ?? venta.status
              }</span></div>`
            : ''
        }
      </div>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel-modal>Cerrar</button>
      <button class="btn btn-secondary" type="button" data-imprimir>Imprimir ticket</button>
      ${puedeDevolver ? `<button class="btn btn-secondary" type="button" data-devolver>Devolver</button>` : ''}
      ${puedeAnular ? `<button class="btn btn-danger" type="button" data-anular>Anular venta</button>` : ''}
    `,
  });

  modal.footer.querySelector('[data-cancel-modal]').addEventListener('click', () => closeModal());
  modal.footer.querySelector('[data-imprimir]').addEventListener('click', () => imprimirTicket(venta));
  modal.footer.querySelector('[data-devolver]')?.addEventListener('click', () => abrirFormularioDevolucion(venta));
  modal.footer.querySelector('[data-anular]')?.addEventListener('click', () => anularVenta(venta));
}

async function anularVenta(venta) {
  const modal = openModal({
    title: 'Anular venta',
    subtitle: venta.saleNumber,
    maxWidth: '400px',
    body: `
      <form id="anular-form" novalidate>
        <div class="alert alert-danger" id="anular-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="field">
          <label class="field-label" for="anular-reason">Motivo</label>
          <input class="input" id="anular-reason" maxlength="255" required autofocus />
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-danger" type="submit" form="anular-form">Anular venta</button>
    `,
  });
  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => closeModal());
  modal.body.querySelector('#anular-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#anular-error');
    try {
      await api.post(`/sales/${venta.id}/cancel`, { reason: modal.body.querySelector('#anular-reason').value.trim() });
      closeModal();
      showToast({ type: 'success', title: 'Venta anulada', message: venta.saleNumber });
      cargarHistorial();
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo anular la venta';
      errorAlert.hidden = false;
    }
  });
}

async function abrirFormularioDevolucion(venta) {
  let items;
  let metodos;
  try {
    [items, metodos] = await Promise.all([
      api.get(`/sales/${venta.id}/returnable-items`),
      api.get('/payment-methods'),
    ]);
  } catch (error) {
    showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo cargar la venta' });
    return;
  }

  const devolvibles = items.filter((i) => i.quantityReturnable > 0);
  if (devolvibles.length === 0) {
    showToast({ type: 'warning', title: 'Nada por devolver', message: 'Todos los artículos de esta venta ya fueron devueltos.' });
    return;
  }
  const metodosActivos = metodos.filter((m) => m.status === 'ACTIVE');

  const modal = openModal({
    title: 'Registrar devolución',
    subtitle: venta.saleNumber,
    maxWidth: '520px',
    body: `
      <form id="devolucion-form" novalidate>
        <div class="alert alert-danger" id="devolucion-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div style="display:flex; flex-direction:column; gap:var(--space-3); margin-bottom: var(--space-4);">
          ${devolvibles
            .map(
              (item) => `
            <div style="border:1px solid var(--color-border); border-radius: var(--radius-md); padding: var(--space-3);">
              <div style="display:flex; justify-content:space-between; align-items:center; gap:var(--space-3);">
                <div>
                  <div style="font-weight:600; font-size:var(--font-size-sm);">${item.productName}</div>
                  <div class="table-cell-muted mono">${item.variantSku} · disponible para devolver: ${item.quantityReturnable}</div>
                </div>
                <input class="input" type="number" min="0" max="${item.quantityReturnable}" value="0" style="width:80px; text-align:right;" data-dev-qty="${item.saleDetailId}" />
              </div>
              <label class="checkbox-field" style="margin-top: var(--space-2);">
                <input type="checkbox" data-dev-restock="${item.saleDetailId}" checked /> Reingresar a stock
              </label>
            </div>
          `
            )
            .join('')}
        </div>
        <div class="form-grid">
          <div class="field field-span-2">
            <label class="field-label" for="dev-reason">Motivo</label>
            <input class="input" id="dev-reason" maxlength="255" required />
          </div>
          <div class="field field-span-2">
            <label class="field-label" for="dev-refund-method">Método de reembolso</label>
            <select class="select" id="dev-refund-method" required>
              ${metodosActivos.map((m) => `<option value="${m.id}">${m.name}</option>`).join('')}
            </select>
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="devolucion-form">Registrar devolución</button>
    `,
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => closeModal());
  modal.body.querySelector('#devolucion-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#devolucion-error');
    errorAlert.hidden = true;

    const itemsSeleccionados = devolvibles
      .map((item) => ({
        saleDetailId: item.saleDetailId,
        quantity: Number(modal.body.querySelector(`[data-dev-qty="${item.saleDetailId}"]`).value) || 0,
        restock: modal.body.querySelector(`[data-dev-restock="${item.saleDetailId}"]`).checked,
      }))
      .filter((item) => item.quantity > 0);

    if (itemsSeleccionados.length === 0) {
      errorAlert.querySelector('.alert-message').textContent = 'Indica la cantidad a devolver de al menos un artículo.';
      errorAlert.hidden = false;
      return;
    }

    try {
      const devolucion = await api.post('/returns', {
        saleId: venta.id,
        reason: modal.body.querySelector('#dev-reason').value.trim(),
        refundMethodId: Number(modal.body.querySelector('#dev-refund-method').value),
        items: itemsSeleccionados,
      });
      closeModal();
      showToast({ type: 'success', title: 'Devolución registrada', message: `${devolucion.returnNumber} · ${formatCurrency(devolucion.totalAmount)}` });
      cargarHistorial();
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo registrar la devolución';
      errorAlert.hidden = false;
    }
  });
}

async function cargarDevoluciones() {
  const body = document.querySelector('#devoluciones-body');
  try {
    const page = await api.get('/returns', { query: { page: devolucionesPage, size: 20, sort: 'createdAt,desc' } });

    body.innerHTML = page.content.length
      ? page.content
          .map(
            (d) => `
        <tr>
          <td class="table-cell-primary mono">${d.returnNumber}</td>
          <td class="mono">${d.saleNumber}</td>
          <td class="table-cell-muted">${formatDateTime(d.createdAt)}</td>
          <td>${d.reason}</td>
          <td>${d.refundMethodName}</td>
          <td class="mono">${formatCurrency(d.totalAmount)}</td>
          <td>${d.username}</td>
        </tr>
      `
          )
          .join('')
      : `<tr><td colspan="7"><div class="empty-state"><span>No se han registrado devoluciones.</span></div></td></tr>`;

    renderPagination(document.querySelector('#devoluciones-pagination'), page, (p) => {
      devolucionesPage = p;
      cargarDevoluciones();
    });
  } catch (error) {
    body.innerHTML = `<tr><td colspan="7"><div class="empty-state"><span>${error instanceof ApiError ? error.message : 'Error al cargar devoluciones'}</span></div></td></tr>`;
  }
}

async function cargarPromotores() {
  const body = document.querySelector('#promotores-body');
  try {
    promotoresCache = await api.get('/promoters');
    body.innerHTML = promotoresCache.length
      ? promotoresCache
          .map(
            (p) => `
        <tr>
          <td class="table-cell-primary">${p.name}</td>
          <td>${statusBadge(p.status)}</td>
          <td>
            <div class="table-actions">
              <button class="btn btn-ghost btn-sm" type="button" data-editar-promotor="${p.id}">Editar</button>
              <button class="btn btn-ghost btn-sm" type="button" data-toggle-promotor="${p.id}" data-status="${p.status}">
                ${p.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
              </button>
            </div>
          </td>
        </tr>
      `
          )
          .join('')
      : `<tr><td colspan="3"><div class="empty-state"><span>Todavía no hay promotores registrados.</span></div></td></tr>`;

    body.querySelectorAll('[data-editar-promotor]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const promotor = promotoresCache.find((p) => String(p.id) === btn.dataset.editarPromotor);
        abrirFormularioPromotor(promotor);
      });
    });
    body.querySelectorAll('[data-toggle-promotor]').forEach((btn) => {
      btn.addEventListener('click', () => cambiarEstadoPromotor(btn.dataset.togglePromotor, btn.dataset.status));
    });
  } catch (error) {
    body.innerHTML = `<tr><td colspan="3"><div class="empty-state"><span>${error instanceof ApiError ? error.message : 'Error al cargar promotores'}</span></div></td></tr>`;
  }
}

function abrirFormularioPromotor(promotor) {
  const esEdicion = Boolean(promotor);
  const modal = openModal({
    title: esEdicion ? 'Editar promotor' : 'Nuevo promotor',
    maxWidth: '380px',
    body: `
      <form id="promotor-form" novalidate>
        <div class="alert alert-danger" id="promotor-form-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="field">
          <label class="field-label" for="pm-nombre">Nombre</label>
          <input class="input" id="pm-nombre" maxlength="120" required value="${promotor?.name ?? ''}" />
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="promotor-form">${esEdicion ? 'Guardar cambios' : 'Crear'}</button>
    `,
  });
  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => closeModal());
  modal.body.querySelector('#promotor-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#promotor-form-error');
    const nombre = modal.body.querySelector('#pm-nombre').value.trim();
    try {
      if (esEdicion) {
        await api.put(`/promoters/${promotor.id}`, { name: nombre });
      } else {
        await api.post('/promoters', { name: nombre });
      }
      closeModal();
      showToast({ type: 'success', title: esEdicion ? 'Promotor actualizado' : 'Promotor creado' });
      cargarPromotores();
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo guardar';
      errorAlert.hidden = false;
    }
  });
}

async function cambiarEstadoPromotor(id, currentStatus) {
  const nuevoEstado = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  try {
    await api.patch(`/promoters/${id}/status`, { status: nuevoEstado });
    showToast({ type: 'success', title: 'Estado actualizado' });
    cargarPromotores();
  } catch (error) {
    showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo actualizar' });
  }
}
