import type { FormEvent } from 'react';
import type { CartItem, CustomerSession, Order, PaymentMethod, PaymentProvider, StoreTemplate } from '../types';
import type { formatCurrency } from '../utils';

interface UbigeoOption { id: string; nombre: string; }

export interface CheckoutSurfaceProps {
  template: StoreTemplate;
  session: CustomerSession;
  items: CartItem[];
  departments: UbigeoOption[];
  provinces: UbigeoOption[];
  districts: UbigeoOption[];
  departmentId: string;
  provinceId: string;
  district: string;
  setDepartmentId: (value: string) => void;
  setProvinceId: (value: string) => void;
  setDistrict: (value: string) => void;
  providers: PaymentProvider[];
  selectedMethodId: number | null;
  selectedMethod?: PaymentMethod;
  selectedProvider: string;
  setSelectedMethodId: (value: number) => void;
  setSelectedProvider: (value: string) => void;
  availableMethods: PaymentMethod[];
  delivery: number;
  error: string;
  submitting: boolean;
  setProofFile: (file: File | null) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  imageUrl: (value?: string | null) => string;
  formatCurrency: typeof formatCurrency;
}

export function CheckoutSurface({ template, session, items, departments, provinces, districts, departmentId, provinceId, district, setDepartmentId, setProvinceId, setDistrict, providers, selectedMethodId, selectedMethod, selectedProvider, setSelectedMethodId, setSelectedProvider, availableMethods, delivery, error, submitting, setProofFile, onSubmit, imageUrl, formatCurrency }: CheckoutSurfaceProps) {
  const variant = template.toLowerCase();
  return <form className={`template-checkout template-checkout-${variant}`} id="checkout-form" onSubmit={onSubmit} data-template-surface="checkout">
    <div className="template-checkout-eyebrow">01 / ENTREGA Y PAGO</div>
    <p className="template-checkout-intro">Datos de quien recibe el envío — la courier los exige para registrar el paquete.</p>
    <div className="template-checkout-layout">
      <section className="template-checkout-delivery">
        <div className="template-panel-heading"><span className="template-panel-kicker">INFORMACIÓN</span><h2>Datos de entrega</h2></div>
        <div className="template-form-grid template-checkout-recipient-grid">
          <label className="template-field"><span>DNI</span><input required maxLength={15} inputMode="numeric" name="recipientDni" /></label>
          <label className="template-field"><span>Nombres</span><input required defaultValue={session.customer.fullName.split(' ')[0]} name="recipientFirstName" /></label>
          <label className="template-field"><span>Apellido paterno</span><input required name="recipientLastNamePaterno" /></label>
          <label className="template-field"><span>Apellido materno</span><input required name="recipientLastNameMaterno" /></label>
        </div>
        <label className="template-field"><span>Teléfono</span><input required defaultValue={session.customer.phone || ''} name="phone" /></label>
        <label className="template-field"><span>Dirección</span><input required placeholder="Av./Jr./Calle, número, referencia" name="address" /></label>
        <div className="template-form-grid template-checkout-location-grid">
          <label className="template-field"><span>Departamento</span><select required value={departmentId} onChange={(event) => { setDepartmentId(event.target.value); setProvinceId(''); setDistrict(''); }}><option value="">Selecciona…</option>{departments.map((item) => <option value={item.id} key={item.id}>{item.nombre}</option>)}</select></label>
          <label className="template-field"><span>Provincia</span><select required disabled={!departmentId} value={provinceId} onChange={(event) => { setProvinceId(event.target.value); setDistrict(''); }}><option value="">{departmentId ? 'Selecciona…' : 'Elige un departamento primero'}</option>{provinces.map((item) => <option value={item.id} key={item.id}>{item.nombre}</option>)}</select></label>
          <label className="template-field template-field-wide"><span>Distrito</span><select required disabled={!provinceId} value={district} onChange={(event) => setDistrict(event.target.value)}><option value="">{provinceId ? 'Selecciona…' : 'Elige una provincia primero'}</option>{districts.map((item) => <option value={item.nombre} key={item.id}>{item.nombre}</option>)}</select></label>
        </div>
        <label className="template-field"><span>Notas <small>(opcional)</small></span><textarea name="notes" rows={3} /></label>
      </section>
      <aside className="template-checkout-order" id="order-summary">
        <div className="template-panel-heading"><span className="template-panel-kicker">TU COMPRA</span><h2>Resumen</h2></div>
        <div className="template-order-lines">{items.map((item) => <div className="template-order-line" key={item.variantId}><span>{item.productName} <small>({item.variantLabel || 'Producto'}) × {item.quantity}</small></span><strong>{formatCurrency(item.unitPrice * item.quantity)}</strong></div>)}</div>
        <div className="template-total-line"><span>Subtotal</span><strong>{formatCurrency(items.reduce((total, item) => total + item.unitPrice * item.quantity, 0))}</strong></div>
        <div className="template-total-line"><span>Envío</span><strong>{delivery ? formatCurrency(delivery) : 'Gratis'}</strong></div>
        <div className="template-grand-total"><span>Total</span><strong>{formatCurrency(items.reduce((total, item) => total + item.unitPrice * item.quantity, 0) + delivery)}</strong></div>
        <div className="template-payment-block"><span className="template-payment-title">Método de pago</span><div className="template-payment-options">{availableMethods.map((method) => <label className={`template-payment-option${selectedMethodId === method.id ? ' is-selected' : ''}`} key={method.id}><input type="radio" name="paymentMethod" checked={selectedMethodId === method.id} onChange={() => setSelectedMethodId(method.id)} /><span><strong>{method.name}</strong>{method.code === 'CONTRAENTREGA' && <small> · envío gratis</small>}{method.instructions && <small>{method.instructions}</small>}</span></label>)}</div>
          {selectedMethod?.qrImageUrl && <div className="template-payment-qr"><img src={imageUrl(selectedMethod.qrImageUrl)} alt={`QR de ${selectedMethod.name}`} onError={(event) => { event.currentTarget.hidden = true; }} />{selectedMethod.accountHolder && <span>{selectedMethod.accountHolder}</span>}{selectedMethod.accountNumber && <span>{selectedMethod.accountNumber}</span>}</div>}
          {selectedMethod?.accountNumber && !selectedMethod.qrImageUrl && <p className="template-payment-account"><strong>{selectedMethod.accountHolder}</strong> — {selectedMethod.accountNumber}</p>}
          {selectedMethod?.type === 'CARD' && <label className="template-field template-provider-field"><span>Pasarela</span><select required name="provider" value={selectedProvider} onChange={(event) => setSelectedProvider(event.target.value)}><option value="">Selecciona una pasarela</option>{providers.map((provider) => <option value={provider.provider} key={provider.provider}>{provider.displayName || provider.provider}</option>)}</select><small>El pago se procesa con las credenciales configuradas por la empresa.</small></label>}
        </div>
        {selectedMethod?.requiresReference && <label className="template-field"><span>Número de operación</span><input name="paymentReference" /></label>}
        {selectedMethod?.type === 'DIGITAL_WALLET' && <label className="template-field"><span>Comprobante de pago <small>(opcional)</small></span><input accept="image/png,image/jpeg,image/webp" type="file" onChange={(event) => setProofFile(event.target.files?.[0] || null)} /><small>Puedes subirlo ahora o después desde Mis pedidos.</small></label>}
        {error && <div className="template-error" role="alert">{error}</div>}
        <button className="template-submit" disabled={submitting} type="submit">{submitting ? 'Procesando…' : 'Confirmar pedido'}</button>
      </aside>
    </div>
  </form>;
}
