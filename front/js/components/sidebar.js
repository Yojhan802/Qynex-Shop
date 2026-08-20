const COLLAPSE_KEY = 'fsp.sidebar-collapsed';

export function initSidebar() {
  const shell = document.querySelector('.app-shell');
  if (!shell) return;

  const toggle = shell.querySelector('[data-sidebar-toggle]');
  const mobileOpen = shell.querySelector('[data-sidebar-open]');
  const scrim = shell.querySelector('.sidebar-scrim');

  if (localStorage.getItem(COLLAPSE_KEY) === '1') {
    shell.setAttribute('data-sidebar', 'collapsed');
  }

  toggle?.addEventListener('click', () => {
    const collapsed = shell.getAttribute('data-sidebar') === 'collapsed';
    if (collapsed) {
      shell.removeAttribute('data-sidebar');
      localStorage.removeItem(COLLAPSE_KEY);
    } else {
      shell.setAttribute('data-sidebar', 'collapsed');
      localStorage.setItem(COLLAPSE_KEY, '1');
    }
  });

  mobileOpen?.addEventListener('click', () => {
    shell.setAttribute('data-sidebar', 'open');
  });

  scrim?.addEventListener('click', () => {
    shell.removeAttribute('data-sidebar');
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && shell.getAttribute('data-sidebar') === 'open') {
      shell.removeAttribute('data-sidebar');
    }
  });
}
