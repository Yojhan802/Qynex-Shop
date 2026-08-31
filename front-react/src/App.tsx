import { useEffect, useState } from 'react';
import { TemplateProvider, resolveStorePage } from './components/TemplateProvider';
import { StoreHomePage } from './pages/StoreHomePage';
import { ProductPage } from './pages/ProductPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { CustomerAuthPage } from './pages/CustomerAuthPage';
import { OrdersPage } from './pages/OrdersPage';
import { AdminDashboardPage, AdminLoginPage, AdminModulePage } from './pages/AdminPages';
import { AdminChangePasswordPage } from './pages/AdminChangePasswordPage';
import { RouteTransition, TemplateMotion } from './templates/TemplateMotion';

function route(pathname: string) {
  const path = pathname.replace(/\/$/, '') || '/';
  if (path === '/producto') return <ProductPage />;
  if (path === '/carrito') return <CartPage />;
  if (path === '/checkout') return <CheckoutPage />;
  if (path === '/cuenta/login') return <CustomerAuthPage />;
  if (path === '/cuenta/registro') return <CustomerAuthPage register />;
  if (path === '/cuenta/pedidos') return <OrdersPage />;
  if (path === '/admin/login') return <AdminLoginPage />;
  if (path === '/login.html') return <AdminLoginPage />;
  if (path === '/admin/cambiar-contrasena') return <AdminChangePasswordPage />;
  if (path === '/cambiar-contrasena.html') return <AdminChangePasswordPage />;
  if (path === '/admin/dashboard') return <AdminDashboardPage />;
  if (path === '/dashboard.html') return <AdminDashboardPage />;
  if (path.startsWith('/admin/')) return <AdminModulePage title={path.split('/').pop()?.replaceAll('-', ' ') || 'Módulo'} />;
  return <StoreHomePage />;
}

export function App() { const [location, setLocation] = useState(window.location.href); useEffect(() => { const update = () => { setLocation(window.location.href); document.body.dataset.storePage = resolveStorePage(window.location.pathname); window.scrollTo({ top: 0, behavior: 'auto' }); }; update(); window.addEventListener('popstate', update); return () => window.removeEventListener('popstate', update); }, []); return <TemplateProvider><TemplateMotion><RouteTransition routeKey={location}>{route(window.location.pathname)}</RouteTransition></TemplateMotion></TemplateProvider>; }
