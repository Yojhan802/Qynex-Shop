import { api } from './api.js';

const CACHE_KEY = 'freestyleperu:company-settings';

/**
 * Última copia conocida de los datos de marca (logo, razón social), leída de
 * forma síncrona. Se usa para pintar el sidebar de una vez con la marca
 * correcta al navegar entre páginas, en vez de mostrar el logo por defecto
 * mientras se resuelve la petición async (lo que se vería, por un instante,
 * como el logo de OTRA empresa si ya se personalizó).
 */
export function getCachedCompanySettings() {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/**
 * Datos de la empresa (logo, razón social) para la marca del shell.
 * Best-effort: si el usuario no tiene CONFIGURACION_VER o falla la
 * petición, devuelve null y quien llama debe usar sus valores por defecto.
 * Actualiza la copia en caché para que la próxima navegación ya nazca
 * con la marca correcta.
 */
export async function fetchCompanySettings() {
  try {
    const settings = await api.get('/settings/company');
    localStorage.setItem(CACHE_KEY, JSON.stringify(settings));
    return settings;
  } catch {
    return null;
  }
}
