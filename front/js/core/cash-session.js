import { api, ApiError } from './api.js';

/** Devuelve la sesión de caja abierta del usuario actual, o null si no tiene ninguna. */
export async function fetchCurrentSession() {
  try {
    return await api.get('/cash-registers/sessions/current');
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export async function fetchRegisters() {
  return api.get('/cash-registers');
}

export async function openSession(cashRegisterId, openingAmount) {
  return api.post('/cash-registers/sessions', { cashRegisterId, openingAmount });
}
