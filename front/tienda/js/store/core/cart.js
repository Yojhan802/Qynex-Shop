// Carrito de la tienda — persistido en localStorage (no requiere login para
// armarlo, solo para llegar al checkout). El precio se guarda solo como
// referencia visual: el backend siempre recalcula el precio real al crear
// el pedido (nunca viaja del cliente como fuente de verdad).

const CART_KEY = 'fsp.customer.cart';

function leer() {
  const raw = localStorage.getItem(CART_KEY);
  if (!raw) return [];
  try {
    const items = JSON.parse(raw);
    return Array.isArray(items) ? items : [];
  } catch {
    return [];
  }
}

function guardar(items) {
  localStorage.setItem(CART_KEY, JSON.stringify(items));
}

export function getCart() {
  return leer();
}

export function addToCart(item, quantity = 1) {
  const items = leer();
  const existente = items.find((it) => it.variantId === item.variantId);
  if (existente) {
    existente.quantity += quantity;
  } else {
    items.push({ ...item, quantity });
  }
  guardar(items);
  return items;
}

export function updateCartQuantity(variantId, quantity) {
  let items = leer();
  if (quantity <= 0) {
    items = items.filter((it) => it.variantId !== variantId);
  } else {
    items = items.map((it) => (it.variantId === variantId ? { ...it, quantity } : it));
  }
  guardar(items);
  return items;
}

export function removeFromCart(variantId) {
  const items = leer().filter((it) => it.variantId !== variantId);
  guardar(items);
  return items;
}

export function clearCart() {
  guardar([]);
}

export function cartTotal(items = leer()) {
  return items.reduce((sum, it) => sum + it.unitPrice * it.quantity, 0);
}

export function cartCount(items = leer()) {
  return items.reduce((sum, it) => sum + it.quantity, 0);
}
