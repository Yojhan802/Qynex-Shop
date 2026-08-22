import { requireSession, hasPermission } from '../core/auth.js';
import { api, ApiError } from '../core/api.js';
import { renderShell } from '../components/shell.js';
import { openModal, closeModal } from '../components/modal.js';
import { showToast } from '../components/toast.js';
import { formatCurrency, formatDateTime } from '../core/format.js';
import { debounce } from '../core/debounce.js';

const SCOPE_LABELS = { ALL: 'Todo el catálogo', CATEGORY: 'Una categoría', PRODUCT: 'Un producto' };

let promocionesCache = [];

function init() {
  document.querySelector('#btn-nueva-promocion').addEventListener('click', () => abrirFormularioPromocion(null));
  if (!hasPermission('PROMOCIONES_GESTIONAR')) {
    document.querySelector('#btn-nueva-promocion').hidden = true;
  }
  cargarPromociones();
}

function alcanceTexto(p) {
  if (p.scopeType === 'CATEGORY') return `Categoría: ${p.scopeCategoryName}`;
  if (p.scopeType === 'PRODUCT') return `Producto: ${p.scopeProductName}`;
  return SCOPE_LABELS.ALL;
}

function vigenciaTexto(p) {
  if (!p.startsAt && !p.endsAt) return 'Siempre';
  const desde = p.startsAt ? formatDateTime(p.startsAt) : '—';
  const hasta = p.endsAt ? formatDateTime(p.endsAt) : '—';
  return `${desde} → ${hasta}`;
}

async function cargarPromociones() {
  const body = document.querySelector('#promociones-body');
  try {
    promocionesCache = await api.get('/promotions');
    body.innerHTML = promocionesCache.length
      ? promocionesCache
          .map(
            (p) => `
        <tr>
          <td class="table-cell-primary mono">${p.code}</td>
          <td>${p.name}${p.visibleOnline ? ' <span class="badge badge-info" style="font-size:var(--font-size-xs);">Tienda online</span>' : ''}</td>
          <td class="mono">${p.discountType === 'PERCENTAGE' ? `${p.discountValue}%` : formatCurrency(p.discountValue)}</td>
          <td class="table-cell-muted">${alcanceTexto(p)}</td>
          <td class="table-cell-muted" style="font-size:var(--font-size-xs);">${vigenciaTexto(p)}</td>
          <td><span class="badge ${p.status === 'ACTIVE' ? 'badge-success' : 'badge-neutral'}">${p.status === 'ACTIVE' ? 'Activa' : 'Inactiva'}</span></td>
          <td>
            <div class="table-actions">
              <button class="btn btn-ghost btn-sm" type="button" data-editar="${p.id}">Editar</button>
              <button class="btn btn-ghost btn-sm" type="button" data-toggle="${p.id}" data-status="${p.status}">
                ${p.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
              </button>
            </div>
          </td>
        </tr>
      `
          )
          .join('')
      : `<tr><td colspan="7"><div class="empty-state"><span>Todavía no hay promociones creadas.</span></div></td></tr>`;

    body.querySelectorAll('[data-editar]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const promo = promocionesCache.find((p) => String(p.id) === btn.dataset.editar);
        abrirFormularioPromocion(promo);
      });
    });
    body.querySelectorAll('[data-toggle]').forEach((btn) => {
      btn.addEventListener('click', () => cambiarEstado(btn.dataset.toggle, btn.dataset.status));
    });
  } catch (error) {
    body.innerHTML = `<tr><td colspan="7"><div class="empty-state"><span>${error instanceof ApiError ? error.message : 'Error al cargar las promociones'}</span></div></td></tr>`;
  }
}

async function cambiarEstado(id, currentStatus) {
  const nuevoEstado = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  try {
    await api.patch(`/promotions/${id}/status`, { status: nuevoEstado });
    showToast({ type: 'success', title: 'Estado actualizado' });
    cargarPromociones();
  } catch (error) {
    showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo actualizar' });
  }
}

async function abrirFormularioPromocion(promo) {
  const esEdicion = Boolean(promo);
  const categorias = await api.get('/categories').catch(() => []);
  let productoSeleccionado = promo?.scopeProductId
    ? { id: promo.scopeProductId, name: promo.scopeProductName }
    : null;

  const aFechaLocal = (iso) => (iso ? iso.slice(0, 16) : '');

  const modal = openModal({
    title: esEdicion ? 'Editar promoción' : 'Nueva promoción',
    maxWidth: '520px',
    body: `
      <form id="promo-form" novalidate>
        <div class="alert alert-danger" id="promo-form-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="form-grid">
          <div class="field">
            <label class="field-label" for="pf-code">Código</label>
            <input class="input mono" id="pf-code" maxlength="30" required value="${promo?.code ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="pf-name">Nombre</label>
            <input class="input" id="pf-name" maxlength="150" required value="${promo?.name ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="pf-type">Tipo de descuento</label>
            <select class="select" id="pf-type">
              <option value="PERCENTAGE" ${promo?.discountType === 'PERCENTAGE' ? 'selected' : ''}>Porcentaje (%)</option>
              <option value="FIXED_AMOUNT" ${promo?.discountType === 'FIXED_AMOUNT' ? 'selected' : ''}>Monto fijo (S/)</option>
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="pf-value">Valor</label>
            <input class="input" type="number" id="pf-value" min="0.01" step="0.01" required value="${promo?.discountValue ?? ''}" />
          </div>
          <div class="field field-span-2">
            <label class="field-label" for="pf-scope">Alcance</label>
            <select class="select" id="pf-scope">
              <option value="ALL" ${!promo || promo.scopeType === 'ALL' ? 'selected' : ''}>Todo el catálogo</option>
              <option value="CATEGORY" ${promo?.scopeType === 'CATEGORY' ? 'selected' : ''}>Una categoría</option>
              <option value="PRODUCT" ${promo?.scopeType === 'PRODUCT' ? 'selected' : ''}>Un producto específico</option>
            </select>
          </div>
          <div class="field field-span-2" id="pf-category-field" style="display:${promo?.scopeType === 'CATEGORY' ? '' : 'none'};">
            <label class="field-label" for="pf-category">Categoría</label>
            <select class="select" id="pf-category">
              ${categorias.map((c) => `<option value="${c.id}" ${promo?.scopeCategoryId === c.id ? 'selected' : ''}>${c.name}</option>`).join('')}
            </select>
          </div>
          <div class="field field-span-2" id="pf-product-field" style="display:${promo?.scopeType === 'PRODUCT' ? '' : 'none'}; position:relative;">
            <label class="field-label" for="pf-product-search">Producto</label>
            <input class="input" id="pf-product-search" placeholder="Buscar producto…" autocomplete="off" value="${productoSeleccionado?.name ?? ''}" />
            <div id="pf-product-results" style="display:none; position:absolute; top:100%; left:0; right:0; z-index:20;
              background:var(--color-surface); border:1px solid var(--color-border); border-radius:var(--radius-md);
              box-shadow:var(--shadow-md); max-height:200px; overflow-y:auto;"></div>
          </div>
          <div class="field">
            <label class="field-label" for="pf-starts">Empieza (opcional)</label>
            <input class="input" type="datetime-local" id="pf-starts" value="${aFechaLocal(promo?.startsAt)}" />
          </div>
          <div class="field">
            <label class="field-label" for="pf-ends">Termina (opcional)</label>
            <input class="input" type="datetime-local" id="pf-ends" value="${aFechaLocal(promo?.endsAt)}" />
          </div>
          <div class="field field-span-2">
            <label class="checkbox-field">
              <input type="checkbox" id="pf-visible-online" ${promo?.visibleOnline ? 'checked' : ''} />
              Mostrar y aplicar automáticamente en la tienda online (ej. Black Friday)
            </label>
            <span class="field-hint">Si no la marcas, solo el cajero puede aplicarla en el POS.</span>
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="promo-form">${esEdicion ? 'Guardar cambios' : 'Crear promoción'}</button>
    `,
  });

  const scopeSelect = modal.body.querySelector('#pf-scope');
  const categoryField = modal.body.querySelector('#pf-category-field');
  const productField = modal.body.querySelector('#pf-product-field');
  scopeSelect.addEventListener('change', () => {
    categoryField.style.display = scopeSelect.value === 'CATEGORY' ? '' : 'none';
    productField.style.display = scopeSelect.value === 'PRODUCT' ? '' : 'none';
  });

  const productSearch = modal.body.querySelector('#pf-product-search');
  const productResults = modal.body.querySelector('#pf-product-results');
  productSearch.addEventListener('input', debounce(async () => {
    const q = productSearch.value.trim();
    if (q.length < 2) {
      productResults.style.display = 'none';
      return;
    }
    try {
      const page = await api.get('/products', { query: { search: q, size: 10 } });
      productResults.innerHTML = page.content.length
        ? page.content
            .map((p) => `<button type="button" class="vp-result" data-product="${p.id}" data-name="${p.name}" style="display:block; width:100%; text-align:left; padding:var(--space-3); border-bottom:1px solid var(--color-border);">${p.name}</button>`)
            .join('')
        : `<div style="padding:var(--space-3); color:var(--color-text-muted); font-size:var(--font-size-sm);">Sin resultados</div>`;
      productResults.querySelectorAll('[data-product]').forEach((btn) => {
        btn.addEventListener('click', () => {
          productoSeleccionado = { id: Number(btn.dataset.product), name: btn.dataset.name };
          productSearch.value = btn.dataset.name;
          productResults.style.display = 'none';
        });
      });
      productResults.style.display = 'block';
    } catch {
      productResults.style.display = 'none';
    }
  }, 300));
  document.addEventListener('click', (event) => {
    if (!event.target.closest('#pf-product-search') && !event.target.closest('#pf-product-results')) productResults.style.display = 'none';
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', closeModal);
  modal.body.querySelector('#promo-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#promo-form-error');
    errorAlert.hidden = true;

    const scopeType = scopeSelect.value;
    if (scopeType === 'PRODUCT' && !productoSeleccionado) {
      errorAlert.querySelector('.alert-message').textContent = 'Busca y elige el producto al que aplica.';
      errorAlert.hidden = false;
      return;
    }

    const payload = {
      code: modal.body.querySelector('#pf-code').value.trim().toUpperCase(),
      name: modal.body.querySelector('#pf-name').value.trim(),
      discountType: modal.body.querySelector('#pf-type').value,
      discountValue: Number(modal.body.querySelector('#pf-value').value),
      scopeType,
      scopeCategoryId: scopeType === 'CATEGORY' ? Number(modal.body.querySelector('#pf-category').value) : null,
      scopeProductId: scopeType === 'PRODUCT' ? productoSeleccionado.id : null,
      startsAt: modal.body.querySelector('#pf-starts').value || null,
      endsAt: modal.body.querySelector('#pf-ends').value || null,
      visibleOnline: modal.body.querySelector('#pf-visible-online').checked,
    };

    try {
      if (esEdicion) {
        await api.put(`/promotions/${promo.id}`, payload);
      } else {
        await api.post('/promotions', payload);
      }
      closeModal();
      showToast({ type: 'success', title: esEdicion ? 'Promoción actualizada' : 'Promoción creada' });
      cargarPromociones();
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo guardar la promoción';
      errorAlert.hidden = false;
    }
  });
}

const session = requireSession();
if (session) {
  renderShell('promociones');
  init();
}
