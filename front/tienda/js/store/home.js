import { storeApi, ApiError, API_ORIGIN } from './core/store-api.js';
import { renderStoreShell } from './components/store-shell.js';
import { formatCurrency } from '../../../js/core/format.js';
import { debounce } from '../../../js/core/debounce.js';
import { renderPagination } from '../../../js/components/pagination.js';

const PRODUCTOS_POR_SECCION = 8;
const MAX_BANNERS = 6;

const params = new URLSearchParams(window.location.search);
let state = {
  page: 0,
  search: params.get('search') ?? '',
  categoryId: params.get('categoryId') ?? '',
  brandId: params.get('brandId') ?? '',
};
let categorias = [];

function hayFiltroActivo() {
  return Boolean(state.search || state.categoryId || state.brandId);
}

function placeholderImage() {
  return `data:image/svg+xml;utf8,${encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="200" height="200" fill="#f1f4f9"/></svg>'
  )}`;
}

function swatchesHtml(colors) {
  if (!colors?.length) return '';
  return `
    <div class="store-card-swatches">
      ${colors.map((c) => `<span class="store-card-swatch" style="background:${c.hexCode || '#ccc'};" title="${c.name}"></span>`).join('')}
    </div>
  `;
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
        ${swatchesHtml(p.colors)}
        ${!p.inStock ? '<span class="badge badge-neutral">Agotado</span>' : ''}
      </div>
    </a>
  `;
}

async function cargarFiltros() {
  try {
    const [cats, marcas] = await Promise.all([
      storeApi.get('/store/catalog/categories'),
      storeApi.get('/store/catalog/brands'),
    ]);
    categorias = cats;
    const catSelect = document.querySelector('#filter-category');
    categorias.forEach((c) => catSelect.insertAdjacentHTML('beforeend', `<option value="${c.id}">${c.name}</option>`));
    catSelect.value = state.categoryId;

    const brandSelect = document.querySelector('#filter-brand');
    marcas.forEach((b) => brandSelect.insertAdjacentHTML('beforeend', `<option value="${b.id}">${b.name}</option>`));
    brandSelect.value = state.brandId;
  } catch {
    // Filtros son un extra — si fallan, el catálogo sigue navegable sin ellos.
  }
}

async function cargarProductosPlano() {
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
      cargarProductosPlano();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  } catch (error) {
    grid.innerHTML = `<div class="empty-state" style="grid-column: 1 / -1;"><span>${error instanceof ApiError ? error.message : 'No se pudo cargar el catálogo'}</span></div>`;
  }
}

async function cargarSecciones() {
  const contenedor = document.querySelector('#catalog-sections');
  try {
    const paginas = await Promise.all(
      categorias.map((c) => storeApi.get('/store/catalog/products', { query: { categoryId: c.id, size: PRODUCTOS_POR_SECCION } }))
    );

    const secciones = categorias
      .map((c, i) => ({ categoria: c, page: paginas[i] }))
      .filter(({ page }) => page.totalElements > 0);

    contenedor.innerHTML = secciones.length
      ? secciones
          .map(
            ({ categoria, page }) => `
        <section class="store-section">
          <div class="store-section-header">
            <h2>${categoria.name}</h2>
            <a href="index.html?categoryId=${categoria.id}">Ver todo →</a>
          </div>
          <div class="store-grid">${page.content.map(productCard).join('')}</div>
        </section>
      `
          )
          .join('')
      : `<div class="empty-state"><span>Todavía no hay productos publicados.</span></div>`;

    renderBanners(secciones);
  } catch (error) {
    contenedor.innerHTML = `<div class="empty-state"><span>${error instanceof ApiError ? error.message : 'No se pudo cargar el catálogo'}</span></div>`;
  }
}

function renderBanners(secciones) {
  const contenedor = document.querySelector('#category-banners');
  const destacadas = secciones.filter(({ page }) => page.content[0]?.imageUrl).slice(0, MAX_BANNERS);

  if (!destacadas.length) {
    contenedor.hidden = true;
    return;
  }
  contenedor.hidden = false;
  contenedor.innerHTML = destacadas
    .map(
      ({ categoria, page }) => `
    <a class="store-category-banner" href="index.html?categoryId=${categoria.id}">
      <img src="${API_ORIGIN}${page.content[0].imageUrl}" alt="" loading="lazy" />
      <span>${categoria.name}</span>
    </a>
  `
    )
    .join('');
}

function render() {
  const seccionesEl = document.querySelector('#catalog-sections');
  const planoEl = document.querySelector('#catalog-flat');
  const bannersEl = document.querySelector('#category-banners');

  if (hayFiltroActivo()) {
    seccionesEl.hidden = true;
    bannersEl.hidden = true;
    planoEl.hidden = false;
    cargarProductosPlano();
  } else {
    planoEl.hidden = true;
    seccionesEl.hidden = false;
    cargarSecciones();
  }
}

async function init() {
  await cargarFiltros();

  document.querySelector('#filter-search').value = state.search;
  document.querySelector('#filter-search').addEventListener('input', debounce((event) => {
    state.search = event.target.value.trim();
    state.page = 0;
    render();
  }, 350));
  document.querySelector('#filter-category').addEventListener('change', (event) => {
    state.categoryId = event.target.value;
    state.page = 0;
    render();
  });
  document.querySelector('#filter-brand').addEventListener('change', (event) => {
    state.brandId = event.target.value;
    state.page = 0;
    render();
  });

  render();
}

renderStoreShell({ active: 'catalogo' });
init();
