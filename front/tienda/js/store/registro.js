import { registrarCliente } from './core/customer-auth.js';
import { renderStoreShell } from './components/store-shell.js';
import { ApiError } from './core/store-api.js';

async function onSubmit(event) {
  event.preventDefault();
  const errorEl = document.querySelector('#registro-error');
  errorEl.innerHTML = '';
  const submitButton = event.target.querySelector('button[type="submit"]');
  submitButton.disabled = true;

  try {
    await registrarCliente({
      fullName: document.querySelector('#fullName').value.trim(),
      email: document.querySelector('#email').value.trim(),
      phone: document.querySelector('#phone').value.trim() || null,
      password: document.querySelector('#password').value,
    });
    window.location.href = '../index.html';
  } catch (error) {
    errorEl.innerHTML = `<p class="field-error" style="margin-bottom: var(--space-3);">${error instanceof ApiError ? error.message : 'No se pudo crear la cuenta'}</p>`;
  } finally {
    submitButton.disabled = false;
  }
}

renderStoreShell({ basePath: '../' });
document.querySelector('#registro-form').addEventListener('submit', onSubmit);
