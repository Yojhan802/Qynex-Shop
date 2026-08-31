import { useEffect, useState } from 'react';
import { ApiError, getCustomerSession, imageUrl, refreshCustomerAccessToken, storeApi } from '../services/api';
import { connectCustomerNotifications } from '../services/live';
import type { Order } from '../types';
import { formatCurrency, formatDate } from '../utils';
import { EmptyState, ErrorState, LoadingState } from '../components/States';
import { StoreShell } from '../components/StoreShell';
import { useStoreTemplate } from '../components/TemplateProvider';
import { OrdersSurface } from '../templates/OrdersSurface';

const labels: Record<string, string> = { PENDING_PAYMENT: 'Pendiente de pago', CONFIRMED: 'Confirmado', CANCELLED: 'Anulado' };

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [selected, setSelected] = useState<Order | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const session = getCustomerSession();
  const template = useStoreTemplate();

  const loadOrders = () => storeApi.get<{ content: Order[] }>('/store/orders', { auth: true })
    .then((result) => setOrders(result.content ?? []))
    .catch((reason) => setError(reason instanceof ApiError ? reason.message : 'No se pudieron cargar tus pedidos'))
    .finally(() => setLoading(false));

  useEffect(() => {
    if (!session) {
      window.history.replaceState({}, '', '/cuenta/login');
      window.dispatchEvent(new PopStateEvent('popstate'));
      return undefined;
    }
    void loadOrders();
    const stream = connectCustomerNotifications((updated) => {
      setOrders((current) => current.map((order) => order.id === updated.id ? { ...order, status: updated.status, orderNumber: updated.orderNumber || order.orderNumber } : order));
    }, () => getCustomerSession()?.accessToken, refreshCustomerAccessToken);
    return () => stream.close();
  }, []);

  if (!session) return <StoreShell><LoadingState label="Redirigiendo al inicio de sesi&oacute;n..." /></StoreShell>;

  const openOrder = (order: Order) => storeApi.get<Order>(`/store/orders/${order.id}`, { auth: true })
    .then(setSelected)
    .catch((reason) => setError(reason instanceof ApiError ? reason.message : 'No se pudo cargar el pedido'));

  const uploadProof = async (file: File) => {
    if (!selected) return;
    setUploading(true);
    const data = new FormData();
    data.append('file', file);
    try {
      const updated = await storeApi.post<Order>(`/store/orders/${selected.id}/payment-proof`, data, { auth: true });
      setSelected(updated);
      setOrders((current) => current.map((order) => order.id === updated.id ? updated : order));
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No se pudo subir el comprobante');
    } finally {
      setUploading(false);
    }
  };

  return <StoreShell active="pedidos">
    <div className="store-page-heading">
      <span className="store-kicker">CUENTA</span>
      <h1>Mis pedidos</h1>
      <p>Consulta el estado y detalle de tus compras.</p>
      {new URLSearchParams(window.location.search).get('created') && <p className="store-inline-success" role="status">Pedido creado correctamente. Puedes revisar su estado aqu&iacute;.</p>}
    </div>
    {loading ? <LoadingState label="Cargando pedidos..." /> : error ? <ErrorState message={error} /> : orders.length === 0 ? <EmptyState>Todav&iacute;a no tienes pedidos.</EmptyState> : <OrdersSurface
      template={template}
      orders={orders}
      selected={selected}
      labels={labels}
      formatCurrency={formatCurrency}
      formatDate={formatDate}
      imageUrl={imageUrl}
      onOpen={openOrder}
      onClose={() => setSelected(null)}
      uploading={uploading}
      onUploadProof={(file) => void uploadProof(file)}
    />}
  </StoreShell>;
}
