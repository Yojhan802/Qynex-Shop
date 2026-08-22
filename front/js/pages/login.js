import { login, getSession } from '../core/auth.js';
import { fetchPublicBranding, getCachedPublicBranding, resolveLogoUrl } from '../core/public-branding.js';

aplicarMarca(getCachedPublicBranding());
fetchPublicBranding().then(aplicarMarca);

function aplicarMarca(branding) {
  document.querySelector('#page-title').textContent = `Iniciar sesión · ${branding.name}`;
  document.querySelector('#brand-name-desktop').textContent = branding.name;
  document.querySelector('#brand-footer').textContent = `© ${new Date().getFullYear()} ${branding.name} — Sistema Integral de Gestión`;

  const logoUrl = resolveLogoUrl(branding);
  if (logoUrl) {
    document.querySelector('#brand-logo-desktop').src = logoUrl;
    document.querySelector('#brand-logo-mobile').src = logoUrl;
  }
  document.querySelector('#brand-logo-mobile').alt = branding.name;
}

const form = document.querySelector('#login-form');
const errorAlert = document.querySelector('#login-error');
const submitButton = document.querySelector('#login-submit');
const passwordInput = document.querySelector('#password');
const togglePasswordButton = document.querySelector('#toggle-password');

if (getSession()) {
  window.location.href = 'dashboard.html';
}

togglePasswordButton?.addEventListener('click', () => {
  const isPassword = passwordInput.type === 'password';
  passwordInput.type = isPassword ? 'text' : 'password';
  togglePasswordButton.setAttribute('aria-label', isPassword ? 'Ocultar contraseña' : 'Mostrar contraseña');
  togglePasswordButton.querySelector('[data-icon-open]').hidden = isPassword;
  togglePasswordButton.querySelector('[data-icon-closed]').hidden = !isPassword;
});

form?.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorAlert.hidden = true;

  const username = document.querySelector('#username').value;
  const password = passwordInput.value;

  submitButton.disabled = true;
  submitButton.querySelector('.spinner').hidden = false;
  submitButton.querySelector('.btn-label').textContent = 'Ingresando…';

  try {
    await login(username, password);
    window.location.href = 'dashboard.html';
  } catch (error) {
    errorAlert.querySelector('.alert-message').textContent = error.message;
    errorAlert.hidden = false;
    submitButton.disabled = false;
    submitButton.querySelector('.spinner').hidden = true;
    submitButton.querySelector('.btn-label').textContent = 'Ingresar';
    document.querySelector('#username').focus();
  }
});
