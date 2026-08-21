// Sesión del cliente de la tienda online — clave propia de sessionStorage
// (distinta de 'fsp.session', que es la del staff) para que un mismo
// navegador pueda tener abierta una sesión de staff y una de cliente sin
// que se pisen entre sí.

const SESSION_KEY = 'fsp.customer.session';

export function getCustomerSession() {
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function setCustomerSession(session) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearCustomerSession() {
  sessionStorage.removeItem(SESSION_KEY);
}
