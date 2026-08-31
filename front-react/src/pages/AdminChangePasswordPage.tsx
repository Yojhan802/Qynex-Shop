import { useState, type FormEvent } from 'react';
import { ApiError, api, clearStaffSession, getStaffSession } from '../services/api';
import { LoadingState } from '../components/States';

export function AdminChangePasswordPage() {
  const session = getStaffSession();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (!session) {
    window.history.replaceState({}, '', '/admin/login');
    window.dispatchEvent(new PopStateEvent('popstate'));
    return <main className="admin-login"><LoadingState label="Redirigiendo al inicio de sesión…" /></main>;
  }
  const activeSession = session;

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (newPassword !== confirmPassword) { setError('Las nuevas contraseñas no coinciden.'); return; }
    setLoading(true); setError('');
    try {
      const endpoint = activeSession.user.mustChangePassword ? '/auth/complete-password-change' : '/auth/change-password';
      await api.post(endpoint, { currentPassword, newPassword }, { auth: 'staff' });
      clearStaffSession();
      window.history.pushState({}, '', '/admin/login');
      window.dispatchEvent(new PopStateEvent('popstate'));
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo cambiar la contraseña.');
      setLoading(false);
    }
  }

  return <main className="admin-login"><div className="admin-login-card"><span className="store-kicker">QYNEX · SEGURIDAD</span><h1>Cambiar contraseña</h1><p>{activeSession.user.mustChangePassword ? 'Debes establecer una contraseña propia para continuar.' : 'Actualiza la contraseña de tu cuenta.'}</p><form onSubmit={submit}><label>Contraseña actual<input className="input" type="password" autoComplete="current-password" required value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} /></label><label>Nueva contraseña<input className="input" type="password" autoComplete="new-password" minLength={8} required value={newPassword} onChange={(event) => setNewPassword(event.target.value)} /></label><label>Repetir nueva contraseña<input className="input" type="password" autoComplete="new-password" minLength={8} required value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} /></label>{error && <div className="alert alert-danger" role="alert">{error}</div>}<button className="btn btn-primary" disabled={loading}>{loading ? 'Guardando…' : 'Guardar nueva contraseña'}</button></form></div></main>;
}
