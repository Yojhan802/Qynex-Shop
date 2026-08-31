import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { StoreConfig, StoreTemplate } from '../types';
import { storeApi } from '../services/api';
import { isValidColor } from '../utils';

const allowed: StoreTemplate[] = ['CLASSIC', 'MINIMAL', 'FASHION', 'SPORT', 'LUXURY', 'BOUTIQUE', 'CATALOG', 'MARKET', 'EDITORIAL', 'URBAN'];
const TemplateContext = createContext<StoreTemplate>('CLASSIC');

export function useStoreTemplate() { return useContext(TemplateContext); }

function normalizeTemplate(value?: string | null): StoreTemplate {
  const normalized = value?.trim().toUpperCase() as StoreTemplate | undefined;
  return normalized && allowed.includes(normalized) ? normalized : 'CLASSIC';
}

export function resolveStorePage(pathname: string) {
  if (pathname.includes('/carrito')) return 'cart';
  if (pathname.includes('/checkout')) return 'checkout';
  if (pathname.includes('/cuenta/pedidos')) return 'orders';
  if (pathname.includes('/producto')) return 'product';
  if (pathname.includes('/cuenta/')) return 'account';
  return 'home';
}

export function TemplateProvider({ children }: { children: ReactNode }) {
  const preview = new URLSearchParams(window.location.search).get('previewTemplate');
  const [template, setTemplate] = useState<StoreTemplate>(normalizeTemplate(preview));
  const isStoreRoute = !window.location.pathname.startsWith('/admin');
  const [ready, setReady] = useState(!isStoreRoute || Boolean(preview));

  useEffect(() => {
    let active = true;
    if (!isStoreRoute) return () => { active = false; };
    storeApi.get<StoreConfig>('/store/catalog/config').then((config) => {
      if (!active || preview) return;
      setTemplate(normalizeTemplate(config?.template));
      applyBrand(config);
    }).catch(() => undefined).finally(() => { if (active) setReady(true); });
    document.body.dataset.storeTemplate = normalizeTemplate(preview);
    return () => { active = false; };
  }, [isStoreRoute, preview]);

  useEffect(() => {
    document.body.dataset.storeTemplate = template;
    document.body.dataset.storeTemplateReady = ready ? 'true' : 'false';
    document.body.dataset.storePage = resolveStorePage(window.location.pathname);
    document.body.classList.toggle('store-body', isStoreRoute);
    document.body.classList.toggle(`store-template-${template.toLowerCase()}`, isStoreRoute);
    document.body.classList.toggle('react-template-loading', isStoreRoute && !ready);
    return () => {
      document.body.classList.remove('store-body', 'react-template-loading', ...allowed.map((key) => `store-template-${key.toLowerCase()}`));
    };
  }, [isStoreRoute, ready, template]);

  if (isStoreRoute && !ready) return <div className="store-template-loading" role="status"><span className="store-spinner" aria-hidden="true" />Preparando la tienda…</div>;
  return <TemplateContext.Provider value={template}>{children}</TemplateContext.Provider>;
}

export function applyBrand(config?: StoreConfig | null) {
  const params = new URLSearchParams(window.location.search);
  const values = {
    '--brand-black': params.get('previewPrimaryColor') ?? config?.primaryColor,
    '--brand-accent': params.get('previewAccentColor') ?? config?.accentColor,
    '--color-background': params.get('previewBackgroundColor') ?? config?.backgroundColor,
  };
  Object.entries(values).forEach(([name, value]) => {
    if (isValidColor(value)) document.body.style.setProperty(name, value);
  });
}
