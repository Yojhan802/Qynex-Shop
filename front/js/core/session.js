// Almacenamiento puro de la sesión (sin llamadas a red), para que
// core/api.js y core/auth.js puedan depender de esto sin ciclos.

const SESSION_KEY = 'fsp.session';

export function getSession() {
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function setSession(session) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}
