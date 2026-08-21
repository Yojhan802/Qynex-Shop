import { getSession, logout, initials } from '../core/auth.js';
import { initSidebar } from './sidebar.js';
import { fetchCurrentSession } from '../core/cash-session.js';
import { openAbrirCajaModal } from './abrir-caja.js';
import { fetchCompanySettings, getCachedCompanySettings } from '../core/settings.js';
import { api, ApiError, API_ORIGIN } from '../core/api.js';
import { debounce } from '../core/debounce.js';

const GROUP_LABELS = { products: 'Productos', customers: 'Clientes', sales: 'Ventas', users: 'Usuarios' };

const ICONS = {
  dashboard: '<path d="M4 13h6V4H4v9zm0 7h6v-5H4v5zm10 0h6V11h-6v9zm0-16v5h6V4h-6z"/>',
  products: '<path d="M4 8l8-4 8 4-8 4-8-4z"/><path d="M4 8v8l8 4 8-4V8"/><path d="M12 12v8"/>',
  inventory: '<rect x="4" y="7" width="16" height="13" rx="1.5"/><path d="M8 7V5.5A2.5 2.5 0 0110.5 3h3A2.5 2.5 0 0116 5.5V7"/>',
  sales: '<circle cx="9" cy="19" r="1.6"/><circle cx="17" cy="19" r="1.6"/><path d="M3 4h2l2.2 11.2a2 2 0 002 1.6h7.6a2 2 0 002-1.6L21 8H6"/>',
  cash: '<rect x="3" y="6" width="18" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/><path d="M7 6V5a2 2 0 012-2h6a2 2 0 012 2v1"/>',
  customers: '<circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6"/><path d="M16 5.2c1.5.4 2.6 1.8 2.6 3.4S17.5 11.6 16 12"/><path d="M18.5 14.3c1.9.7 3.2 2.6 3.2 5.7"/>',
  orders: '<rect x="5" y="4" width="14" height="17" rx="1.5"/><path d="M9 3h6v3H9z"/><path d="M8 11h8M8 15h5"/>',
  reports: '<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/>',
  audit: '<path d="M9 3h6l1 3h3a1 1 0 011 1v13a1 1 0 01-1 1H5a1 1 0 01-1-1V7a1 1 0 011-1h3l1-3z"/><path d="M9 12l2 2 4-4"/>',
  users: '<circle cx="8" cy="8" r="3"/><circle cx="17" cy="9" r="2.4"/><path d="M2.5 20c0-3 2.5-5.4 5.5-5.4S13.5 17 13.5 20"/><path d="M15 15.2c2.4.3 4.5 2.2 4.5 4.8"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 13a1.7 1.7 0 00.3 1.9l.1.1a2 2 0 11-2.9 2.9l-.1-.1a1.7 1.7 0 00-1.9-.3 1.7 1.7 0 00-1 1.6V19a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1-1.6 1.7 1.7 0 00-1.9.3l-.1.1a2 2 0 11-2.9-2.9l.1-.1a1.7 1.7 0 00.3-1.9 1.7 1.7 0 00-1.6-1H4a2 2 0 110-4h.1a1.7 1.7 0 001.6-1 1.7 1.7 0 00-.3-1.9l-.1-.1a2 2 0 112.9-2.9l.1.1a1.7 1.7 0 001.9.3H10a1.7 1.7 0 001-1.6V4a2 2 0 114 0v.1a1.7 1.7 0 001 1.6 1.7 1.7 0 001.9-.3l.1-.1a2 2 0 112.9 2.9l-.1.1a1.7 1.7 0 00-.3 1.9V10a1.7 1.7 0 001.6 1H20a2 2 0 110 4h-.1a1.7 1.7 0 00-1.6 1z"/>',
  collapse: '<path d="M9 4v16M4 4h16v16H4V4z"/><path d="M14 10l-2 2 2 2" stroke-linecap="round" stroke-linejoin="round"/>',
};

function icon(name, size = 20) {
  return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">${ICONS[name] || ''}</svg>`;
}

const NAV_SECTIONS = [
  {
    items: [
      { id: 'dashboard', label: 'Dashboard', href: 'dashboard.html', icon: 'dashboard', enabled: true },
      { id: 'productos', label: 'Productos', href: 'productos.html', icon: 'products', enabled: true },
      { id: 'inventario', label: 'Inventario', href: 'inventario.html', icon: 'inventory', enabled: true },
      { id: 'ventas', label: 'Ventas / POS', href: 'pos.html', icon: 'sales', enabled: true },
      { id: 'caja', label: 'Caja', href: 'caja.html', icon: 'cash', enabled: true },
      { id: 'clientes', label: 'Clientes', href: 'clientes.html', icon: 'customers', enabled: true },
      { id: 'pedidos', label: 'Pedidos', href: 'pedidos.html', icon: 'orders', enabled: true },
    ],
  },
  {
    label: 'Análisis',
    items: [
      { id: 'reportes', label: 'Reportes', href: 'reportes.html', icon: 'reports', enabled: true },
      { id: 'auditoria', label: 'Auditoría', href: 'auditoria.html', icon: 'audit', enabled: true },
    ],
  },
  {
    label: 'Administración',
    items: [
      { id: 'usuarios', label: 'Usuarios', href: 'usuarios.html', icon: 'users', enabled: true },
      { id: 'configuracion', label: 'Configuración', href: 'configuracion.html', icon: 'settings', enabled: true },
    ],
  },
];

function navItemHtml(item, activePage) {
  const isActive = item.id === activePage;
  const commonAttrs = `class="nav-item" ${isActive ? "aria-current='page'" : ''}`;
  if (!item.enabled) {
    return `<span class="${'nav-item'}" aria-disabled="true" title="Disponible en una próxima fase" style="opacity:.45; cursor:not-allowed;">
      ${icon(item.icon)}<span>${item.label}</span>
    </span>`;
  }
  return `<a href="${item.href}" ${commonAttrs}>${icon(item.icon)}<span>${item.label}</span></a>`;
}

export function renderShell(activePage) {
  const session = getSession();
  if (!session) {
    window.location.href = 'login.html';
    return;
  }

  const sidebarRoot = document.querySelector('#shell-sidebar');
  const topbarRoot = document.querySelector('#shell-topbar');
  if (!sidebarRoot || !topbarRoot) return;

  // Se pinta con la última marca conocida (cacheada) desde el primer render,
  // para no mostrar el logo por defecto ni el de una empresa anterior mientras
  // se resuelve la petición async de abajo.
  const marcaCacheada = getCachedCompanySettings();
  const logoInicial = marcaCacheada?.logoUrl ? `${API_ORIGIN}${marcaCacheada.logoUrl}` : 'assets/brand/logo-mark-light.png';
  const nombreInicial = marcaCacheada?.name?.trim() || 'Freestyle Perú';

  sidebarRoot.innerHTML = `
    <aside class="sidebar">
      <div class="sidebar-brand">
        <img id="sidebar-brand-logo" src="${logoInicial}" alt="${nombreInicial}" />
        <span id="sidebar-brand-name">${nombreInicial}</span>
      </div>
      <nav class="sidebar-nav" aria-label="Navegación principal">
        ${NAV_SECTIONS.map((section) => `
          ${section.label ? `<span class="nav-section-label">${section.label}</span>` : ''}
          ${section.items.map((item) => navItemHtml(item, activePage)).join('')}
        `).join('')}
      </nav>
      <div class="sidebar-footer">
        <button class="sidebar-toggle" type="button" data-sidebar-toggle aria-label="Contraer u expandir menú">
          ${icon('collapse', 16)}
          <span>Contraer</span>
        </button>
      </div>
    </aside>
    <div class="sidebar-scrim" data-sidebar-scrim></div>
  `;

  topbarRoot.innerHTML = `
    <header class="topbar">
      <div style="display:flex; align-items:center; gap: var(--space-3);">
        <button class="btn btn-ghost btn-sm" type="button" data-sidebar-open aria-label="Abrir menú" style="display:none;">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16M4 12h16M4 18h16" stroke-linecap="round"/></svg>
        </button>
        <label class="topbar-search">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3" stroke-linecap="round"/></svg>
          <input type="search" id="global-search-input" placeholder="Buscar producto, SKU, cliente…" aria-label="Búsqueda global" autocomplete="off" />
          <div class="global-search-results" id="global-search-results" hidden></div>
        </label>
      </div>
      <div class="topbar-actions">
        <button class="cash-status" id="cash-status-badge" type="button" style="border:none; cursor:default;">
          <span class="dot" style="background:var(--neutral-300);"></span> Comprobando caja…
        </button>
        <div class="user-menu">
          <span class="avatar">${initials(session.user.fullName)}</span>
          <div style="line-height:1.2;">
            <div style="font-size: var(--font-size-sm); font-weight:600;">${session.user.fullName}</div>
            <div style="font-size: var(--font-size-xs); color: var(--color-text-muted);">${session.user.roles[0]}</div>
          </div>
          <button class="btn btn-ghost btn-sm" type="button" id="logout-button" aria-label="Cerrar sesión">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" stroke-linecap="round"/><path d="M16 17l5-5-5-5M21 12H9" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </div>
      </div>
    </header>
  `;

  document.querySelector('#logout-button')?.addEventListener('click', logout);
  initSidebar();
  actualizarEstadoCaja();
  actualizarMarca();
  initBusquedaGlobal();

  const mediaQuery = window.matchMedia('(max-width: 767px)');
  const mobileOpenBtn = document.querySelector('[data-sidebar-open]');
  const applyMobileVisibility = () => {
    if (mobileOpenBtn) mobileOpenBtn.style.display = mediaQuery.matches ? 'inline-flex' : 'none';
  };
  applyMobileVisibility();
  mediaQuery.addEventListener('change', applyMobileVisibility);
}

async function actualizarMarca() {
  const settings = await fetchCompanySettings();
  if (!settings) return;

  const nombre = settings.name?.trim();
  if (nombre) {
    const nombreEl = document.querySelector('#sidebar-brand-name');
    if (nombreEl) nombreEl.textContent = nombre;
  }
  if (settings.logoUrl) {
    const logoEl = document.querySelector('#sidebar-brand-logo');
    if (logoEl) logoEl.src = `${API_ORIGIN}${settings.logoUrl}`;
  }
}

function initBusquedaGlobal() {
  const input = document.querySelector('#global-search-input');
  const resultados = document.querySelector('#global-search-results');
  if (!input || !resultados) return;

  const buscar = debounce(async (q) => {
    if (q.trim().length < 2) {
      resultados.hidden = true;
      return;
    }
    try {
      const data = await api.get('/search', { query: { q } });
      renderResultadosBusqueda(resultados, data);
    } catch (error) {
      resultados.innerHTML = `<div class="global-search-empty">${error instanceof ApiError ? error.message : 'No se pudo buscar'}</div>`;
      resultados.hidden = false;
    }
  }, 300);

  input.addEventListener('input', (event) => buscar(event.target.value));
  input.addEventListener('focus', () => {
    if (input.value.trim().length >= 2 && resultados.innerHTML) resultados.hidden = false;
  });
  document.addEventListener('click', (event) => {
    if (!event.target.closest('.topbar-search')) resultados.hidden = true;
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') resultados.hidden = true;
  });
}

function renderResultadosBusqueda(contenedor, data) {
  const grupos = Object.entries(data).filter(([, items]) => items.length > 0);

  contenedor.innerHTML = grupos.length
    ? grupos
        .map(
          ([grupo, items]) => `
      <div class="global-search-group-label">${GROUP_LABELS[grupo] || grupo}</div>
      ${items
        .map(
          (item) => `
        <a class="global-search-item" href="${item.url}">
          <span class="global-search-item-title">${item.title}</span>
          ${item.subtitle ? `<span class="global-search-item-subtitle">${item.subtitle}</span>` : ''}
        </a>
      `
        )
        .join('')}
    `
        )
        .join('')
    : '<div class="global-search-empty">Sin resultados</div>';

  contenedor.hidden = false;
}

export async function actualizarEstadoCaja() {
  const badge = document.querySelector('#cash-status-badge');
  if (!badge) return;

  try {
    const sesion = await fetchCurrentSession();
    if (sesion) {
      badge.classList.remove('badge-neutral');
      badge.style.background = 'var(--color-success-bg)';
      badge.style.color = 'var(--color-success-text)';
      badge.innerHTML = `<span class="dot" style="background:var(--color-success);"></span> ${sesion.cashRegisterName} abierta`;
      badge.style.cursor = 'default';
      badge.onclick = null;
    } else {
      badge.style.background = 'var(--color-warning-bg)';
      badge.style.color = 'var(--color-warning-text)';
      badge.innerHTML = `<span class="dot" style="background:var(--color-warning);"></span> Sin caja abierta`;
      badge.style.cursor = 'pointer';
      badge.onclick = () => openAbrirCajaModal({ onOpened: actualizarEstadoCaja });
    }
  } catch {
    badge.innerHTML = `<span class="dot" style="background:var(--neutral-300);"></span> Caja no disponible`;
  }
}
