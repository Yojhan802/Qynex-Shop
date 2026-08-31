const RECONNECT_DELAY = 3000;

export function connectCustomerNotifications(onOrderUpdated: (order: { id: number; status: string; orderNumber?: string }) => void, getToken: () => string | undefined, refreshToken: () => Promise<boolean>) {
  let closed = false;
  let source: EventSource | null = null;
  let retry: number | undefined;
  const open = () => {
    if (closed) return;
    const token = getToken();
    if (!token) return;
    source = new EventSource(`/api/store/notifications/stream?token=${encodeURIComponent(token)}`);
    source.addEventListener('pedido-actualizado', (event) => { try { onOrderUpdated(JSON.parse((event as MessageEvent).data)); } catch { /* evento inválido, se ignora */ } });
    source.onerror = () => {
      source?.close();
      if (closed) return;
      refreshToken().finally(() => { if (!closed) retry = window.setTimeout(open, RECONNECT_DELAY); });
    };
  };
  open();
  return { close() { closed = true; source?.close(); if (retry) window.clearTimeout(retry); } };
}
