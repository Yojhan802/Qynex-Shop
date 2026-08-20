import { openModal, closeModal } from './modal.js';

/** Confirmación para acciones destructivas o irreversibles (docs/06 §36). */
export function confirmAction({ title, message, confirmLabel = 'Confirmar', danger = true }) {
  return new Promise((resolve) => {
    const iconWrap = danger ? 'stat-icon-danger' : 'stat-icon-warning';
    const modal = openModal({
      title,
      body: `
        <div class="confirm-icon ${iconWrap}">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 3l8 14H2L10 3z" stroke-linejoin="round"/><path d="M10 8.5v3M10 14h.01" stroke-linecap="round"/>
          </svg>
        </div>
        <p style="color: var(--color-text-secondary);">${message}</p>
      `,
      footer: `
        <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
        <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" type="button" data-confirm>${confirmLabel}</button>
      `,
      maxWidth: '420px',
    });

    modal.footer.querySelector('[data-cancel]').addEventListener('click', () => {
      closeModal();
      resolve(false);
    });
    modal.footer.querySelector('[data-confirm]').addEventListener('click', () => {
      closeModal();
      resolve(true);
    });
  });
}
