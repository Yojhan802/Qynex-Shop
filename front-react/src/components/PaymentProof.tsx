import { useEffect, useState } from 'react';
import { api, storeApi } from '../services/api';

/**
 * Comprobante de pago de un pedido: la captura de Yape o de la transferencia que subió el
 * cliente, con su nombre y su número de operación.
 *
 * No se sirve por `/uploads/orders/**` —esa ruta está bloqueada justamente por eso— sino por
 * un endpoint que comprueba quién pregunta. Como la API autentica con Bearer y no con
 * cookies, un `<img src>` no puede identificarse: hay que pedir el archivo con fetch y
 * mostrarlo desde un blob local.
 *
 * `como` decide a qué endpoint se pide: el del staff de la empresa o el del propio cliente.
 */
export function PaymentProof({ orderId, como, className }: {
  orderId: number;
  como: 'staff' | 'cliente';
  className?: string;
}) {
  const [url, setUrl] = useState<string | null>(null);
  const [fallo, setFallo] = useState(false);

  useEffect(() => {
    let vigente = true;
    let creada: string | null = null;
    setUrl(null);
    setFallo(false);

    const pedido = como === 'staff'
      ? api.download(`/orders/${orderId}/payment-proof`, { auth: 'staff' })
      : storeApi.download(`/store/orders/${orderId}/payment-proof`, { auth: true });

    pedido
      .then(({ blob }) => {
        // Si el diálogo ya se cerró, no se llega a crear la URL y no hay nada que revocar.
        if (!vigente) return;
        creada = URL.createObjectURL(blob);
        setUrl(creada);
      })
      .catch(() => { if (vigente) setFallo(true); });

    return () => {
      vigente = false;
      if (creada) URL.revokeObjectURL(creada);
    };
  }, [orderId, como]);

  if (fallo) return <small>No se pudo cargar el comprobante.</small>;
  if (!url) return <small>Cargando comprobante…</small>;
  return (
    <a href={url} target="_blank" rel="noreferrer">
      <img className={className} src={url} alt="Comprobante de pago enviado por el cliente" />
    </a>
  );
}
