import { requireSession } from '../core/auth.js';
import { api, ApiError } from '../core/api.js';
import { renderShell } from '../components/shell.js';
import { renderBarList, renderStackedBar } from '../components/charts.js';
import { colorForPaymentMethod } from '../core/payment-colors.js';
import { formatCurrency, formatDateLong } from '../core/format.js';
import { descargarCsv } from '../core/csv.js';

let activeTab = 'ventas';
let range = { from: undefined, to: undefined };
let ultimoReporteVentas = null;
let ultimoReporteCaja = null;

function init() {
  const permisos = new Set(session?.user.permissions ?? []);
  const btnExportar = document.querySelector('#btn-exportar');
  if (!permisos.has('REPORTES_EXPORTAR')) {
    btnExportar.hidden = true;
  } else {
    btnExportar.addEventListener('click', exportarCsv);
  }

  document.querySelectorAll('.tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      activeTab = tab.dataset.tab;
      document.querySelectorAll('.tab').forEach((t) => t.setAttribute('aria-selected', String(t.dataset.tab === activeTab)));
      document.querySelector('#panel-ventas').hidden = activeTab !== 'ventas';
      document.querySelector('#panel-caja').hidden = activeTab !== 'caja';
      cargarPanelActivo();
    });
  });

  document.querySelector('#btn-aplicar-rango').addEventListener('click', () => {
    range.from = document.querySelector('#filter-from').value || undefined;
    range.to = document.querySelector('#filter-to').value || undefined;
    cargarPanelActivo();
  });

  cargarPanelActivo();
}

function cargarPanelActivo() {
  if (activeTab === 'ventas') cargarVentas();
  else cargarCaja();
}

async function cargarVentas() {
  try {
    const [porCategoria, porMetodo, porProducto, porVendedor, porPromotor] = await Promise.all([
      api.get('/reports/sales/by-category', { query: range }),
      api.get('/reports/sales/by-payment-method', { query: range }),
      api.get('/reports/products/top-selling', { query: { ...range, limit: 10 } }),
      api.get('/reports/sales/by-seller', { query: range }),
      api.get('/reports/sales/by-promoter', { query: range }),
    ]);
    ultimoReporteVentas = { porCategoria, porMetodo, porProducto, porVendedor, porPromotor };

    renderChartOrEmpty('#chart-categoria', porCategoria, 'label', 'total');
    renderChartOrEmpty('#chart-vendedor', porVendedor, 'label', 'total');
    renderChartOrEmpty(
      '#chart-productos',
      porProducto.map((p) => ({ name: p.productName, total: p.total })),
      'name',
      'total'
    );

    const metodoContainer = document.querySelector('#chart-metodo-pago');
    if (porMetodo.length === 0) {
      metodoContainer.innerHTML = `<div class="empty-state"><span>Sin ventas en este período</span></div>`;
    } else {
      const conColor = porMetodo.map((m, i) => ({ ...m, color: colorForPaymentMethod(m.label, i) }));
      renderStackedBar(metodoContainer, conColor, { valueKey: 'total', labelKey: 'label', colorKey: 'color' });
    }

    const cardPromotores = document.querySelector('#card-promotores');
    cardPromotores.hidden = porPromotor.length === 0;
    if (porPromotor.length > 0) {
      document.querySelector('#tabla-promotores').innerHTML = porPromotor
        .map(
          (p) => `
        <tr>
          <td class="table-cell-primary">${p.promoterName}</td>
          <td>${p.salesCount}</td>
          <td class="mono">${formatCurrency(p.total)}</td>
        </tr>
      `
        )
        .join('');
    }
  } catch (error) {
    showErrorEnTodos(error);
  }
}

function renderChartOrEmpty(selector, items, labelKey, valueKey) {
  const container = document.querySelector(selector);
  if (!items || items.length === 0) {
    container.innerHTML = `<div class="empty-state"><span>Sin datos en este período</span></div>`;
    return;
  }
  renderBarList(container, items, { valueKey, labelKey });
}

function showErrorEnTodos(error) {
  const message = error instanceof ApiError ? error.message : 'No se pudieron cargar los reportes';
  ['#chart-categoria', '#chart-metodo-pago', '#chart-productos', '#chart-vendedor'].forEach((selector) => {
    document.querySelector(selector).innerHTML = `<div class="empty-state"><span>${message}</span></div>`;
  });
}

async function cargarCaja() {
  const body = document.querySelector('#caja-report-body');
  try {
    const sesiones = await api.get('/reports/cash/sessions', { query: range });
    ultimoReporteCaja = sesiones;
    body.innerHTML = sesiones.length
      ? sesiones
          .map((s) => {
            const diffColor = s.difference === 0 ? 'var(--color-success-text)' : s.difference < 0 ? 'var(--color-danger-text)' : 'var(--color-info-text)';
            return `
            <tr>
              <td class="table-cell-primary">${s.registerName}</td>
              <td>${s.openedByUsername}</td>
              <td>${s.closedByUsername ?? '—'}</td>
              <td class="table-cell-muted">${formatDateLong(s.closedAt)}</td>
              <td class="mono">${formatCurrency(s.expectedAmount)}</td>
              <td class="mono">${formatCurrency(s.countedAmount)}</td>
              <td class="mono" style="color:${diffColor};">${formatCurrency(s.difference)}</td>
            </tr>
          `;
          })
          .join('')
      : `<tr><td colspan="7"><div class="empty-state"><span>Sin cierres de caja en este período</span></div></td></tr>`;
  } catch (error) {
    body.innerHTML = `<tr><td colspan="7"><div class="empty-state"><span>${error instanceof ApiError ? error.message : 'Error al cargar'}</span></div></td></tr>`;
  }
}

function exportarCsv() {
  const fecha = new Date().toISOString().slice(0, 10);
  const rango = range.from || range.to ? ` (${range.from ?? '…'} a ${range.to ?? '…'})` : '';

  if (activeTab === 'ventas') {
    if (!ultimoReporteVentas) return;
    const { porCategoria, porMetodo, porProducto, porVendedor, porPromotor } = ultimoReporteVentas;
    const filas = [
      [`Reporte de ventas${rango}`],
      [],
      ['Ventas por categoría'],
      ['Categoría', 'Total', '% del período'],
      ...porCategoria.map((r) => [r.label, r.total, r.percentage != null ? `${r.percentage.toFixed(1)}%` : '']),
      [],
      ['Ventas por método de pago'],
      ['Método', 'Total', '% del período'],
      ...porMetodo.map((r) => [r.label, r.total, r.percentage != null ? `${r.percentage.toFixed(1)}%` : '']),
      [],
      ['Productos más vendidos'],
      ['Producto', 'Unidades', 'Total'],
      ...porProducto.map((r) => [r.productName, r.units, r.total]),
      [],
      ['Ventas por vendedor'],
      ['Vendedor', 'Total', '% del período'],
      ...porVendedor.map((r) => [r.label, r.total, r.percentage != null ? `${r.percentage.toFixed(1)}%` : '']),
      [],
      ['Ventas por promotor'],
      ['Promotor', 'Ventas', 'Total'],
      ...porPromotor.map((r) => [r.promoterName, r.salesCount, r.total]),
    ];
    descargarCsv(`reporte-ventas-${fecha}.csv`, filas);
  } else {
    if (!ultimoReporteCaja) return;
    const filas = [
      [`Reporte de caja${rango}`],
      [],
      ['Caja', 'Abierta por', 'Cerrada por', 'Cierre', 'Esperado', 'Contado', 'Diferencia'],
      ...ultimoReporteCaja.map((s) => [
        s.registerName,
        s.openedByUsername,
        s.closedByUsername ?? '',
        s.closedAt ?? '',
        s.expectedAmount,
        s.countedAmount,
        s.difference,
      ]),
    ];
    descargarCsv(`reporte-caja-${fecha}.csv`, filas);
  }
}

const session = requireSession();
if (session) {
  renderShell('reportes');
  init();
}
