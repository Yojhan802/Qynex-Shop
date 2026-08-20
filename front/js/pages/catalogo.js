import { requireSession } from '../core/auth.js';
import { api, ApiError } from '../core/api.js';
import { renderShell } from '../components/shell.js';
import { openModal, closeModal } from '../components/modal.js';
import { statusBadge } from '../components/status-badge.js';
import { showToast } from '../components/toast.js';
import { loadCatalog, activeOnly } from '../core/catalog.js';

let catalog = null;
let activeTab = 'categorias';

const TABS = {
  categorias: {
    label: 'Categoría',
    endpoint: '/categories',
    items: () => catalog.categories,
    headers: ['Nombre', 'Estado', ''],
    row: (item) => [item.name, statusBadge(item.status)],
    fields: [{ id: 'name', label: 'Nombre', type: 'text', maxlength: 80 }],
    toRequest: (v) => ({ name: v.name }),
  },
  subcategorias: {
    label: 'Subcategoría',
    endpoint: '/subcategories',
    items: () => catalog.subcategories,
    headers: ['Categoría', 'Subcategoría', 'Estado', ''],
    row: (item) => [item.categoryName, item.name, statusBadge(item.status)],
    fields: [
      { id: 'categoryId', label: 'Categoría', type: 'select', options: () => activeOnly(catalog.categories).map((c) => ({ value: c.id, label: c.name })) },
      { id: 'name', label: 'Nombre', type: 'text', maxlength: 80 },
    ],
    toRequest: (v) => ({ categoryId: Number(v.categoryId), name: v.name }),
  },
  marcas: {
    label: 'Marca',
    endpoint: '/brands',
    items: () => catalog.brands,
    headers: ['Nombre', 'Estado', ''],
    row: (item) => [item.name, statusBadge(item.status)],
    fields: [{ id: 'name', label: 'Nombre', type: 'text', maxlength: 80 }],
    toRequest: (v) => ({ name: v.name }),
  },
  colores: {
    label: 'Color',
    endpoint: '/colors',
    items: () => catalog.colors,
    headers: ['', 'Nombre', 'Código', 'Estado', ''],
    row: (item) => [
      `<span style="display:inline-block; width:20px; height:20px; border-radius:var(--radius-sm); border:1px solid var(--color-border); background:${item.hexCode};"></span>`,
      item.name,
      item.hexCode,
      statusBadge(item.status),
    ],
    fields: [
      { id: 'name', label: 'Nombre', type: 'text', maxlength: 40 },
      { id: 'hexCode', label: 'Color', type: 'color', default: '#000000' },
    ],
    toRequest: (v) => ({ name: v.name, hexCode: v.hexCode }),
  },
  tallas: {
    label: 'Talla',
    endpoint: '/sizes',
    items: () => catalog.sizes,
    headers: ['Nombre', 'Orden', 'Estado', ''],
    row: (item) => [item.name, item.sortOrder, statusBadge(item.status)],
    fields: [
      { id: 'name', label: 'Nombre', type: 'text', maxlength: 20 },
      { id: 'sortOrder', label: 'Orden', type: 'number', default: 1 },
    ],
    toRequest: (v) => ({ name: v.name, sortOrder: Number(v.sortOrder) }),
  },
};

async function init() {
  document.querySelectorAll('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      activeTab = tab.dataset.tab;
      document.querySelectorAll('.tab').forEach((t) => t.setAttribute('aria-selected', String(t.dataset.tab === activeTab)));
      document.querySelector('#btn-nuevo-item-label').textContent = `Nueva ${TABS[activeTab].label.toLowerCase()}`;
      renderTabla();
    });
  });
  document.querySelector('#btn-nuevo-item').addEventListener('click', () => abrirFormulario(null));

  await cargarCatalogo();
  renderTabla();
}

async function cargarCatalogo() {
  catalog = await loadCatalog({ force: true });
}

function renderTabla() {
  const config = TABS[activeTab];
  const head = document.querySelector('#catalogo-head');
  const body = document.querySelector('#catalogo-body');

  head.innerHTML = `<tr>${config.headers.map((h) => `<th>${h}</th>`).join('')}</tr>`;

  const items = config.items();
  body.innerHTML = items.length
    ? items
        .map(
          (item) => `
      <tr>
        ${config.row(item).map((cell) => `<td>${cell}</td>`).join('')}
        <td>
          <div class="table-actions">
            <button class="btn btn-ghost btn-sm" type="button" data-editar="${item.id}">Editar</button>
            <button class="btn btn-ghost btn-sm" type="button" data-toggle="${item.id}" data-status="${item.status}">
              ${item.status === 'ACTIVE' ? 'Desactivar' : 'Activar'}
            </button>
          </div>
        </td>
      </tr>
    `
        )
        .join('')
    : `<tr><td colspan="${config.headers.length + 1}"><div class="empty-state"><span>Todavía no hay ${config.label.toLowerCase()}s registradas.</span></div></td></tr>`;

  body.querySelectorAll('[data-editar]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const item = items.find((i) => String(i.id) === btn.dataset.editar);
      abrirFormulario(item);
    });
  });
  body.querySelectorAll('[data-toggle]').forEach((btn) => {
    btn.addEventListener('click', () => cambiarEstado(btn.dataset.toggle, btn.dataset.status));
  });
}

function abrirFormulario(item) {
  const config = TABS[activeTab];
  const esEdicion = Boolean(item);

  const modal = openModal({
    title: esEdicion ? `Editar ${config.label.toLowerCase()}` : `Nueva ${config.label.toLowerCase()}`,
    maxWidth: '420px',
    body: `
      <form id="cat-form" novalidate>
        <div class="alert alert-danger" id="cat-form-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="form-grid">
          ${config.fields.map((field) => campoHtml(field, item)).join('')}
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="cat-form">${esEdicion ? 'Guardar cambios' : 'Crear'}</button>
    `,
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', () => closeModal());
  modal.body.querySelector('#cat-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#cat-form-error');
    const values = {};
    config.fields.forEach((field) => {
      values[field.id] = modal.body.querySelector(`#cf-${field.id}`).value.trim();
    });

    try {
      if (esEdicion) {
        await api.put(`${config.endpoint}/${item.id}`, config.toRequest(values));
      } else {
        await api.post(config.endpoint, config.toRequest(values));
      }
      closeModal();
      showToast({ type: 'success', title: esEdicion ? `${config.label} actualizada` : `${config.label} creada` });
      await cargarCatalogo();
      renderTabla();
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo guardar';
      errorAlert.hidden = false;
    }
  });
}

function campoHtml(field, item) {
  const valorActual = item ? item[field.id] : (field.default ?? '');
  if (field.type === 'select') {
    const opciones = field.options();
    return `
      <div class="field field-span-2">
        <label class="field-label" for="cf-${field.id}">${field.label}</label>
        <select class="select" id="cf-${field.id}" required>
          ${opciones.map((o) => `<option value="${o.value}" ${String(o.value) === String(valorActual) ? 'selected' : ''}>${o.label}</option>`).join('')}
        </select>
      </div>
    `;
  }
  if (field.type === 'color') {
    return `
      <div class="field field-span-2">
        <label class="field-label" for="cf-${field.id}">${field.label}</label>
        <input class="input" type="color" id="cf-${field.id}" value="${valorActual || '#000000'}" style="height:44px; padding:4px;" required />
      </div>
    `;
  }
  return `
    <div class="field field-span-2">
      <label class="field-label" for="cf-${field.id}">${field.label}</label>
      <input class="input" type="${field.type}" id="cf-${field.id}" maxlength="${field.maxlength ?? ''}" value="${valorActual}" required />
    </div>
  `;
}

async function cambiarEstado(id, currentStatus) {
  const config = TABS[activeTab];
  const nuevoEstado = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  try {
    await api.patch(`${config.endpoint}/${id}/status`, { status: nuevoEstado });
    showToast({ type: 'success', title: 'Estado actualizado' });
    await cargarCatalogo();
    renderTabla();
  } catch (error) {
    showToast({ type: 'danger', title: 'Error', message: error instanceof ApiError ? error.message : 'No se pudo actualizar' });
  }
}

const session = requireSession();
if (session) {
  renderShell('productos');
  init();
}
