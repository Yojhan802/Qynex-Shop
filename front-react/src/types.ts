export type StoreTemplate =
  | 'CLASSIC' | 'MINIMAL' | 'FASHION' | 'SPORT' | 'LUXURY'
  | 'BOUTIQUE' | 'CATALOG' | 'MARKET' | 'EDITORIAL' | 'URBAN';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages?: number;
  number?: number;
  size?: number;
}

export interface Category { id: number; name: string; imageUrl?: string | null; }
export interface Brand { id: number; name: string; }
export interface PublicSearchSuggestion { type: string; id: number; title: string; subtitle?: string | null; imageUrl?: string | null; }
export interface StorefrontBanner { id: number; imageUrl?: string | null; headline?: string | null; description?: string | null; ctaLabel?: string | null; ctaUrl?: string | null; }
export interface ProductImage { id?: number; imageUrl: string; sortOrder?: number; }
export interface ProductAttribute {
  attributeId: number;
  attributeName: string;
  attributeValueId: number;
  value: string;
  inputType?: 'TEXT' | 'SWATCH' | string;
  hexCode?: string | null;
}
export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  promoPrice?: number | null;
  imageUrl?: string | null;
  images?: ProductImage[];
  brandName?: string | null;
  categoryName?: string | null;
  material?: string | null;
  fit?: string | null;
  sizeGuideImageUrl?: string | null;
  inStock?: boolean;
  colors?: Array<{ name: string; hexCode?: string | null }>;
  variants?: Array<{
    variantId: number;
    variantLabel?: string;
    inStock?: boolean;
    price?: number;
    attributes?: ProductAttribute[];
  }>;
}

export interface StoreConfig {
  name?: string | null;
  template?: StoreTemplate | string;
  primaryColor?: string | null;
  accentColor?: string | null;
  backgroundColor?: string | null;
  logoUrl?: string | null;
}

export interface CustomerSession {
  accessToken: string;
  refreshToken: string;
  customer: { id: number; email: string; fullName: string; phone?: string | null };
}

export interface StaffSession {
  accessToken: string;
  refreshToken: string;
  user: { id: number; username: string; fullName: string; permissions: string[]; mustChangePassword?: boolean };
}

export interface CartItem {
  variantId: number;
  productId?: number;
  productName: string;
  variantLabel?: string;
  unitPrice: number;
  imageUrl?: string | null;
  quantity: number;
}

export interface PaymentMethod {
  id: number;
  code?: string;
  name: string;
  type: string;
  requiresReference?: boolean;
  accountHolder?: string | null;
  accountNumber?: string | null;
  qrImageUrl?: string | null;
  instructions?: string | null;
  enabled?: boolean;
}
export interface PaymentProvider { provider: string; displayName?: string; publicKey?: string | null; enabled?: boolean; }
export interface Order {
  id: number;
  orderNumber: string;
  createdAt: string;
  status: string;
  total: number;
  subtotal?: number;
  shippingCost?: number;
  paymentMethodName?: string;
  paymentProofUrl?: string | null;
  address?: string;
  district?: string;
  province?: string;
  department?: string;
  cancellationReason?: string | null;
  confirmedAt?: string | null;
  items?: Array<{ productName: string; variantLabel?: string; quantity: number; subtotal: number }>;
}
