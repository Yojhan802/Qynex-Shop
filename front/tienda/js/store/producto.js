import { storeApi, ApiError, API_ORIGIN } from './core/store-api.js';
import { renderStoreShell, actualizarContadorCarrito } from './components/store-shell.js';
import { addToCart } from './core/cart.js';
import { formatCurrency } from '../../../js/core/format.js';
import { showToast } from '../../../js/components/toast.js';

const productId = new URLSearchParams(window.location.search).get('id');
let producto = null;
let selectedColorId = null;
let selectedVariantId = null;

function placeholderImage() {
  return `data:image/svg+xml;utf8,${encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400"><rect width="400" height="400" fill="#f1f4f9"/></svg>'
  )}`;
}

function coloresUnicos() {
  const vistos = new Map();
  producto.variants.forEach((v) => {
    if (!vistos.has(v.colorId)) vistos.set(v.colorId, { colorId: v.colorId, colorName: v.colorName, colorHex: v.colorHex });
  });
  return [...vistos.values()];
}

function render() {
  const imageUrl = producto.imageUrl ? `${API_ORIGIN}${producto.imageUrl}` : placeholderImage();
  const tieneDescuento = producto.promoPrice != null;
  const colores = coloresUnicos();

  document.querySelector('#product-detail').innerHTML = `
    <div class="store-detail">
      <div class="store-product-image" style="border-radius: var(--radius-lg);">
        <img src="${imageUrl}" alt="${producto.name}" style="width:100%; height:100%; object-fit:cover;" />
      </div>
      <div>
        <span class="store-product-meta">${producto.brandName ?? producto.categoryName}</span>
        <h1 style="margin: var(--space-2) 0;">${producto.name}</h1>
        <div class="store-product-price" style="font-size: var(--font-size-xl); margin-bottom: var(--space-4);">
          ${tieneDescuento ? `<span class="price-old">${formatCurrency(producto.price)}</span>` : ''}
          <span>${formatCurrency(tieneDescuento ? producto.promoPrice : producto.price)}</span>
        </div>
        ${producto.description ? `<p style="margin-bottom: var(--space-4);">${producto.description}</p>` : ''}

        ${
          producto.material || producto.fit
            ? `
        <div style="margin-bottom: var(--space-5); display:flex; flex-direction:column; gap:4px; font-size: var(--font-size-sm);">
          ${producto.material ? `<div><strong>Material:</strong> ${producto.material}</div>` : ''}
          ${producto.fit ? `<div><strong>Calce:</strong> ${producto.fit}</div>` : ''}
        </div>
        `
            : ''
        }

        <div class="field" style="margin-bottom: var(--space-4);">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <span class="field-label">Color</span>
            ${producto.sizeGuideImageUrl ? `<a href="${API_ORIGIN}${producto.sizeGuideImageUrl}" target="_blank" rel="noopener" style="font-size: var(--font-size-xs); color: var(--brand-accent); text-decoration:underline;">Guía de tallas</a>` : ''}
          </div>
          <div class="store-swatches" id="color-swatches">
            ${colores
              .map(
                (c) => `
              <button type="button" class="store-swatch" data-color-id="${c.colorId}" aria-pressed="false">
                ${c.colorHex ? `<span class="store-swatch-dot" style="background:${c.colorHex};"></span>` : ''}
                ${c.colorName}
              </button>
            `
              )
              .join('')}
          </div>
        </div>

        <div class="field" style="margin-bottom: var(--space-5);">
          <span class="field-label">Talla</span>
          <div class="store-swatches" id="size-swatches">
            <span class="field-hint">Elige un color primero</span>
          </div>
        </div>

        <div style="display:flex; align-items:center; gap: var(--space-3);">
          <input type="number" class="input" id="quantity" value="1" min="1" max="10" style="width:80px;" />
          <button class="btn btn-primary btn-lg" type="button" id="btn-add-cart" disabled>Agregar al carrito</button>
        </div>
      </div>
    </div>
  `;

  document.querySelector('#color-swatches').addEventListener('click', (event) => {
    const btn = event.target.closest('[data-color-id]');
    if (!btn) return;
    selectedColorId = Number(btn.dataset.colorId);
    selectedVariantId = null;
    document.querySelectorAll('#color-swatches [data-color-id]').forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
    renderTallas();
  });

  document.querySelector('#btn-add-cart').addEventListener('click', agregarAlCarrito);
}

function renderTallas() {
  const contenedor = document.querySelector('#size-swatches');
  const tallas = producto.variants.filter((v) => v.colorId === selectedColorId);
  contenedor.innerHTML = tallas
    .map(
      (v) => `
    <button type="button" class="store-swatch" data-variant-id="${v.variantId}" aria-pressed="false" ${v.inStock ? '' : 'disabled'}>
      ${v.sizeName}
    </button>
  `
    )
    .join('');

  contenedor.addEventListener('click', (event) => {
    const btn = event.target.closest('[data-variant-id]');
    if (!btn) return;
    selectedVariantId = Number(btn.dataset.variantId);
    contenedor.querySelectorAll('[data-variant-id]').forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
    document.querySelector('#btn-add-cart').disabled = false;
  });
}

function agregarAlCarrito() {
  const variante = producto.variants.find((v) => v.variantId === selectedVariantId);
  if (!variante) return;
  const quantity = Math.max(1, Number(document.querySelector('#quantity').value) || 1);

  addToCart(
    {
      variantId: variante.variantId,
      productId: producto.id,
      productName: producto.name,
      colorName: variante.colorName,
      sizeName: variante.sizeName,
      unitPrice: producto.promoPrice ?? producto.price,
      imageUrl: producto.imageUrl,
    },
    quantity
  );
  actualizarContadorCarrito();
  showToast({ type: 'success', title: 'Agregado al carrito', message: `${producto.name} (${variante.colorName} / ${variante.sizeName})` });
}

async function init() {
  if (!productId) {
    document.querySelector('#product-detail').innerHTML = `<div class="empty-state"><span>Producto no encontrado.</span></div>`;
    return;
  }
  try {
    producto = await storeApi.get(`/store/catalog/products/${productId}`);
    render();
  } catch (error) {
    document.querySelector('#product-detail').innerHTML = `<div class="empty-state"><span>${error instanceof ApiError ? error.message : 'No se pudo cargar el producto'}</span></div>`;
  }
}

renderStoreShell();
init();
