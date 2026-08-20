import { formatInteger } from '../core/format.js';

const CHEVRON_LEFT = '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6" stroke-linecap="round" stroke-linejoin="round"/></svg>';
const CHEVRON_RIGHT = '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 6l6 6-6 6" stroke-linecap="round" stroke-linejoin="round"/></svg>';

/**
 * Renderiza la barra de paginación de un PageResponse del backend y conecta
 * los botones a onPageChange(nuevaPagina).
 */
export function renderPagination(container, page, onPageChange) {
  if (page.totalElements === 0) {
    container.innerHTML = '';
    return;
  }

  const start = page.page * page.size + 1;
  const end = Math.min(page.page * page.size + page.content.length, page.totalElements);

  container.innerHTML = `
    <span>${formatInteger(start)}–${formatInteger(end)} de ${formatInteger(page.totalElements)}</span>
    <div class="pagination-controls">
      <button class="pagination-btn" type="button" data-prev ${page.first ? 'disabled' : ''} aria-label="Página anterior">${CHEVRON_LEFT}</button>
      <span class="mono" style="padding: 0 var(--space-2);">${page.page + 1} / ${Math.max(page.totalPages, 1)}</span>
      <button class="pagination-btn" type="button" data-next ${page.last ? 'disabled' : ''} aria-label="Página siguiente">${CHEVRON_RIGHT}</button>
    </div>
  `;

  container.querySelector('[data-prev]')?.addEventListener('click', () => onPageChange(page.page - 1));
  container.querySelector('[data-next]')?.addEventListener('click', () => onPageChange(page.page + 1));
}
