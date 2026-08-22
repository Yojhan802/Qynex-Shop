// Wrapper chico sobre EventSource con reconexión consciente del token. El
// reintento nativo de EventSource no sirve solo porque reusaría la URL vieja
// con un token ya vencido: los streams SSE reciben el access token por query
// param en vez de header (ver JwtAuthenticationFilter — EventSource del
// navegador no puede mandar headers propios en la conexión inicial).
const RECONNECT_DELAY_MS = 3000;

/**
 * @param {string} url - URL absoluta del stream, sin query param de token.
 * @param {object} opts
 * @param {() => string|undefined} opts.getToken - access token actual, o undefined si no hay sesión.
 * @param {() => Promise<boolean>} opts.refreshToken - intenta refrescar el token; resuelve si sirvió.
 * @param {Record<string, (data: any) => void>} opts.onEvent - handler por nombre de evento SSE.
 * @returns {{ close(): void }}
 */
export function connectLiveStream(url, { getToken, refreshToken, onEvent }) {
  let closed = false;
  let source = null;

  function abrir() {
    if (closed) return;
    const token = getToken();
    if (!token) return;

    source = new EventSource(`${url}?token=${encodeURIComponent(token)}`);
    Object.entries(onEvent).forEach(([eventName, handler]) => {
      source.addEventListener(eventName, (event) => {
        try {
          handler(JSON.parse(event.data));
        } catch {
          // Evento no-JSON o handler roto: no debe tumbar el stream completo.
        }
      });
    });
    source.onerror = () => {
      source?.close();
      if (closed) return;
      refreshToken().finally(() => {
        if (!closed) setTimeout(abrir, RECONNECT_DELAY_MS);
      });
    };
  }

  abrir();
  return {
    close() {
      closed = true;
      source?.close();
    },
  };
}
