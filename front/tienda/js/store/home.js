import { storeApi, ApiError, API_ORIGIN } from './core/store-api.js';
import { renderStoreShell } from './components/store-shell.js';
import { formatCurrency } from '../../../js/core/format.js';
import { debounce } from '../../../js/core/debounce.js';
import { renderPagination } from '../../../js/components/pagination.js';

let state = { page: 0, search: '', categoryId: '', brandId: '' };

function placeholderImage() {
  return `data:image/svg+xml;utf8,${encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="200" height="200" fill="#f1f4f9"/></svg>'
  )}`;
}

function productCard(p) {
  const imageUrl = p.imageUrl ? `${API_ORIGIN}${p.imageUrl}` : placeholderImage();
  const tieneDescuento = p.promoPrice != null;
  return `
    <a class="store-product-card" href="producto.html?id=${p.id}">
      <div class="store-product-image"><img src="${imageUrl}" alt="${p.name}" loading="lazy" /></div>
      <div class="store-product-body">
        <span class="store-product-meta">${p.brandName ?? p.categoryName}</span>
        <span class="store-product-name">${p.name}</span>
        <span class="store-product-price">
          ${tieneDescuento ? `<span class="price-old">${formatCurrency(p.price)}</span>` : ''}
          <span>${formatCurrency(tieneDescuento ? p.promoPrice : p.price)}</span>
        </span>
        ${!p.inStock ? '<span class="badge badge-neutral">Agotado</span>' : ''}
      </div>
    </a>
  `;
}

async function cargarFiltros() {
  try {
    const [categorias, marcas] = await Promise.all([
      storeApi.get('/store/catalog/categories'),
      storeApi.get('/store/catalog/brands'),
    ]);
    const catSelect = document.querySelector('#filter-category');
    categorias.forEach((c) => catSelect.insertAdjacentHTML('beforeend', `<option value="${c.id}">${c.name}</option>`));
    const brandSelect = document.querySelector('#filter-brand');
    marcas.forEach((b) => brandSelect.insertAdjacentHTML('beforeend', `<option value="${b.id}">${b.name}</option>`));
  } catch {
    // Filtros son un extra — si fallan, el catálogo sigue navegable sin ellos.
  }
}

async function cargarProductos() {
  const grid = document.querySelector('#product-grid');
  try {
    const page = await storeApi.get('/store/catalog/products', {
      query: {
        search: state.search || undefined,
        categoryId: state.categoryId || undefined,
        brandId: state.brandId || undefined,
        page: state.page,
        size: 12,
      },
    });

    grid.innerHTML = page.content.length
      ? page.content.map(productCard).join('')
      : `<div class="empty-state" style="grid-column: 1 / -1;"><span>No se encontraron productos.</span></div>`;

    renderPagination(document.querySelector('#pagination'), page, (p) => {
      state.page = p;
      cargarProductos();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  } catch (error) {
    grid.innerHTML = `<div class="empty-state" style="grid-column: 1 / -1;"><span>${error instanceof ApiError ? error.message : 'No se pudo cargar el catálogo'}</span></div>`;
  }
}

function init() {
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
  cargarFiltros();
  cargarProductos();
}

renderStoreShell({ active: 'catalogo' });
init();
