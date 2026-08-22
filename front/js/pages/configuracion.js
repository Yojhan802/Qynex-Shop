import { requireSession, hasPermission } from '../core/auth.js';
import { api, ApiError, API_ORIGIN } from '../core/api.js';
import { renderShell } from '../components/shell.js';
import { openModal, closeModal } from '../components/modal.js';
import { statusBadge } from '../components/status-badge.js';
import { showToast } from '../components/toast.js';
import { formatDateTime } from '../core/format.js';

const TIPO_LABELS = { CASH: 'Efectivo', DIGITAL_WALLET: 'Billetera digital', CARD: 'Tarjeta', TRANSFER: 'Transferencia' };

const PLAN_ORDER = ['STARTER', 'PROFESIONAL', 'ECOMMERCE', 'IA'];
const PLAN_LABELS = { STARTER: 'Starter', PROFESIONAL: 'Profesional', ECOMMERCE: 'Ecommerce', IA: 'IA' };
const PLAN_BADGE_CLASS = { STARTER: 'badge-neutral', PROFESIONAL: 'badge-info', ECOMMERCE: 'badge-success', IA: 'badge-warning' };
const PLAN_DESCRIPTIONS = {
  STARTER: 'Ventas, inventario y caja para empezar — hasta 3 usuarios.',
  PROFESIONAL: 'Todo Starter, más promotores, auditoría, separaciones, combos y promociones, y usuarios ilimitados.',
  ECOMMERCE: 'Todo Profesional, más tienda online: catálogo, carrito, pedidos y notificaciones en tiempo real.',
  IA: 'Todo Ecommerce, más funciones de inteligencia artificial.',
};
const PLAN_MODULES = [
  { label: 'Ventas y POS', minPlan: 'STARTER' },
  { label: 'Inventario y almacén', minPlan: 'STARTER' },
  { label: 'Caja', minPlan: 'STARTER' },
  { label: 'Clientes', minPlan: 'STARTER' },
  { label: 'Reportes', minPlan: 'STARTER' },
  { label: 'Usuarios y roles (hasta 3 en Starter, ilimitado desde Profesional)', minPlan: 'STARTER' },
  { label: 'Promotores', minPlan: 'PROFESIONAL' },
  { label: 'Auditoría', minPlan: 'PROFESIONAL' },
  { label: 'Separaciones (apartados con depósito)', minPlan: 'PROFESIONAL' },
  { label: 'Combos y promociones', minPlan: 'PROFESIONAL' },
  { label: 'Tienda online (catálogo, carrito, pedidos)', minPlan: 'ECOMMERCE' },
  { label: 'Notificaciones en tiempo real (pedidos nuevos y su estado)', minPlan: 'ECOMMERCE' },
  { label: 'Inteligencia artificial', minPlan: 'IA' },
];

let activeTab = 'empresa';

function init() {
  document.querySelectorAll('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      activeTab = tab.dataset.tab;
      document.querySelectorAll('.tab').forEach((t) => t.setAttribute('aria-selected', String(t.dataset.tab === activeTab)));
      document.querySelector('#panel-empresa').hidden = activeTab !== 'empresa';
      document.querySelector('#panel-pagos').hidden = activeTab !== 'pagos';
      cargarPanelActivo();
    });
  });

  cargarPanelActivo();
}

function cargarPanelActivo() {
  if (activeTab === 'empresa') cargarEmpresa();
  else cargarMetodosPago();
}

async function cargarEmpresa() {
  const content = document.querySelector('#empresa-content');
  try {
    const settings = await api.get('/settings/company');
    renderPlanCard(settings);
    renderFormularioEmpresa(settings);
  } catch (error) {
    content.innerHTML = `<div class="empty-state"><span>${error instanceof ApiError ? error.message : 'No se pudo cargar la configuración'}</span></div>`;
  }
}

function renderPlanCard(settings) {
  const container = document.querySelector('#plan-content');
  const plan = settings.plan;
  const planIndex = PLAN_ORDER.indexOf(plan);

  const items = PLAN_MODULES.map((mod) => {
    const incluido = planIndex >= PLAN_ORDER.indexOf(mod.minPlan);
    const icon = incluido
      ? `<svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="color: var(--color-success); flex-shrink:0;"><path d="M4 10l4 4 8-8" stroke-linecap="round" stroke-linejoin="round"/></svg>`
      : `<svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="color: var(--color-text-muted); flex-shrink:0;"><path d="M6 6l8 8M14 6l-8 8" stroke-linecap="round"/></svg>`;
    return `<li style="display:flex; align-items:center; gap: var(--space-2); ${incluido ? '' : 'color: var(--color-text-muted);'}">${icon}<span>${mod.label}</span></li>`;
  }).join('');

  container.innerHTML = `
    <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom: var(--space-4);">
      <div>
        <p class="table-cell-muted" style="margin-bottom: var(--space-1);">Plan actual</p>
        <div style="display:flex; align-items:center; gap: var(--space-2);">
          <span class="badge ${PLAN_BADGE_CLASS[plan] ?? 'badge-neutral'}">${PLAN_LABELS[plan] ?? plan}</span>
        </div>
      </div>
      ${renderEstadoSuscripcion(settings)}
    </div>
    <p style="margin-bottom: var(--space-4);">${PLAN_DESCRIPTIONS[plan] ?? ''}</p>
    <ul style="list-style:none; padding:0; margin:0; display:grid; gap: var(--space-2);">${items}</ul>
    <p class="table-cell-muted" style="margin-top: var(--space-4); padding-top: var(--space-4); border-top: 1px solid var(--color-border);">
      El plan y la suscripción los gestiona el proveedor del sistema — contáctalo para ampliar el plan o regularizar el pago.
    </p>
  `;
}

function renderEstadoSuscripcion(settings) {
  const { subscriptionStatus, nextPaymentDue } = settings;
  if (subscriptionStatus === 'SUSPENDIDA') {
    return `
      <div style="text-align:right;">
        <p class="table-cell-muted" style="margin-bottom: var(--space-1);">Suscripción</p>
        <span class="badge badge-danger">Suspendida</span>
      </div>
    `;
  }
  if (!nextPaymentDue) return '';

  const dias = Math.ceil((new Date(nextPaymentDue) - new Date()) / (1000 * 60 * 60 * 24));
  const fecha = new Date(nextPaymentDue + 'T00:00:00').toLocaleDateString('es-PE', { day: 'numeric', month: 'long', year: 'numeric' });
  const proximoAVencer = dias <= 7;
  return `
    <div style="text-align:right;">
      <p class="table-cell-muted" style="margin-bottom: var(--space-1);">Próximo pago</p>
      <span class="badge ${proximoAVencer ? 'badge-warning' : 'badge-neutral'}">${fecha}</span>
    </div>
  `;
}

function renderFormularioEmpresa(settings) {
  const content = document.querySelector('#empresa-content');
  const puedeIdentidad = hasPermission('CONFIGURACION_IDENTIDAD_EDITAR');
  const puedeOperativo = hasPermission('CONFIGURACION_EDITAR');

  const secciones = [];
  if (puedeIdentidad) secciones.push('<div class="table-card" style="padding: var(--space-5);" id="identidad-card"></div>');
  if (puedeOperativo) secciones.push('<div class="table-card" style="padding: var(--space-5);" id="operativo-card"></div>');
  if (secciones.length === 0) {
    secciones.push(`
      <div class="table-card" style="padding: var(--space-5);">
        <p class="table-cell-muted">No tienes permisos para editar más configuración de la empresa.</p>
      </div>
    `);
  }
  content.innerHTML = secciones.join('');

  if (puedeIdentidad) renderIdentidadForm(settings);
  if (puedeOperativo) renderOperativoForm(settings);
}

function renderIdentidadForm(settings) {
  const card = document.querySelector('#identidad-card');
  const logoSrc = settings.logoUrl ? `${API_ORIGIN}${settings.logoUrl}` : null;

  card.innerHTML = `
    <h3 style="margin: 0 0 var(--space-4);">Identidad de la empresa</h3>
    <div style="display:flex; align-items:center; gap: var(--space-4); margin-bottom: var(--space-5); padding-bottom: var(--space-5); border-bottom: 1px solid var(--color-border);">
      <div style="width:72px; height:72px; border-radius: var(--radius-md); background: var(--color-surface-muted); display:flex; align-items:center; justify-content:center; overflow:hidden; flex-shrink:0;">
        ${
          logoSrc
            ? `<img src="${logoSrc}" alt="Logo de la empresa" style="width:100%; height:100%; object-fit:contain;" />`
            : `<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 8l8-4 8 4-8 4-8-4z"/><path d="M4 8v8l8 4 8-4V8"/></svg>`
        }
      </div>
      <div>
        <label class="btn btn-secondary btn-sm" for="logo-input" style="cursor:pointer;">Cambiar logo</label>
        <input type="file" id="logo-input" accept="image/png,image/jpeg,image/webp,image/svg+xml" style="display:none;" />
        <p class="table-cell-muted" style="margin-top: var(--space-1);">PNG, JPG, WEBP o SVG.</p>
      </div>
    </div>

    <form id="identidad-form" novalidate>
      <div class="alert alert-danger" id="identidad-form-error" role="alert" hidden>
        <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
        <span class="alert-message"></span>
      </div>
      <div class="form-grid">
        <div class="field field-span-2">
          <label class="field-label" for="ef-name">Razón social</label>
          <input class="input" id="ef-name" required maxlength="150" value="${settings.name}" />
        </div>
        <div class="field">
          <label class="field-label" for="ef-ruc">RUC</label>
          <input class="input" id="ef-ruc" maxlength="15" value="${settings.ruc ?? ''}" />
        </div>
        <div class="field">
          <label class="field-label" for="ef-phone">Teléfono</label>
          <input class="input" id="ef-phone" maxlength="20" value="${settings.phone ?? ''}" />
        </div>
        <div class="field field-span-2">
          <label class="field-label" for="ef-address">Dirección</label>
          <input class="input" id="ef-address" maxlength="255" value="${settings.address ?? ''}" />
        </div>
        <div class="field field-span-2">
          <label class="field-label" for="ef-email">Email</label>
          <input class="input" type="email" id="ef-email" maxlength="120" value="${settings.email ?? ''}" />
        </div>
      </div>

      <div style="display:flex; justify-content:space-between; align-items:center; padding-top: var(--space-4); border-top: 1px solid var(--color-border);">
        <p class="table-cell-muted">
          ${settings.updatedByUsername ? `Última edición por ${settings.updatedByUsername} · ${formatDateTime(settings.updatedAt)}` : ''}
        </p>
        <button class="btn btn-primary" type="submit">Guardar cambios</button>
      </div>
    </form>
  `;

  document.querySelector('#logo-input').addEventListener('change', (event) => {
    const file = event.target.files?.[0];
    if (file) subirLogo(file);
  });

  document.querySelector('#identidad-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = document.querySelector('#identidad-form-error');
    errorAlert.hidden = true;
    try {
      const actualizado = await api.put('/settings/company/identity', {
        name: document.querySelector('#ef-name').value.trim(),
        ruc: document.querySelector('#ef-ruc').value.trim() || null,
        address: document.querySelector('#ef-address').value.trim() || null,
        phone: document.querySelector('#ef-phone').value.trim() || null,
        email: document.querySelector('#ef-email').value.trim() || null,
      });
      showToast({ type: 'success', title: 'Configuración guardada' });
      renderIdentidadForm(actualizado);
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo guardar la configuración';
      errorAlert.hidden = false;
    }
  });
}

async function subirLogo(file) {
  const formData = new FormData();
  formData.append('file', file);
  try {
    const actualizado = await api.post('/settings/company/logo', formData);
    showToast({ type: 'success', title: 'Logo actualizado' });
    renderIdentidadForm(actualizado);
  } catch (error) {
    showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo subir el logo' });
  }
}

function renderOperativoForm(settings) {
  const card = document.querySelector('#operativo-card');

  card.innerHTML = `
    <h3 style="margin: 0 0 var(--space-4);">Datos operativos</h3>
    <form id="operativo-form" novalidate>
      <div class="alert alert-danger" id="operativo-form-error" role="alert" hidden>
        <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
        <span class="alert-message"></span>
      </div>
      <div class="form-grid">
        <div class="field">
          <label class="field-label" for="ef-currency-code">Moneda (código)</label>
          <input class="input mono" id="ef-currency-code" required maxlength="3" value="${settings.currencyCode}" style="text-transform:uppercase;" />
        </div>
        <div class="field">
          <label class="field-label" for="ef-currency-symbol">Símbolo</label>
          <input class="input mono" id="ef-currency-symbol" required maxlength="5" value="${settings.currencySymbol}" />
        </div>
        <div class="field">
          <label class="field-label" for="ef-igv">IGV (%)</label>
          <input class="input" type="number" id="ef-igv" required min="0" max="100" step="0.01" value="${(settings.igvRate * 100).toFixed(2)}" />
        </div>
        <div class="field">
          <label class="field-label" for="ef-shipping">Tarifa de envío (S/)</label>
          <input class="input" type="number" id="ef-shipping" required min="0" step="0.01" value="${settings.shippingFlatRate ?? '0.00'}" />
          <span class="field-hint">Se cobra en todos los pedidos online, salvo contraentrega en Huacho (gratis).</span>
        </div>
        <div class="field field-span-2">
          <label class="field-label" for="ef-footer">Pie de ticket</label>
          <textarea class="input" id="ef-footer" maxlength="255" rows="2">${settings.ticketFooter ?? ''}</textarea>
        </div>
        <div class="field">
          <label class="field-label" for="ef-reservation-deposit">Seña por defecto de separaciones (S/)</label>
          <input class="input" type="number" id="ef-reservation-deposit" required min="0" step="0.01" value="${settings.reservationDepositAmount ?? '20.00'}" />
          <span class="field-hint">El cajero puede ajustarla al crear cada separación.</span>
        </div>
        <div class="field">
          <label class="field-label" for="ef-reservation-days">Vencimiento de separaciones (días)</label>
          <input class="input" type="number" id="ef-reservation-days" required min="1" step="1" value="${settings.reservationExpirationDays ?? 3}" />
          <span class="field-hint">Pasado este plazo se libera el stock y la seña se pierde.</span>
        </div>
      </div>

      <div style="display:flex; justify-content:space-between; align-items:center; padding-top: var(--space-4); border-top: 1px solid var(--color-border);">
        <p class="table-cell-muted">
          ${settings.updatedByUsername ? `Última edición por ${settings.updatedByUsername} · ${formatDateTime(settings.updatedAt)}` : ''}
        </p>
        <button class="btn btn-primary" type="submit">Guardar cambios</button>
      </div>
    </form>
  `;

  document.querySelector('#operativo-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = document.querySelector('#operativo-form-error');
    errorAlert.hidden = true;
    try {
      const actualizado = await api.put('/settings/company', {
        currencyCode: document.querySelector('#ef-currency-code').value.trim().toUpperCase(),
        currencySymbol: document.querySelector('#ef-currency-symbol').value.trim(),
        igvRate: Number(document.querySelector('#ef-igv').value) / 100,
        ticketFooter: document.querySelector('#ef-footer').value.trim() || null,
        shippingFlatRate: Number(document.querySelector('#ef-shipping').value),
        reservationDepositAmount: Number(document.querySelector('#ef-reservation-deposit').value),
        reservationExpirationDays: Number(document.querySelector('#ef-reservation-days').value),
      });
      showToast({ type: 'success', title: 'Configuración guardada' });
      renderOperativoForm(actualizado);
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo guardar la configuración';
      errorAlert.hidden = false;
    }
  });
}

async function cargarMetodosPago() {
  const body = document.querySelector('#payment-methods-body');
  try {
    const metodos = await api.get('/payment-methods');
    body.innerHTML = metodos.length
      ? metodos
          .map(
            (m) => `
        <tr>
          <td>
            <div class="table-cell-primary">${m.name}</div>
            <div class="table-cell-muted">${TIPO_LABELS[m.type] ?? m.type}</div>
          </td>
          <td>${m.accountHolder ?? '—'}</td>
          <td class="mono">${m.accountNumber ?? '—'}</td>
          <td>${statusBadge(m.status)}</td>
          <td>
            <div class="table-actions">
              <button class="btn btn-ghost btn-sm" type="button" data-action="editar" data-id="${m.id}">Editar</button>
              <button class="btn btn-ghost btn-sm" type="button" data-action="toggle" data-id="${m.id}" data-status="${m.status}">
                ${m.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
              </button>
            </div>
          </td>
        </tr>
      `
          )
          .join('')
      : `<tr><td colspan="5"><div class="empty-state"><span>No hay métodos de pago configurados.</span></div></td></tr>`;

    body.querySelectorAll('[data-action="editar"]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const metodo = metodos.find((m) => String(m.id) === btn.dataset.id);
        abrirFormularioMetodoPago(metodo);
      });
    });
    body.querySelectorAll('[data-action="toggle"]').forEach((btn) => {
      btn.addEventListener('click', () => cambiarEstadoMetodoPago(btn.dataset.id, btn.dataset.status));
    });
  } catch (error) {
    body.innerHTML = `<tr><td colspan="5"><div class="empty-state"><span>${error instanceof ApiError ? error.message : 'Error al cargar métodos de pago'}</span></div></td></tr>`;
  }
}

function abrirFormularioMetodoPago(metodo) {
  const qrSrc = metodo.qrImageUrl ? `${API_ORIGIN}${metodo.qrImageUrl}` : null;

  const modal = openModal({
    title: `Editar ${metodo.name}`,
    subtitle: TIPO_LABELS[metodo.type] ?? metodo.type,
    maxWidth: '480px',
    body: `
      <form id="pm-form" novalidate>
        <div class="alert alert-danger" id="pm-form-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        ${
          metodo.requiresReference
            ? `
        <div style="display:flex; align-items:center; gap: var(--space-4); margin-bottom: var(--space-5); padding-bottom: var(--space-5); border-bottom: 1px solid var(--color-border);">
          <div id="pm-qr-preview-wrap" style="width:96px; height:96px; border-radius: var(--radius-md); background: var(--color-surface-muted); display:flex; align-items:center; justify-content:center; overflow:hidden; flex-shrink:0;">
            ${
              qrSrc
                ? `<img id="pm-qr-preview" src="${qrSrc}" alt="QR de pago" style="width:100%; height:100%; object-fit:contain;" />`
                : `<svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="4" y="4" width="7" height="7"/><rect x="13" y="4" width="7" height="7"/><rect x="4" y="13" width="7" height="7"/></svg>`
            }
          </div>
          <div>
            <label class="btn btn-secondary btn-sm" for="pm-qr-input" style="cursor:pointer;">Subir código QR</label>
            <input type="file" id="pm-qr-input" accept="image/png,image/jpeg,image/webp,image/svg+xml" style="display:none;" />
            <p class="table-cell-muted" style="margin-top: var(--space-1);">Se muestra en el cobro cuando el cajero elige este método.</p>
          </div>
        </div>
        `
            : ''
        }
        <div class="form-grid">
          <div class="field field-span-2">
            <label class="field-label" for="pm-name">Nombre a mostrar</label>
            <input class="input" id="pm-name" required maxlength="40" value="${metodo.name}" />
          </div>
          <div class="field field-span-2">
            <label class="field-label" for="pm-holder">Titular de la cuenta</label>
            <input class="input" id="pm-holder" maxlength="150" value="${metodo.accountHolder ?? ''}" />
          </div>
          <div class="field field-span-2">
            <label class="field-label" for="pm-number">Número / cuenta</label>
            <input class="input mono" id="pm-number" maxlength="30" value="${metodo.accountNumber ?? ''}" />
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="pm-form">Guardar</button>
    `,
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => closeModal());
  modal.body.querySelector('#pm-qr-input')?.addEventListener('change', async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    try {
      const actualizado = await api.post(`/payment-methods/${metodo.id}/qr`, formData);
      metodo.qrImageUrl = actualizado.qrImageUrl;
      const wrap = modal.body.querySelector('#pm-qr-preview-wrap');
      wrap.innerHTML = `<img id="pm-qr-preview" src="${API_ORIGIN}${actualizado.qrImageUrl}" alt="QR de pago" style="width:100%; height:100%; object-fit:contain;" />`;
      showToast({ type: 'success', title: 'Código QR actualizado' });
    } catch (error) {
      showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo subir el QR' });
    }
  });

  modal.body.querySelector('#pm-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#pm-form-error');
    try {
      await api.put(`/payment-methods/${metodo.id}`, {
        name: modal.body.querySelector('#pm-name').value.trim(),
        accountHolder: modal.body.querySelector('#pm-holder').value.trim() || null,
        accountNumber: modal.body.querySelector('#pm-number').value.trim() || null,
        qrImageUrl: metodo.qrImageUrl ?? null,
      });
      closeModal();
      showToast({ type: 'success', title: 'Método de pago actualizado' });
      cargarMetodosPago();
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo actualizar';
      errorAlert.hidden = false;
    }
  });
}

async function cambiarEstadoMetodoPago(id, currentStatus) {
  const nuevoEstado = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  try {
    await api.patch(`/payment-methods/${id}/status`, { status: nuevoEstado });
    showToast({ type: 'success', title: 'Estado actualizado' });
    cargarMetodosPago();
  } catch (error) {
    showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo actualizar' });
  }
}

const session = requireSession();
if (session) {
  renderShell('configuracion');
  init();
}
