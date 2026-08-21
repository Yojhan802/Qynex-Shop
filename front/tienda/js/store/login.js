import { loginCliente } from './core/customer-auth.js';
import { renderStoreShell } from './components/store-shell.js';
import { ApiError } from './core/store-api.js';

function volverA() {
  const volver = new URLSearchParams(window.location.search).get('volver');
  return volver && /^[a-z-]+\.html$/.test(volver) ? `../${volver}` : '../index.html';
}

async function onSubmit(event) {
  event.preventDefault();
  const errorEl = document.querySelector('#login-error');
  errorEl.innerHTML = '';
  const submitButton = event.target.querySelector('button[type="submit"]');
  submitButton.disabled = true;

  try {
    await loginCliente(document.querySelector('#email').value.trim(), document.querySelector('#password').value);
    window.location.href = volverA();
  } catch (error) {
    errorEl.innerHTML = `<p class="field-error" style="margin-bottom: var(--space-3);">${error instanceof ApiError ? error.message : 'No se pudo iniciar sesión'}</p>`;
  } finally {
    submitButton.disabled = false;
  }
}

renderStoreShell({ basePath: '../' });
document.querySelector('#login-form').addEventListener('submit', onSubmit);
