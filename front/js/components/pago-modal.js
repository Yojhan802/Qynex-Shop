import { api, ApiError, API_ORIGIN } from '../core/api.js';
import { openModal, closeModal } from './modal.js';
import { formatCurrency } from '../core/format.js';

/** Modal de cobro con soporte de pago mixto. `onConfirm({ payments, promoterId })` recibe la venta a registrar. */
export async function openPagoModal({ total, onConfirm }) {
  const [metodos, promotores] = await Promise.all([
    api.get('/payment-methods').then((lista) => lista.filter((m) => m.status === 'ACTIVE')),
    api.get('/promoters').then((lista) => lista.filter((p) => p.status === 'ACTIVE')).catch(() => []),
  ]);

  const modal = openModal({
    title: 'Cobrar venta',
    subtitle: `Total a pagar: ${formatCurrency(total)}`,
    maxWidth: '480px',
    body: `
      <form id="pago-form" novalidate>
        <div class="alert alert-danger" id="pago-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        ${
          promotores.length > 0
            ? `
        <div class="field" style="margin-bottom: var(--space-4);">
          <label class="field-label" for="pago-promotor">Promotor (opcional)</label>
          <select class="select" id="pago-promotor">
            <option value="">Ninguno — solo el vendedor de caja</option>
            ${promotores.map((p) => `<option value="${p.id}">${p.name}</option>`).join('')}
          </select>
        </div>
        `
            : ''
        }
        <div style="display:flex; flex-direction:column; gap: var(--space-3);">
          ${metodos
            .map(
              (m) => `
            <div>
              <div style="display:flex; align-items:center; gap: var(--space-3);">
                <span style="flex:1; font-weight:600; font-size:var(--font-size-sm);">${m.name}</span>
                ${m.qrImageUrl ? `<button type="button" class="btn btn-ghost btn-sm" data-toggle-qr="${m.id}">Mostrar QR</button>` : ''}
                ${m.requiresReference ? `<input class="input" style="width:120px;" placeholder="N° operación" data-ref="${m.id}" />` : ''}
                <div class="input-group" style="width:130px;">
                  <input class="input" type="number" min="0" step="0.01" value="0.00" data-amount="${m.id}" style="text-align:right;" />
                </div>
                <button type="button" class="btn btn-ghost btn-sm" data-fill="${m.id}">Todo</button>
              </div>
              ${
                m.qrImageUrl
                  ? `
              <div class="pago-qr-panel" data-qr-panel="${m.id}" hidden>
                <img src="${API_ORIGIN}${m.qrImageUrl}" alt="Código QR para pagar con ${m.name}" style="max-width:200px; width:100%; border-radius: var(--radius-md); border:1px solid var(--color-border);" />
              </div>
              `
                  : ''
              }
            </div>
          `
            )
            .join('')}
        </div>
        <div style="display:flex; justify-content:space-between; margin-top: var(--space-5); padding-top: var(--space-4); border-top:1px solid var(--color-border); font-weight:600;">
          <span>Restante</span>
          <span class="mono" id="pago-restante">${formatCurrency(total)}</span>
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-dark" type="submit" form="pago-form" id="pago-confirmar" disabled>Confirmar venta</button>
    `,
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', closeModal);
  const restanteEl = modal.body.querySelector('#pago-restante');
  const confirmBtn = modal.footer.querySelector('#pago-confirmar');

  function recalcular() {
    const suma = metodos.reduce((acc, m) => acc + (Number(modal.body.querySelector(`[data-amount="${m.id}"]`).value) || 0), 0);
    const restante = Math.round((total - suma) * 100) / 100;
    restanteEl.textContent = formatCurrency(restante);
    restanteEl.style.color = restante === 0 ? 'var(--color-success-text)' : 'var(--color-text)';
    confirmBtn.disabled = restante !== 0;
    return { suma, restante };
  }

  metodos.forEach((m) => {
    modal.body.querySelector(`[data-toggle-qr="${m.id}"]`)?.addEventListener('click', (event) => {
      const panel = modal.body.querySelector(`[data-qr-panel="${m.id}"]`);
      panel.hidden = !panel.hidden;
      event.currentTarget.textContent = panel.hidden ? 'Mostrar QR' : 'Ocultar QR';
    });
    modal.body.querySelector(`[data-amount="${m.id}"]`).addEventListener('input', recalcular);
    modal.body.querySelector(`[data-fill="${m.id}"]`).addEventListener('click', () => {
      const { restante } = recalcular();
      const input = modal.body.querySelector(`[data-amount="${m.id}"]`);
      const actual = Number(input.value) || 0;
      input.value = (actual + restante).toFixed(2);
      recalcular();
    });
  });
  recalcular();

  modal.body.querySelector('#pago-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#pago-error');
    errorAlert.hidden = true;

    const payments = metodos
      .map((m) => ({
        paymentMethodId: m.id,
        amount: Number(modal.body.querySelector(`[data-amount="${m.id}"]`).value) || 0,
        reference: m.requiresReference ? modal.body.querySelector(`[data-ref="${m.id}"]`)?.value.trim() || null : null,
      }))
      .filter((p) => p.amount > 0);

    if (payments.length === 0) {
      errorAlert.querySelector('.alert-message').textContent = 'Ingresa al menos un monto.';
      errorAlert.hidden = false;
      return;
    }

    const promotorSelect = modal.body.querySelector('#pago-promotor');
    const promoterId = promotorSelect?.value ? Number(promotorSelect.value) : null;

    confirmBtn.disabled = true;
    try {
      await onConfirm({ payments, promoterId });
      // Cierra esta instancia concreta: onConfirm puede haber abierto ya un
      // modal nuevo (p. ej. el ticket) que no debe cerrarse por accidente.
      modal.close();
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo registrar la venta';
      errorAlert.hidden = false;
      confirmBtn.disabled = false;
    }
  });
}
