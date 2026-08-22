import { api, ApiError } from '../core/api.js';
import { getSession } from '../core/session.js';
import { openModal, closeModal } from './modal.js';
import { showToast } from './toast.js';

export async function openUsuarioForm({ usuario = null, onSaved }) {
  const esEdicion = Boolean(usuario);
  const roles = await api.get('/roles');

  // RN-25: solo se pueden asignar roles con techo <= el más alto entre los roles propios.
  const misRoles = new Set(getSession()?.user?.roles ?? []);
  const techoAsignacion = roles
      .filter((r) => misRoles.has(r.code))
      .reduce((max, r) => Math.max(max, r.hierarchyLevel), 0);

  const rolesSeleccionados = new Set((usuario?.roles ?? []).map((r) => r.id));

  const modal = openModal({
    title: esEdicion ? 'Editar usuario' : 'Nuevo usuario',
    subtitle: esEdicion ? usuario.username : 'La contraseña inicial la define el sistema; el usuario deberá cambiarla al ingresar.',
    maxWidth: '560px',
    body: `
      <form id="usuario-form" novalidate>
        <div class="alert alert-danger" id="usuario-form-error" role="alert" hidden>
          <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="8"/><path d="M10 6v5M10 14h.01" stroke-linecap="round"/></svg>
          <span class="alert-message"></span>
        </div>
        <div class="form-grid">
          ${
            esEdicion
              ? ''
              : `
          <div class="field field-span-2">
            <label class="field-label" for="uf-username">Usuario</label>
            <input class="input" id="uf-username" required maxlength="50" pattern="[a-zA-Z0-9._-]+" />
          </div>
          <div class="field field-span-2">
            <label class="field-label" for="uf-password">Contraseña inicial</label>
            <input class="input" id="uf-password" type="password" required minlength="8" placeholder="Mínimo 8 caracteres, con letras y números" />
          </div>
          `
          }
          <div class="field field-span-2">
            <label class="field-label" for="uf-fullname">Nombre completo</label>
            <input class="input" id="uf-fullname" required maxlength="120" value="${usuario?.fullName ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="uf-email">Email</label>
            <input class="input" type="email" id="uf-email" maxlength="120" value="${usuario?.email ?? ''}" />
          </div>
          <div class="field">
            <label class="field-label" for="uf-phone">Teléfono</label>
            <input class="input" id="uf-phone" maxlength="20" value="${usuario?.phone ?? ''}" />
          </div>
          <div class="field field-span-2">
            <span class="field-label">Roles</span>
            <div style="display:flex; flex-wrap:wrap; gap: var(--space-3); padding: var(--space-2) 0;">
              ${roles
                .map((r) => {
                  const bloqueado = r.hierarchyLevel > techoAsignacion && !rolesSeleccionados.has(r.id);
                  return `
                <label class="checkbox-field" ${bloqueado ? 'style="opacity:0.5;" title="Supera tu nivel de autorización"' : ''}>
                  <input type="checkbox" name="uf-role" value="${r.id}" ${rolesSeleccionados.has(r.id) ? 'checked' : ''} ${bloqueado ? 'disabled' : ''} /> ${r.name}
                </label>
              `;
                })
                .join('')}
            </div>
          </div>
        </div>
      </form>
    `,
    footer: `
      <button class="btn btn-secondary" type="button" data-cancel>Cancelar</button>
      <button class="btn btn-primary" type="submit" form="usuario-form">${esEdicion ? 'Guardar cambios' : 'Crear usuario'}</button>
    `,
  });

  modal.footer.querySelector('[data-cancel]').addEventListener('click', closeModal);
  modal.body.querySelector('#usuario-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    const errorAlert = modal.body.querySelector('#usuario-form-error');
    errorAlert.hidden = true;

    const roleIds = [...modal.body.querySelectorAll('input[name="uf-role"]:checked')].map((el) => Number(el.value));
    if (roleIds.length === 0) {
      errorAlert.querySelector('.alert-message').textContent = 'Selecciona al menos un rol.';
      errorAlert.hidden = false;
      return;
    }

    const basePayload = {
      email: modal.body.querySelector('#uf-email').value.trim() || null,
      fullName: modal.body.querySelector('#uf-fullname').value.trim(),
      phone: modal.body.querySelector('#uf-phone').value.trim() || null,
      roleIds,
    };

    try {
      let resultado;
      if (esEdicion) {
        resultado = await api.put(`/users/${usuario.id}`, basePayload);
      } else {
        resultado = await api.post('/users', {
          ...basePayload,
          username: modal.body.querySelector('#uf-username').value.trim(),
          password: modal.body.querySelector('#uf-password').value,
          dni: null,
        });
      }
      closeModal();
      showToast({ type: 'success', title: esEdicion ? 'Usuario actualizado' : 'Usuario creado', message: resultado.fullName });
      onSaved?.(resultado);
    } catch (error) {
      errorAlert.querySelector('.alert-message').textContent = error instanceof ApiError ? error.message : 'No se pudo guardar el usuario';
      errorAlert.hidden = false;
    }
  });
}
