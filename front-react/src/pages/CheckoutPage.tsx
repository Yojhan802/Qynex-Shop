import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { ApiError, getCustomerSession, imageUrl, storeApi } from '../services/api';
import { cartTotal, clearCart, getCart } from '../services/cart';
import { getDepartamentos, getDistritos, getProvincias } from '../services/ubigeo';
import type { Order, PaymentMethod, PaymentProvider } from '../types';
import { formatCurrency } from '../utils';
import { ErrorState, LoadingState } from '../components/States';
import { StoreShell } from '../components/StoreShell';
import { useStoreTemplate } from '../components/TemplateProvider';
import { CheckoutSurface } from '../templates/CheckoutSurface';

declare global {
  interface Window { CulqiCheckout?: new (key: string, config: unknown) => { culqi?: () => void; token?: { id?: string }; error?: { user_message?: string; merchant_message?: string }; close: () => void; open: () => void }; VisanetCheckout?: { configure: (config: unknown) => void; open: () => void }; Izipay?: new (config: unknown) => { LoadForm: (config: unknown) => void }; }
}

function loadScript(url: string, key: 'culqi' | 'niubiz' | 'izipay') {
  const selector = `script[data-qynex-sdk="${key}"]`;
  if (document.querySelector(selector)) return Promise.resolve();
  return new Promise<void>((resolve, reject) => { const script = document.createElement('script'); script.src = url; script.async = true; script.dataset.qynexSdk = key; script.onload = () => resolve(); script.onerror = () => reject(new Error(`No se pudo cargar el checkout de ${key}`)); document.head.appendChild(script); });
}

async function onlinePayment(order: Order, provider: PaymentProvider, session: NonNullable<ReturnType<typeof getCustomerSession>>, form: HTMLFormElement) {
  const transaction = await storeApi.post<{ id: number; amount: number; currencyCode?: string }>(`/store/orders/${order.id}/payment-transactions`, { provider: provider.provider }, { auth: true, headers: { 'Idempotency-Key': `store-order-${order.id}-${provider.provider}` } });
  const checkout = await storeApi.get<{ sessionToken?: string; merchantCode?: string; correlationId?: string; publicKey?: string; purchaseNumber?: string; currencyCode?: string; amount?: number; scriptUrl?: string; expirationMinutes?: number }>(`/store/payment-transactions/${transaction.id}/checkout`, { auth: true });
  let sourceId = '';
  if (provider.provider === 'CULQI') {
    if (!provider.publicKey) throw new Error('La empresa no ha configurado la llave pública de Culqi');
    await loadScript('https://js.culqi.com/checkout-js', 'culqi');
    sourceId = await new Promise<string>((resolve, reject) => { if (!window.CulqiCheckout) return reject(new Error('Culqi no está disponible')); const checkoutUi = new window.CulqiCheckout(provider.publicKey!, { settings: { title: 'Pago de pedido', currency: checkout.currencyCode || 'PEN', amount: Math.round(Number(transaction.amount) * 100) }, client: { email: session.customer.email }, options: { lang: 'auto', installments: true, modal: true, paymentMethods: { tarjeta: true } } }); checkoutUi.culqi = () => checkoutUi.token?.id ? (checkoutUi.close(), resolve(checkoutUi.token.id!)) : reject(new Error(checkoutUi.error?.user_message || checkoutUi.error?.merchant_message || 'No se pudo tokenizar el pago')); checkoutUi.open(); });
  } else if (provider.provider === 'NIUBIZ') {
    if (!checkout.sessionToken || !checkout.merchantCode) throw new Error('Niubiz no devolvió una sesión válida de pago');
    await loadScript(checkout.scriptUrl || 'https://static-content-qas.vnforapps.com/v2/js/checkout.js?qa=true', 'niubiz');
    sourceId = await new Promise<string>((resolve, reject) => { if (!window.VisanetCheckout) return reject(new Error('Niubiz no está disponible')); window.VisanetCheckout.configure({ sessiontoken: checkout.sessionToken, channel: 'web', merchantid: checkout.merchantCode, purchasenumber: checkout.purchaseNumber, amount: checkout.amount, currency: checkout.currencyCode, expirationminutes: String(checkout.expirationMinutes || 20), timeouturl: 'about:blank', cardholdername: session.customer.fullName, cardholderemail: session.customer.email, complete: (value: unknown) => { const data = value as { transactionToken?: string; tokenId?: string; token?: string; order?: { transactionToken?: string; tokenId?: string } }; const token = typeof value === 'string' ? value : data.transactionToken || data.tokenId || data.token || data.order?.transactionToken || data.order?.tokenId; token ? resolve(String(token)) : reject(new Error('Niubiz no devolvió el token de transacción')); } }); window.VisanetCheckout.open(); });
  } else if (provider.provider === 'IZIPAY') {
    if (!checkout.sessionToken || !checkout.merchantCode || !checkout.correlationId || !checkout.publicKey) throw new Error('Izipay no devolvió una sesión válida de pago');
    await loadScript(checkout.scriptUrl || 'https://sandbox-checkout.izipay.pe/payments/v1/js/index.js', 'izipay');
    sourceId = await new Promise<string>((resolve, reject) => { if (!window.Izipay) return reject(new Error('Izipay no está disponible')); try { const fullName = session.customer.fullName.trim().split(/\s+/); const sdk = new window.Izipay({ config: { transactionId: checkout.correlationId, action: 'pay', merchantCode: checkout.merchantCode, order: { orderNumber: checkout.purchaseNumber, currency: checkout.currencyCode, amount: checkout.amount, processType: 'AT', merchantBuyerId: `customer-${session.customer.id}`, dateTimeTransaction: new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14) }, billing: { firstName: fullName.shift() || 'Cliente', lastName: fullName.join(' ') || 'Final', email: session.customer.email, phoneNumber: String(new FormData(form).get('phone') || session.customer.phone || ''), street: String(new FormData(form).get('address') || '') }, shipping: { firstName: fullName[0] || 'Cliente', lastName: fullName.slice(1).join(' ') || 'Final', email: session.customer.email } } }); sdk.LoadForm({ authorization: checkout.sessionToken, keyRSA: checkout.publicKey, callbackResponse: () => resolve(checkout.correlationId!) }); } catch (error) { reject(error instanceof Error ? error : new Error('No se pudo abrir Izipay')); } });
  } else throw new Error(`El checkout de ${provider.provider} no está disponible`);
  const result = await storeApi.post<{ status: string; failureMessage?: string }>(`/store/payment-transactions/${transaction.id}/charge`, { sourceId }, { auth: true });
  if (result.status !== 'APPROVED' && result.status !== 'PENDING') throw new Error(result.failureMessage || 'El pago no fue aprobado');
  return result.status;
}

export function CheckoutPage() {
  const session = useMemo(() => getCustomerSession(), []);
  const template = useStoreTemplate();
  const items = getCart();
  const [methods, setMethods] = useState<PaymentMethod[]>([]);
  const [providers, setProviders] = useState<PaymentProvider[]>([]);
  const [shipping, setShipping] = useState<{ flatRate?: number; freeShippingDistrict?: string | null }>({});
  const [selectedMethodId, setSelectedMethodId] = useState<number | null>(null);
  const [selectedProvider, setSelectedProvider] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [provinceId, setProvinceId] = useState('');
  const [district, setDistrict] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [proofFile, setProofFile] = useState<File | null>(null);
  const departments = getDepartamentos();
  const provinces = getProvincias(departmentId);
  const districts = getDistritos(provinceId);
  const selectedMethod = methods.find((method) => method.id === selectedMethodId);
  const availableMethods = useMemo(() => methods.filter((method) => method.code !== 'CONTRAENTREGA' || district.toLowerCase() === String(shipping.freeShippingDistrict || '').toLowerCase()), [methods, district, shipping.freeShippingDistrict]);
  const delivery = selectedMethod?.code === 'CONTRAENTREGA' ? 0 : Number(shipping.flatRate ?? 0);

  useEffect(() => {
    if (!session) return;
    Promise.all([storeApi.get<PaymentMethod[]>('/store/catalog/payment-methods'), storeApi.get<typeof shipping>('/store/catalog/shipping-info'), storeApi.get<PaymentProvider[]>('/store/catalog/payment-providers')]).then(([paymentMethods, shippingInfo, paymentProviders]) => { setMethods(paymentMethods ?? []); setSelectedMethodId(paymentMethods?.[0]?.id ?? null); setShipping(shippingInfo ?? {}); setProviders(paymentProviders ?? []); setSelectedProvider(paymentProviders?.[0]?.provider ?? ''); }).catch((reason) => setError(reason.message)).finally(() => setLoading(false));
  }, [session]);

  useEffect(() => { if (selectedMethodId && !availableMethods.some((method) => method.id === selectedMethodId)) setSelectedMethodId(null); }, [district, selectedMethodId, availableMethods]);

  if (!session) { window.history.replaceState({}, '', '/cuenta/login?volver=checkout'); window.dispatchEvent(new PopStateEvent('popstate')); return <StoreShell><LoadingState label="Redirigiendo al inicio de sesión…" /></StoreShell>; }
  if (!items.length) return <StoreShell><ErrorState message="Tu carrito está vacío." /></StoreShell>;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!selectedMethod) return setError('Selecciona un método de pago.');
    if (!departmentId || !provinceId || !district) return setError('Completa departamento, provincia y distrito.');
    setSubmitting(true); setError('');
    try {
      const formData = new FormData(form);
      if (!session) throw new Error('La sesión de cliente expiró. Vuelve a iniciar sesión.');
      const order = await storeApi.post<Order>('/store/orders', { items: items.map((item) => ({ variantId: item.variantId, quantity: item.quantity })), paymentMethodId: selectedMethod.id, paymentReference: String(formData.get('paymentReference') || '').trim() || null, recipientDni: String(formData.get('recipientDni') || '').trim(), recipientFirstName: String(formData.get('recipientFirstName') || '').trim(), recipientLastNamePaterno: String(formData.get('recipientLastNamePaterno') || '').trim(), recipientLastNameMaterno: String(formData.get('recipientLastNameMaterno') || '').trim(), phone: String(formData.get('phone') || '').trim(), address: String(formData.get('address') || '').trim(), department: departments.find((item) => item.id === departmentId)?.nombre || '', province: provinces.find((item) => item.id === provinceId)?.nombre || '', district, notes: String(formData.get('notes') || '').trim() || null }, { auth: true });
      let paymentStatus = 'CREATED';
      if (selectedMethod.type === 'CARD') { const provider = providers.find((entry) => entry.provider === selectedProvider); if (!provider) throw new Error('Selecciona una pasarela de pago.'); paymentStatus = await onlinePayment(order, provider, session, form); }
      if (proofFile) { const proof = new FormData(); proof.append('file', proofFile); await storeApi.post(`/store/orders/${order.id}/payment-proof`, proof, { auth: true }); }
      clearCart(); window.history.pushState({}, '', `/cuenta/pedidos?created=${paymentStatus === 'PENDING' ? 'pending' : 'success'}`); window.dispatchEvent(new PopStateEvent('popstate'));
    } catch (reason) { setError(reason instanceof ApiError ? reason.message : reason instanceof Error ? reason.message : 'No se pudo crear el pedido'); } finally { setSubmitting(false); }
  }

  return <StoreShell><div className="store-page-heading"><span className="store-kicker">PASO 2 · DATOS Y PAGO</span><h1>Finalizar compra</h1><p>Completa tus datos para recibir el pedido.</p></div>{loading ? <LoadingState label="Cargando métodos de pago…" /> : <div className="template-checkout-host" id="checkout-content"><CheckoutSurface template={template} session={session} items={items} departments={departments} provinces={provinces} districts={districts} departmentId={departmentId} provinceId={provinceId} district={district} setDepartmentId={setDepartmentId} setProvinceId={setProvinceId} setDistrict={setDistrict} providers={providers} selectedMethodId={selectedMethodId} selectedMethod={selectedMethod} selectedProvider={selectedProvider} setSelectedMethodId={setSelectedMethodId} setSelectedProvider={setSelectedProvider} availableMethods={availableMethods} delivery={delivery} error={error} submitting={submitting} setProofFile={setProofFile} onSubmit={submit} imageUrl={imageUrl} formatCurrency={formatCurrency} /> </div>}</StoreShell>;
}

