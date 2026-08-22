import { getCustomerSession, logoutCliente } from '../core/customer-auth.js';
import { cartCount } from '../core/cart.js';
import { fetchPublicBranding, getCachedPublicBranding, resolveLogoUrl } from '../../../../js/core/public-branding.js';

/**
 * Header/footer comunes de la tienda pública. `basePath` es la ruta relativa
 * hacia la raíz de tienda/ (vacío para páginas de primer nivel, '../' para
 * las de cuenta/), ya que este componente se usa desde ambos niveles.
 */
export function renderStoreShell({ basePath = '', active = '' } = {}) {
  const header = document.querySelector('#store-header');
  const footer = document.querySelector('#store-footer');
  const session = getCustomerSession();

  if (header) {
    header.innerHTML = `
      <a class="store-brand" href="${basePath}index.html">
        <img id="store-brand-logo" src="${basePath}../assets/brand/logo-mark-dark.png" alt="" />
        <span id="store-brand-name">Qynex</span>
      </a>
      <nav class="store-nav" aria-label="Navegación de la tienda">
        <a href="${basePath}index.html" ${active === 'catalogo' ? "aria-current='page'" : ''}>Catálogo</a>
        ${
          session
            ? `<a href="${basePath}cuenta/pedidos.html" ${active === 'pedidos' ? "aria-current='page'" : ''}>Mis pedidos</a>
               <a href="#" data-logout>Salir (${session.customer.fullName.split(' ')[0]})</a>`
            : `<a href="${basePath}cuenta/login.html">Ingresar</a>`
        }
        <a class="store-cart-link" href="${basePath}carrito.html" aria-label="Carrito">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="19" cy="21" r="1"/><path d="M2 3h2l2.4 12.4a2 2 0 002 1.6h8.7a2 2 0 002-1.6L21 8H6" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <span class="store-cart-count" id="store-cart-count">${cartCount()}</span>
        </a>
      </nav>
    `;
    header.querySelector('[data-logout]')?.addEventListener('click', (event) => {
      event.preventDefault();
      logoutCliente();
      window.location.href = `${basePath}index.html`;
    });
  }

  if (footer) {
    footer.innerHTML = `<p id="store-footer-text">© ${new Date().getFullYear()} Qynex. Todos los derechos reservados.</p>`;
  }

  aplicarMarcaTienda(getCachedPublicBranding());
  fetchPublicBranding().then(aplicarMarcaTienda);
}

function aplicarMarcaTienda(branding) {
  const nombreEl = document.querySelector('#store-brand-name');
  if (nombreEl) nombreEl.textContent = branding.name;

  const logoEl = document.querySelector('#store-brand-logo');
  const logoUrl = resolveLogoUrl(branding);
  if (logoEl && logoUrl) logoEl.src = logoUrl;
  if (logoEl) logoEl.alt = branding.name;

  const footerEl = document.querySelector('#store-footer-text');
  if (footerEl) footerEl.textContent = `© ${new Date().getFullYear()} ${branding.name}. Todos los derechos reservados.`;
}

export function actualizarContadorCarrito() {
  const badge = document.querySelector('#store-cart-count');
  if (badge) badge.textContent = String(cartCount());
}
