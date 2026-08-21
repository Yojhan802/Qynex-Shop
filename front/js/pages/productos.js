import { requireSession } from '../core/auth.js';
import { api, ApiError, API_ORIGIN } from '../core/api.js';
import { renderShell } from '../components/shell.js';
import { loadCatalog, activeOnly } from '../core/catalog.js';
import { renderPagination } from '../components/pagination.js';
import { statusBadge } from '../components/status-badge.js';
import { openProductoForm } from '../components/producto-form.js';
import { showToast } from '../components/toast.js';
import { formatCurrency } from '../core/format.js';
import { debounce } from '../core/debounce.js';

const session = requireSession();
if (session) {
  renderShell('productos');
  init();
}

let catalog = null;
let state = { page: 0, search: '', categoryId: '', brandId: '', status: '' };

async function init() {
  catalog = await loadCatalog();
  poblarFiltros();
  document.querySelector('#btn-nuevo-producto').addEventListener('click', () => {
    openProductoForm({ catalog, onSaved: () => cargarProductos() });
  });
  document.querySelector('#filter-search').addEventListener('input', debounce((event) => {
    state.search = event.target.value.trim();
    state.page = 0;
    cargarProductos();
  }, 350));
  document.querySelector('#filter-category').addEventListener('change', (event) => {
    state.categoryId = event.target.value;
    state.page = 0;
    cargarProductos();
  });
  document.querySelector('#filter-brand').addEventListener('change', (event) => {
    state.brandId = event.target.value;
    state.page = 0;
    cargarProductos();
  });
  document.querySelector('#filter-status').addEventListener('change', (event) => {
    state.status = event.target.value;
    state.page = 0;
    cargarProductos();
  });

  cargarProductos();
}

function poblarFiltros() {
  const categorySelect = document.querySelector('#filter-category');
  activeOnly(catalog.categories).forEach((c) => {
    categorySelect.insertAdjacentHTML('beforeend', `<option value="${c.id}">${c.name}</option>`);
  });
  const brandSelect = document.querySelector('#filter-brand');
  activeOnly(catalog.brands).forEach((b) => {
    brandSelect.insertAdjacentHTML('beforeend', `<option value="${b.id}">${b.name}</option>`);
  });
}

async function cargarProductos() {
  const body = document.querySelector('#products-body');
  try {
    const page = await api.get('/products', {
      query: {
        search: state.search || undefined,
        categoryId: state.categoryId || undefined,
        brandId: state.brandId || undefined,
        status: state.status || undefined,
        page: state.page,
        size: 20,
        sort: 'name,asc',
      },
    });

    if (page.content.length === 0) {
      body.innerHTML = `
        <tr><td colspan="8">
          <div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 8l8-4 8 4-8 4-8-4z"/><path d="M4 8v8l8 4 8-4V8"/></svg>
            <span>No se encontraron productos con estos filtros.</span>
          </div>
        </td></tr>
      `;
    } else {
      body.innerHTML = page.content.map(filaProducto).join('');
      body.querySelectorAll('tr[data-id]').forEach((row) => {
        row.addEventListener('click', (event) => {
          if (event.target.closest('[data-action]')) return;
          window.location.href = `producto-detalle.html?id=${row.dataset.id}`;
        });
      });
      body.querySelectorAll('[data-action="toggle-status"]').forEach((btn) => {
        btn.addEventListener('click', (event) => {
          event.stopPropagation();
          cambiarEstado(btn.dataset.id, btn.dataset.currentStatus);
        });
      });
    }

    renderPagination(document.querySelector('#pagination'), page, (nuevaPagina) => {
      state.page = nuevaPagina;
      cargarProductos();
    });
  } catch (error) {
    const message = error instanceof ApiError ? error.message : 'No se pudieron cargar los productos';
    body.innerHTML = `<tr><td colspan="8"><div class="empty-state"><span>${message}</span></div></td></tr>`;
  }
}

function filaProducto(p) {
  const precio = p.promoPrice
    ? `<span style="text-decoration:line-through;color:var(--color-text-muted);">${formatCurrency(p.price)}</span> ${formatCurrency(p.promoPrice)}`
    : formatCurrency(p.price);
  const stockClass = p.totalStock === 0 ? 'color:var(--color-danger-text);font-weight:600;' : '';
  const toggleLabel = p.status === 'ACTIVE' ? 'Desactivar' : 'Activar';
  return `
    <tr class="clickable" data-id="${p.id}">
      <td>
        <div class="table-row-with-thumb">
          <div class="table-thumb">
            ${p.imageUrl ? `<img src="${API_ORIGIN}${p.imageUrl}" alt="" />` : '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 8l8-4 8 4-8 4-8-4z"/><path d="M4 8v8l8 4 8-4V8"/></svg>'}
          </div>
          <div>
            <div class="table-cell-primary">${p.name}</div>
            <div class="table-cell-muted mono">${p.sku}</div>
          </div>
        </div>
      </td>
      <td>${p.categoryName ?? '—'}</td>
      <td>${p.brandName ?? '—'}</td>
      <td class="mono">${precio}</td>
      <td>${p.variantCount}</td>
      <td style="${stockClass}">${p.totalStock}</td>
      <td>${statusBadge(p.status)}</td>
      <td>
        <div class="table-actions">
          <button class="btn btn-ghost btn-sm" type="button" data-action="toggle-status" data-id="${p.id}" data-current-status="${p.status}">
            ${toggleLabel}
          </button>
        </div>
      </td>
    </tr>
  `;
}

async function cambiarEstado(id, currentStatus) {
  const nextStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  try {
    await api.patch(`/products/${id}/status`, { status: nextStatus });
    showToast({ type: 'success', title: 'Estado actualizado' });
    cargarProductos();
  } catch (error) {
    const message = error instanceof ApiError ? error.message : 'No se pudo cambiar el estado';
    showToast({ type: 'danger', title: 'Error', message });
  }
}
