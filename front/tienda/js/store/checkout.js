import { storeApi, ApiError, API_ORIGIN } from './core/store-api.js';
import { requireCustomerSession } from './core/customer-auth.js';
import { renderStoreShell } from './components/store-shell.js';
import { getCart, cartTotal, clearCart } from './core/cart.js';
import { getDepartamentos, getProvincias, getDistritos } from './core/ubigeo.js';
import { renderStoreSteps } from './components/store-steps.js';
import { formatCurrency, escapeHtml } from '../../../js/core/format.js';
import { showToast } from '../../../js/components/toast.js';

const CODIGO_CONTRAENTREGA = 'CONTRAENTREGA';

let metodosPagoTodos = [];
let metodoSeleccionado = null;
let shippingInfo = { flatRate: 0, freeShippingDistrict: null };
let distritoSeleccionadoNombre = null;

function metodosPagoVisibles() {
  const esHuacho = distritoSeleccionadoNombre?.toLowerCase() === shippingInfo.freeShippingDistrict?.toLowerCase();
  return metodosPagoTodos.filter((m) => m.code !== CODIGO_CONTRAENTREGA || esHuacho);
}

function calcularEnvio() {
  if (metodoSeleccionado?.code === CODIGO_CONTRAENTREGA) return 0;
  return shippingInfo.flatRate;
}

function render(session) {
  const contenedor = document.querySelector('#checkout-content');

  contenedor.innerHTML = `
    <form id="checkout-form">
      <p class="field-hint" style="margin-bottom: var(--space-3);">Datos de quien recibe el envío — la courier los exige para registrar el paquete.</p>
      <div class="form-grid" style="margin-bottom: var(--space-3);">
        <div class="field">
          <label class="field-label" for="recipientDni">DNI</label>
          <input class="input" id="recipientDni" name="recipientDni" required maxlength="15" inputmode="numeric" />
        </div>
        <div class="field">
          <label class="field-label" for="recipientFirstName">Nombres</label>
          <input class="input" id="recipientFirstName" name="recipientFirstName" required maxlength="100" value="${session.customer.fullName?.split(' ')[0] ?? ''}" />
        </div>
        <div class="field">
          <label class="field-label" for="recipientLastNamePaterno">Apellido paterno</label>
          <input class="input" id="recipientLastNamePaterno" name="recipientLastNamePaterno" required maxlength="60" />
        </div>
        <div class="field">
          <label class="field-label" for="recipientLastNameMaterno">Apellido materno</label>
          <input class="input" id="recipientLastNameMaterno" name="recipientLastNameMaterno" required maxlength="60" />
        </div>
      </div>
      <div class="field" style="margin-bottom: var(--space-3);">
        <label class="field-label" for="phone">Teléfono</label>
        <input class="input" id="phone" name="phone" required value="${session.customer.phone ?? ''}" />
      </div>
      <div class="field" style="margin-bottom: var(--space-3);">
        <label class="field-label" for="address">Dirección</label>
        <input class="input" id="address" name="address" required placeholder="Av./Jr./Calle, número, referencia" />
      </div>

      <div class="form-grid" style="margin-bottom: var(--space-3);">
        <div class="field">
          <label class="field-label" for="departamento">Departamento</label>
          <select class="select" id="departamento" required>
            <option value="">Selecciona…</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label" for="provincia">Provincia</label>
          <select class="select" id="provincia" required disabled>
            <option value="">Elige un departamento primero</option>
          </select>
        </div>
        <div class="field field-span-2">
          <label class="field-label" for="distrito">Distrito</label>
          <select class="select" id="distrito" required disabled>
            <option value="">Elige una provincia primero</option>
          </select>
        </div>
      </div>

      <div class="field" style="margin-bottom: var(--space-4);">
        <label class="field-label" for="notes">Notas (opcional)</label>
        <textarea class="input" id="notes" name="notes" rows="2"></textarea>
      </div>

      <div class="field" style="margin-bottom: var(--space-4);">
        <span class="field-label">Método de pago</span>
        <div id="payment-methods" style="display:flex; flex-direction:column; gap: var(--space-2); margin-top: var(--space-2);"></div>
        <div id="payment-detail"></div>
      </div>

      <div class="field" id="reference-field" style="display:none; margin-bottom: var(--space-4);">
        <label class="field-label" for="paymentReference">Número de operación</label>
        <input class="input" id="paymentReference" name="paymentReference" />
      </div>

      <div class="field" id="proof-field" style="display:none; margin-bottom: var(--space-4);">
        <label class="field-label" for="proofInput">Comprobante de pago</label>
        <input type="file" class="input" id="proofInput" accept="image/png,image/jpeg,image/webp" />
        <span class="field-hint">Opcional — si ya pagaste, súbelo para agilizar la confirmación. También puedes hacerlo después desde "Mis pedidos".</span>
      </div>

      <button class="btn btn-primary btn-lg btn-block" type="submit">Confirmar pedido</button>
    </form>

    <div class="store-summary" id="order-summary"></div>
  `;

  poblarUbigeo();
  renderMetodosPago();
  actualizarResumen();
  document.querySelector('#checkout-form').addEventListener('submit', enviarPedido);
}

async function poblarUbigeo() {
  const depSelect = document.querySelector('#departamento');
  const provSelect = document.querySelector('#provincia');
  const distSelect = document.querySelector('#distrito');

  const departamentos = await getDepartamentos();
  departamentos.forEach((d) => depSelect.insertAdjacentHTML('beforeend', `<option value="${d.id}">${d.nombre}</option>`));

  depSelect.addEventListener('change', async () => {
    provSelect.innerHTML = '<option value="">Selecciona…</option>';
    distSelect.innerHTML = '<option value="">Elige una provincia primero</option>';
    distSelect.disabled = true;
    distritoSeleccionadoNombre = null;
    if (!depSelect.value) {
      provSelect.disabled = true;
      provSelect.innerHTML = '<option value="">Elige un departamento primero</option>';
      renderMetodosPago();
      return;
    }
    const provincias = await getProvincias(depSelect.value);
    provincias.forEach((p) => provSelect.insertAdjacentHTML('beforeend', `<option value="${p.id}">${p.nombre}</option>`));
    provSelect.disabled = false;
    renderMetodosPago();
  });

  provSelect.addEventListener('change', async () => {
    distSelect.innerHTML = '<option value="">Selecciona…</option>';
    distritoSeleccionadoNombre = null;
    if (!provSelect.value) {
      distSelect.disabled = true;
      distSelect.innerHTML = '<option value="">Elige una provincia primero</option>';
      renderMetodosPago();
      return;
    }
    const distritos = await getDistritos(provSelect.value);
    distritos.forEach((d) => distSelect.insertAdjacentHTML('beforeend', `<option value="${d.nombre}">${d.nombre}</option>`));
    distSelect.disabled = false;
    renderMetodosPago();
  });

  distSelect.addEventListener('change', () => {
    distritoSeleccionadoNombre = distSelect.value || null;
    renderMetodosPago();
    actualizarResumen();
  });
}

function renderMetodosPago() {
  const contenedor = document.querySelector('#payment-methods');
  const visibles = metodosPagoVisibles();

  // Si el método elegido ya no está disponible (cambiaron de distrito), se deselecciona.
  if (metodoSeleccionado && !visibles.some((m) => m.id === metodoSeleccionado.id)) {
    metodoSeleccionado = null;
  }

  contenedor.innerHTML = visibles
    .map(
      (m) => `
    <label class="store-payment-option" data-selected="${metodoSeleccionado?.id === m.id}" data-method-id="${m.id}">
      <input type="radio" name="paymentMethod" value="${m.id}" style="width:auto;" ${metodoSeleccionado?.id === m.id ? 'checked' : ''} />
      <span>${m.name}${m.code === CODIGO_CONTRAENTREGA ? ' · envío gratis' : ''}</span>
    </label>
  `
    )
    .join('');

  contenedor.querySelectorAll('[data-method-id]').forEach((label) => {
    label.addEventListener('click', () => seleccionarMetodo(Number(label.dataset.methodId)));
  });

  if (!metodoSeleccionado) {
    document.querySelector('#payment-detail').innerHTML = '';
    document.querySelector('#reference-field').style.display = 'none';
    document.querySelector('#proof-field').style.display = 'none';
  }
  actualizarResumen();
}

function seleccionarMetodo(id) {
  metodoSeleccionado = metodosPagoTodos.find((m) => m.id === id);
  document.querySelectorAll('#payment-methods [data-method-id]').forEach((label) => {
    const selected = Number(label.dataset.methodId) === id;
    label.dataset.selected = String(selected);
    label.querySelector('input').checked = selected;
  });

  const detalle = document.querySelector('#payment-detail');
  if (metodoSeleccionado?.qrImageUrl) {
    detalle.innerHTML = `
      <div class="store-qr-box">
        <img src="${API_ORIGIN}${metodoSeleccionado.qrImageUrl}" alt="QR de ${metodoSeleccionado.name}" />
        ${metodoSeleccionado.accountHolder ? `<span>${metodoSeleccionado.accountHolder}</span>` : ''}
        ${metodoSeleccionado.accountNumber ? `<span class="mono">${metodoSeleccionado.accountNumber}</span>` : ''}
      </div>
    `;
  } else if (metodoSeleccionado?.accountNumber) {
    detalle.innerHTML = `<p style="margin-top: var(--space-2);"><strong>${metodoSeleccionado.accountHolder ?? ''}</strong> — <span class="mono">${metodoSeleccionado.accountNumber}</span></p>`;
  } else {
    detalle.innerHTML = '';
  }

  document.querySelector('#reference-field').style.display = metodoSeleccionado?.requiresReference ? 'flex' : 'none';
  document.querySelector('#proof-field').style.display = metodoSeleccionado?.type === 'DIGITAL_WALLET' ? 'flex' : 'none';
  actualizarResumen();
}

function actualizarResumen() {
  const items = getCart();
  const subtotal = cartTotal(items);
  const envio = calcularEnvio();
  const resumen = document.querySelector('#order-summary');
  if (!resumen) return;

  resumen.innerHTML = `
    <h3 style="margin-bottom: var(--space-3);">Resumen</h3>
    ${items
      .map(
        (it) => `
      <div class="store-summary-row">
        <span>${escapeHtml(it.productName)} (${escapeHtml(it.variantLabel)}) × ${it.quantity}</span>
        <span>${formatCurrency(it.unitPrice * it.quantity)}</span>
      </div>
    `
      )
      .join('')}
    <div class="store-summary-row"><span>Subtotal</span><span>${formatCurrency(subtotal)}</span></div>
    <div class="store-summary-row">
      <span>Envío${!metodoSeleccionado ? ' (estimado)' : ''}</span>
      <span>${envio === 0 ? 'Gratis' : formatCurrency(envio)}</span>
    </div>
    <div class="store-summary-total"><span>Total</span><span>${formatCurrency(subtotal + envio)}</span></div>
  `;
}

async function enviarPedido(event) {
  event.preventDefault();
  const form = event.target;
  const depSelect = document.querySelector('#departamento');
  const provSelect = document.querySelector('#provincia');
  const distSelect = document.querySelector('#distrito');

  if (!metodoSeleccionado) {
    showToast({ type: 'danger', title: 'Elige un método de pago' });
    return;
  }
  if (!depSelect.value || !provSelect.value || !distSelect.value) {
    showToast({ type: 'danger', title: 'Completa departamento, provincia y distrito' });
    return;
  }

  const items = getCart();
  if (!items.length) {
    showToast({ type: 'danger', title: 'Tu carrito está vacío' });
    return;
  }

  const submitButton = form.querySelector('button[type="submit"]');
  submitButton.disabled = true;

  try {
    const pedido = await storeApi.post(
      '/store/orders',
      {
        items: items.map((it) => ({ variantId: it.variantId, quantity: it.quantity })),
        paymentMethodId: metodoSeleccionado.id,
        paymentReference: form.paymentReference.value.trim() || null,
        recipientDni: form.recipientDni.value.trim(),
        recipientFirstName: form.recipientFirstName.value.trim(),
        recipientLastNamePaterno: form.recipientLastNamePaterno.value.trim(),
        recipientLastNameMaterno: form.recipientLastNameMaterno.value.trim(),
        phone: form.phone.value.trim(),
        address: form.address.value.trim(),
        department: depSelect.options[depSelect.selectedIndex].text,
        province: provSelect.options[provSelect.selectedIndex].text,
        district: distSelect.value,
        notes: form.notes.value.trim() || null,
      },
      { auth: true }
    );

    const proofFile = document.querySelector('#proofInput')?.files?.[0];
    if (proofFile) {
      const formData = new FormData();
      formData.append('file', proofFile);
      try {
        await storeApi.post(`/store/orders/${pedido.id}/payment-proof`, formData, { auth: true });
      } catch {
        // El pedido ya se creó — si el comprobante falla al subir, el cliente lo agrega después desde "Mis pedidos".
        showToast({ type: 'danger', title: 'El pedido se creó, pero no se pudo subir el comprobante', message: 'Puedes agregarlo luego desde "Mis pedidos".' });
      }
    }

    clearCart();
    showToast({ type: 'success', title: 'Pedido creado' });
    window.location.href = 'cuenta/pedidos.html';
  } catch (error) {
    showToast({ type: 'danger', title: 'No se pudo crear el pedido', message: error instanceof ApiError ? error.message : undefined });
  } finally {
    submitButton.disabled = false;
  }
}

async function init() {
  const session = requireCustomerSession();
  if (!session) return;

  if (!getCart().length) {
    window.location.href = 'carrito.html';
    return;
  }

  renderStoreShell();
  renderStoreSteps(document.querySelector('#store-steps'), 2);

  const [metodos, envio] = await Promise.all([
    storeApi.get('/store/catalog/payment-methods').catch(() => []),
    storeApi.get('/store/catalog/shipping-info').catch(() => ({ flatRate: 0, freeShippingDistrict: null })),
  ]);
  metodosPagoTodos = metodos;
  shippingInfo = envio;

  render(session);
}

init();
