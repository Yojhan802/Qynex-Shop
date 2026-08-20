import { api, ApiError } from '../core/api.js';
import { activeOnly, subcategoriesOf } from '../core/catalog.js';
import { openModal, closeModal } from './modal.js';
import { showToast } from './toast.js';

/**
 * Abre el modal de crear/editar producto. `producto` es null para crear.
 * `onSaved(productoActualizado)` se invoca tras guardar con éxito.
 */
export function openProductoForm({ catalog, producto = null, onSaved }) {
  const esEdicion = Boolean(producto);
  const categoriasActivas = activeOnly(catalog.categories);
  const marcasActivas = activeOnly(catalog.brands);

  const modal = openModal({
    title: esEdicion ? 'Editar producto' : 'Nuevo producto',
    subtitle: esEdicion ? producto.sku : 'El SKU y el código interno se generan automáticamente.',
    maxWidth: '620px',
    body: `
      <form id="producto-form" novalidate>
        <div class="alert alert-danger" id="producto-form-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="form-grid">
          <div class="field field-span-2">
            <label class="field-label" for="pf-name">Nombre</label>
            <input class="input" id="pf-name" required maxlength="150" value="${producto?.name ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="pf-category">Categoría</label>
            <select class="select" id="pf-category" required>
              <option value="">Selecciona…</option>
              ${categoriasActivas.map((c) => `<option value="${c.id}" ${producto?.categoryId === c.id ? 'selected' : ''}>${c.name}</option>`).join('')}
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="pf-subcategory">Subcategoría</label>
            <select class="select" id="pf-subcategory">
              <option value="">Ninguna</option>
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="pf-brand">Marca</label>
            <select class="select" id="pf-brand">
              <option value="">Ninguna</option>
              ${marcasActivas.map((b) => `<option value="${b.id}" ${producto?.brandId === b.id ? 'selected' : ''}>${b.name}</option>`).join('')}
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="pf-price">Precio (S/)</label>
            <input class="input" id="pf-price" type="number" min="0.01" step="0.01" required value="${producto?.price ?? ''}" />
          </div>
          <div class="field field-span-2">
            <label class="field-label" for="pf-promo-price">Precio promocional (S/)</label>
            <input class="input" id="pf-promo-price" type="number" min="0.01" step="0.01" value="${producto?.promoPrice ?? ''}" />
            <span class="field-hint">Opcional. Debe ser menor que el precio regular.</span>
          </div>
          <div class="field field-span-2">
            <label class="field-label" for="pf-description">Descripción</label>
            <textarea class="input" id="pf-description" rows="3" style="height:auto; padding-top:10px; resize:vertical;">${producto?.description ?? ''}</textarea>
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="producto-form" id="pf-submit">
        <span class="spinner" hidden></span>
        <span class="btn-label">${esEdicion ? 'Guardar cambios' : 'Crear producto'}</span>
      </button>
    `,
  });

  const categorySelect = modal.body.querySelector('#pf-category');
  const subcategorySelect = modal.body.querySelector('#pf-subcategory');

  function refrescarSubcategorias() {
    const subs = activeOnly(subcategoriesOf(catalog, categorySelect.value));
    subcategorySelect.innerHTML =
      '<option value="">Ninguna</option>' +
      subs.map((s) => `<option value="${s.id}" ${producto?.subcategoryId === s.id ? 'selected' : ''}>${s.name}</option>`).join('');
  }
  categorySelect.addEventListener('change', refrescarSubcategorias);
  refrescarSubcategorias();

  modal.footer.querySelector('[data-cancel]').addEventListener('click', closeModal);

  modal.body.querySelector('#producto-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#producto-form-error');
    errorAlert.hidden = true;

    const payload = {
      name: modal.body.querySelector('#pf-name').value.trim(),
      categoryId: Number(categorySelect.value) || null,
      subcategoryId: subcategorySelect.value ? Number(subcategorySelect.value) : null,
      brandId: modal.body.querySelector('#pf-brand').value ? Number(modal.body.querySelector('#pf-brand').value) : null,
      description: modal.body.querySelector('#pf-description').value.trim() || null,
      price: Number(modal.body.querySelector('#pf-price').value),
      promoPrice: modal.body.querySelector('#pf-promo-price').value ? Number(modal.body.querySelector('#pf-promo-price').value) : null,
    };

    if (!payload.categoryId) {
      errorAlert.querySelector('.alert-message').textContent = 'Selecciona una categoría.';
      errorAlert.hidden = false;
      return;
    }

    const submitBtn = modal.footer.querySelector('#pf-submit');
    submitBtn.disabled = true;
    submitBtn.querySelector('.spinner').hidden = false;

    try {
      const resultado = esEdicion
        ? await api.put(`/products/${producto.id}`, payload)
        : await api.post('/products', payload);
      closeModal();
      showToast({
        type: 'success',
        title: esEdicion ? 'Producto actualizado' : 'Producto creado',
        message: resultado.name,
      });
      onSaved?.(resultado);
    } catch (error) {
      const message = error instanceof ApiError ? error.message : 'No se pudo guardar el producto';
      errorAlert.querySelector('.alert-message').textContent = message;
      errorAlert.hidden = false;
      submitBtn.disabled = false;
      submitBtn.querySelector('.spinner').hidden = true;
    }
  });
}
