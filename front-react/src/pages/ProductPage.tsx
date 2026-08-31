import { useEffect, useMemo, useState } from 'react';
import { storeApi, imageUrl } from '../services/api';
import { addToCart } from '../services/cart';
import type { Product, ProductAttribute } from '../types';
import { formatCurrency } from '../utils';
import { ErrorState, LoadingState } from '../components/States';
import { StoreShell } from '../components/StoreShell';

function attributeGroups(product: Product) {
  const first = product.variants?.find((variant) => variant.attributes?.length)?.attributes ?? [];
  return first.map((attribute) => {
    const values = new Map<number, ProductAttribute>();
    product.variants?.forEach((variant) => variant.attributes?.filter((item) => item.attributeId === attribute.attributeId).forEach((item) => values.set(item.attributeValueId, item)));
    return { ...attribute, values: [...values.values()] };
  });
}

export function ProductPage() {
  const id = useMemo(() => new URLSearchParams(window.location.search).get('id'), []);
  const [product, setProduct] = useState<Product | null>(null);
  const [selectedAttributes, setSelectedAttributes] = useState<Record<number, number>>({});
  const [selectedVariantId, setSelectedVariantId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [gallery, setGallery] = useState(0);
  const [message, setMessage] = useState('');
  const [imageFailed, setImageFailed] = useState(false);

  useEffect(() => {
    if (id) storeApi.get<Product>(`/store/catalog/products/${id}`).then((value) => { setProduct(value); setSelectedVariantId(value.variants?.[0]?.variantId ?? null); }).catch((error) => setMessage(error.message));
  }, [id]);

  const groups = useMemo(() => product ? attributeGroups(product) : [], [product]);
  const selectedVariant = useMemo(() => product?.variants?.find((variant) => {
    if (!groups.length) return variant.variantId === selectedVariantId;
    return groups.every((group) => variant.attributes?.some((item) => item.attributeId === group.attributeId && selectedAttributes[group.attributeId] === item.attributeValueId));
  }), [groups, product, selectedAttributes, selectedVariantId]);
  const images = product ? [...new Set([product.imageUrl, ...(product.images ?? []).sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)).map((entry) => entry.imageUrl)].filter(Boolean) as string[])] : [];
  const price = selectedVariant?.price ?? product?.promoPrice ?? product?.price ?? 0;
  const canBuy = product ? (groups.length ? Boolean(selectedVariant?.variantId && selectedVariant.inStock !== false) : selectedVariant?.inStock !== false && product.inStock !== false) : false;

  if (!product && !message) return <StoreShell><LoadingState label="Cargando producto…" /></StoreShell>;
  if (!product) return <StoreShell><ErrorState message={message || 'No se pudo cargar el producto'} /></StoreShell>;

  const chooseAttribute = (group: { attributeId: number }, value: number, index: number) => {
    setSelectedAttributes((current) => {
      const next = { ...current, [group.attributeId]: value };
      groups.slice(index + 1).forEach((item) => delete next[item.attributeId]);
      return next;
    });
    setMessage('');
  };

  return <StoreShell>
    <div className="store-detail store-product-detail">
      <div className="store-detail-gallery store-product-gallery">
        <div className="store-product-image store-detail-main-image store-product-detail-main">
          <img src={imageFailed ? imageUrl() : imageUrl(images[gallery])} alt={product.name} onError={() => setImageFailed(true)} />
        </div>
        {images.length > 1 && <div className="store-detail-thumbnails store-product-thumbs" role="list" aria-label="Imágenes del producto">
          {images.map((src, index) => <button type="button" key={`${src}-${index}`} className={`store-detail-thumbnail ${gallery === index ? 'is-selected is-active' : ''}`} onClick={() => { setGallery(index); setImageFailed(false); }} aria-label={`Ver imagen ${index + 1}`}>
            <img src={imageUrl(src)} alt="" onError={(event) => { event.currentTarget.src = imageUrl(); }} />
          </button>)}
        </div>}
      </div>
      <section className="store-detail-panel store-product-info">
        <span className="store-product-meta">{product.brandName || product.categoryName || ''}</span>
        <h1>{product.name}</h1>
        <div className="store-product-price store-product-detail-price">{formatCurrency(price)}</div>
        {product.description && <p className="store-product-description">{product.description}</p>}
        {(product.material || product.fit) && <div className="store-product-specs"><span>{product.material && <>Material: <strong>{product.material}</strong></>}</span><span>{product.fit && <>Calce: <strong>{product.fit}</strong></>}</span></div>}
        {groups.length ? groups.map((group, index) => <div className="field store-variant-picker" key={group.attributeId}>
          <div className="field-label-row"><span className="field-label">{group.attributeName}</span>{index === 0 && product.sizeGuideImageUrl && <a href={imageUrl(product.sizeGuideImageUrl)} target="_blank" rel="noopener">Guía de tallas</a>}</div>
          <div className="store-swatches" aria-label={`Opciones de ${group.attributeName}`}>
            {group.values.map((value) => {
              const enabled = product.variants?.some((variant) => variant.attributes?.some((item) => item.attributeId === group.attributeId && item.attributeValueId === value.attributeValueId) && (index === 0 || groups.slice(0, index).every((previous) => variant.attributes?.some((item) => item.attributeId === previous.attributeId && selectedAttributes[previous.attributeId] === item.attributeValueId))));
              return <button type="button" className={`store-swatch ${selectedAttributes[group.attributeId] === value.attributeValueId ? 'is-selected' : ''}`} key={value.attributeValueId} disabled={!enabled} aria-pressed={selectedAttributes[group.attributeId] === value.attributeValueId} onClick={() => chooseAttribute(group, value.attributeValueId, index)}>{value.inputType === 'SWATCH' && value.hexCode && <span className="store-swatch-dot" style={{ backgroundColor: value.hexCode }} aria-hidden="true" />}{value.value}</button>;
            })}
          </div>
        </div>) : product.variants?.length ? <fieldset className="store-variant-picker"><legend>Elige una variante</legend><div className="store-swatches">{product.variants.map((variant) => <button type="button" className={`store-swatch ${selectedVariant?.variantId === variant.variantId ? 'is-selected' : ''}`} key={variant.variantId} disabled={variant.inStock === false} onClick={() => { setSelectedVariantId(variant.variantId); setMessage(''); }}>{variant.variantLabel || `Opción ${variant.variantId}`}</button>)}</div></fieldset> : null}
        <div className="store-purchase-row"><label>Cantidad<input className="input" type="number" min="1" max="99" value={quantity} onChange={(event) => setQuantity(Math.max(1, Number(event.target.value) || 1))} /></label><button className="btn btn-primary" type="button" disabled={!canBuy} onClick={() => { const variantId = selectedVariant?.variantId ?? product.id; addToCart({ variantId, productId: product.id, productName: product.name, variantLabel: selectedVariant?.variantLabel, unitPrice: Number(price), imageUrl: product.imageUrl }, quantity); setMessage('Producto agregado al carrito'); }}>Agregar al carrito</button></div>
        {message && <p className="store-inline-success" role="status">{message}</p>}
      </section>
    </div>
  </StoreShell>;
}
