import { storeApi } from './store-api.js';
import { getCustomerSession, setCustomerSession, clearCustomerSession } from './customer-session.js';

export { getCustomerSession } from './customer-session.js';

export async function registrarCliente({ email, password, fullName, phone }) {
  const data = await storeApi.post('/store/auth/register', { email, password, fullName, phone });
  setCustomerSession({ accessToken: data.accessToken, refreshToken: data.refreshToken, customer: data.customer });
  return data;
}

export async function loginCliente(email, password) {
  const data = await storeApi.post('/store/auth/login', { email, password });
  setCustomerSession({ accessToken: data.accessToken, refreshToken: data.refreshToken, customer: data.customer });
  return data;
}

export function logoutCliente() {
  clearCustomerSession();
}

/**
 * Redirige a login (recordando a dónde volver) si no hay sesión de cliente.
 * `loginPath` es la ruta relativa a login.html DESDE la página que llama —
 * distinta según se llame desde tienda/ (páginas de primer nivel) o desde
 * tienda/cuenta/ (donde login.html es un hermano, no un hijo).
 */
export function requireCustomerSession(loginPath = 'cuenta/login.html') {
  const session = getCustomerSession();
  if (!session) {
    // El "volver" solo tiene sentido cuando login.html es hijo directo de la
    // página actual (login.js lo resuelve como '../<volver>'): para llamadas
    // desde dentro de cuenta/ (login.html es hermano) se omite y basta con
    // volver al catálogo tras iniciar sesión.
    const conVolver = loginPath === 'cuenta/login.html';
    if (conVolver) {
      const volver = encodeURIComponent(window.location.pathname.split('/').pop());
      window.location.href = `${loginPath}?volver=${volver}`;
    } else {
      window.location.href = loginPath;
    }
    return null;
  }
  return session;
}
