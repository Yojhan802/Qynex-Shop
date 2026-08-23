import { API_ORIGIN } from './api.js';

const CACHE_KEY = 'app:public-branding';
const DEFAULT_BRANDING = { name: 'Qynex', logoUrl: null };

/**
 * Nombre + logo de esta instalación, sin necesidad de sesión — para login,
 * "servicio suspendido" y la tienda pública. Se usa junto con
 * {@link getCachedPublicBranding} con el mismo criterio que
 * core/settings.js: pintar de una vez con la última marca conocida
 * (cacheada) mientras se resuelve esta petición async, para no mostrar el
 * logo por defecto ni el de otra instalación por un instante.
 */
export async function fetchPublicBranding() {
  try {
    const res = await fetch(`${API_ORIGIN}/api/system/branding`);
    if (!res.ok) throw new Error();
    const branding = await res.json();
    localStorage.setItem(CACHE_KEY, JSON.stringify(branding));
    return branding;
  } catch {
    return DEFAULT_BRANDING;
  }
}

export function getCachedPublicBranding() {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    return raw ? JSON.parse(raw) : DEFAULT_BRANDING;
  } catch {
    return DEFAULT_BRANDING;
  }
}

export function resolveLogoUrl(branding) {
  return branding?.logoUrl ? `${API_ORIGIN}${branding.logoUrl}` : null;
}

/**
 * El ícono de la pestaña (`<link rel="icon">`) no se actualiza solo cuando
 * cambia el logo — a diferencia de una <img>, nada vuelve a pedirlo salvo
 * que se le cambie el href a mano. Sin logo propio, se deja el estático
 * (Qynex) que ya trae el HTML.
 */
export function applyFavicon(logoUrl) {
  if (!logoUrl) return;
  const link = document.querySelector('link[rel="icon"]');
  if (link) link.href = logoUrl;
}
