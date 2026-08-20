import { ApiError } from '../core/api.js';
import { fetchRegisters, openSession } from '../core/cash-session.js';
import { openModal, closeModal } from './modal.js';
import { showToast } from './toast.js';

export function openAbrirCajaModal({ onOpened }) {
  const modal = openModal({
    title: 'Abrir caja',
    subtitle: 'Necesitas una sesión de caja abierta para registrar ventas.',
    maxWidth: '420px',
    body: `
      <form id="abrir-caja-form" novalidate>
        <div class="alert alert-danger" id="abrir-caja-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="field" style="margin-bottom: var(--space-4);">
          <label class="field-label" for="ac-register">Caja</label>
          <select class="select" id="ac-register" required><option value="">Cargando…</option></select>
        </div>
        <div class="field">
          <label class="field-label" for="ac-amount">Monto inicial (S/)</label>
          <input class="input" type="number" id="ac-amount" min="0" step="0.01" value="0.00" required />
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="abrir-caja-form">Abrir caja</button>
    `,
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', closeModal);

  fetchRegisters()
    .then((registers) => {
      const select = modal.body.querySelector('#ac-register');
      const activos = registers.filter((r) => r.status === 'ACTIVE');
      select.innerHTML = activos.length
        ? activos.map((r) => `<option value="${r.id}">${r.name}</option>`).join('')
        : '<option value="">No hay cajas activas</option>';
    })
    .catch(() => {
      modal.body.querySelector('#ac-register').innerHTML = '<option value="">Error al cargar cajas</option>';
    });

  modal.body.querySelector('#abrir-caja-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#abrir-caja-error');
    errorAlert.hidden = true;

    const registerId = Number(modal.body.querySelector('#ac-register').value);
    const amount = Number(modal.body.querySelector('#ac-amount').value);
    if (!registerId) {
      errorAlert.querySelector('.alert-message').textContent = 'Selecciona una caja.';
      errorAlert.hidden = false;
      return;
    }

    try {
      const sesion = await openSession(registerId, amount);
      closeModal();
      showToast({ type: 'success', title: 'Caja abierta' });
      onOpened?.(sesion);
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo abrir la caja';
      errorAlert.hidden = false;
    }
  });
}
