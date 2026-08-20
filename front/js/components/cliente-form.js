import { api, ApiError } from '../core/api.js';
import { openModal, closeModal } from './modal.js';
import { showToast } from './toast.js';

const DOC_TYPES = [
  { value: 'SIN_DOCUMENTO', label: 'Sin documento' },
  { value: 'DNI', label: 'DNI' },
  { value: 'RUC', label: 'RUC' },
  { value: 'CE', label: 'Carné de extranjería' },
];

export function openClienteForm({ cliente = null, onSaved }) {
  const esEdicion = Boolean(cliente);

  const modal = openModal({
    title: esEdicion ? 'Editar cliente' : 'Nuevo cliente',
    maxWidth: '520px',
    body: `
      <form id="cliente-form" novalidate>
        <div class="alert alert-danger" id="cliente-form-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="form-grid">
          <div class="field field-span-2">
            <label class="field-label" for="cf-name">Nombre completo</label>
            <input class="input" id="cf-name" required maxlength="150" value="${cliente?.fullName ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="cf-doc-type">Tipo de documento</label>
            <select class="select" id="cf-doc-type">
              ${DOC_TYPES.map((t) => `<option value="${t.value}" ${cliente?.docType === t.value ? 'selected' : ''}>${t.label}</option>`).join('')}
            </select>
          </div>
          <div class="field">
            <label class="field-label" for="cf-doc-number">Número de documento</label>
            <input class="input" id="cf-doc-number" maxlength="15" value="${cliente?.docNumber ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="cf-phone">Teléfono</label>
            <input class="input" id="cf-phone" maxlength="20" value="${cliente?.phone ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="cf-email">Email</label>
            <input class="input" type="email" id="cf-email" maxlength="120" value="${cliente?.email ?? ''}" />
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="cliente-form">${esEdicion ? 'Guardar cambios' : 'Crear cliente'}</button>
    `,
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', closeModal);
  modal.body.querySelector('#cliente-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#cliente-form-error');
    errorAlert.hidden = true;

    const payload = {
      fullName: modal.body.querySelector('#cf-name').value.trim(),
      docType: modal.body.querySelector('#cf-doc-type').value,
      docNumber: modal.body.querySelector('#cf-doc-number').value.trim() || null,
      phone: modal.body.querySelector('#cf-phone').value.trim() || null,
      email: modal.body.querySelector('#cf-email').value.trim() || null,
      birthDate: null,
    };

    try {
      const resultado = esEdicion ? await api.put(`/customers/${cliente.id}`, payload) : await api.post('/customers', payload);
      closeModal();
      showToast({ type: 'success', title: esEdicion ? 'Cliente actualizado' : 'Cliente creado', message: resultado.fullName });
      onSaved?.(resultado);
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo guardar el cliente';
      errorAlert.hidden = false;
    }
  });
}
