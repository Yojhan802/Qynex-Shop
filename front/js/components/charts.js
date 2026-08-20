// Componentes de gráfico en SVG/HTML plano, sin librerías externas.
// Especificaciones (marcas finas, gaps de 2px, leyenda siempre visible,
// etiquetas nunca sobre el color) siguen la skill de dataviz del proyecto.

import { formatCurrency, formatCompact } from '../core/format.js';

/**
 * Lista de barras horizontales para rankings de una sola serie
 * (categoría, producto o vendedor). Todas las barras comparten el mismo
 * color: es identidad de "una serie", no una rampa de valor.
 */
export function renderBarList(container, items, { valueKey, labelKey, color = 'var(--brand-accent)', formatValue = formatCurrency }) {
  const max = Math.max(...items.map((item) => item[valueKey]));
  container.innerHTML = items
    .map((item) => {
      const pct = max > 0 ? Math.round((item[valueKey] / max) * 100) : 0;
      return `
        <div class="viz-bar-row">
          <span class="viz-bar-label" title="${item[labelKey]}">${item[labelKey]}</span>
          <span class="viz-bar-track">
            <span class="viz-bar-fill" style="width:${pct}%; background:${color};"></span>
          </span>
          <span class="viz-bar-value">${formatValue(item[valueKey])}</span>
        </div>
      `;
    })
    .join('');
}

/**
 * Barra apilada horizontal única para relaciones parte-todo (p. ej. métodos
 * de pago). El donut queda deprioritizado a propósito: una barra compara
 * magnitudes con más precisión que los ángulos de un donut.
 */
export function renderStackedBar(container, items, { valueKey, labelKey, colorKey }) {
  const total = items.reduce((sum, item) => sum + item[valueKey], 0);
  const bar = document.createElement('div');
  bar.style.display = 'flex';
  bar.style.height = '20px';
  bar.style.borderRadius = 'var(--radius-full)';
  bar.style.overflow = 'hidden';
  bar.style.gap = '2px';

  items.forEach((item) => {
    const pct = total > 0 ? (item[valueKey] / total) * 100 : 0;
    const segment = document.createElement('div');
    segment.style.width = `${pct}%`;
    segment.style.background = item[colorKey];
    segment.title = `${item[labelKey]}: ${formatCurrency(item[valueKey])}`;
    bar.appendChild(segment);
  });

  const legend = document.createElement('div');
  legend.className = 'viz-legend';
  legend.innerHTML = items
    .map((item) => {
      const pct = total > 0 ? Math.round((item[valueKey] / total) * 100) : 0;
      return `
        <span class="viz-legend-item">
          <span class="viz-legend-swatch" style="background:${item[colorKey]}"></span>
          ${item[labelKey]} · <span class="viz-legend-value">${pct}%</span>
        </span>
      `;
    })
    .join('');

  container.innerHTML = '';
  const wrap = document.createElement('div');
  wrap.style.padding = '0 var(--space-5) var(--space-2)';
  wrap.appendChild(bar);
  container.appendChild(wrap);
  container.appendChild(legend);
}

/**
 * Gráfico de línea de una sola serie (p. ej. ventas por día). Línea de 2px,
 * relleno al 10%, cuadrícula horizontal en gris recesivo, y una sola
 * etiqueta directa en el punto más alto (etiquetar selectivamente, nunca
 * un valor por punto).
 */
export function renderLineChart(container, items, { xKey, yKey, width = 560, height = 200 }) {
  const padding = { top: 16, right: 16, bottom: 28, left: 16 };
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;

  const values = items.map((item) => item[yKey]);
  const maxValue = Math.max(...values, 0) * 1.15 || 1; // evita división entre 0 cuando todos los valores son 0
  const stepX = plotWidth / (items.length - 1);

  const points = items.map((item, index) => {
    const x = padding.left + stepX * index;
    const y = padding.top + plotHeight - (item[yKey] / maxValue) * plotHeight;
    return { x, y, item };
  });

  const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ');
  const areaPath = `${linePath} L${points[points.length - 1].x.toFixed(1)},${padding.top + plotHeight} L${points[0].x.toFixed(1)},${padding.top + plotHeight} Z`;

  const peak = points.reduce((max, p) => (p.item[yKey] > max.item[yKey] ? p : max), points[0]);

  const gridLines = [0.25, 0.5, 0.75, 1].map((fraction) => {
    const y = padding.top + plotHeight * (1 - fraction);
    return `<line x1="${padding.left}" y1="${y.toFixed(1)}" x2="${width - padding.right}" y2="${y.toFixed(1)}" stroke="var(--chart-grid)" stroke-width="1" />`;
  }).join('');

  const xLabels = points
    .map((p) => `<text x="${p.x.toFixed(1)}" y="${height - 8}" text-anchor="middle" font-size="11" fill="var(--chart-muted)">${p.item[xKey]}</text>`)
    .join('');

  const dots = points
    .map((p) => `<circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="3" fill="var(--brand-accent)" stroke="var(--color-surface)" stroke-width="2" />`)
    .join('');

  const peakLabel = `
    <text x="${peak.x.toFixed(1)}" y="${(peak.y - 12).toFixed(1)}" text-anchor="middle" font-size="12" font-weight="700" fill="var(--color-text)">
      ${formatCompact(peak.item[yKey])}
    </text>
  `;

  container.innerHTML = `
    <svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Ventas por día de la semana">
      ${gridLines}
      <path d="${areaPath}" fill="var(--brand-accent)" opacity="0.1" />
      <path d="${linePath}" fill="none" stroke="var(--brand-accent)" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />
      ${dots}
      ${peakLabel}
      ${xLabels}
    </svg>
  `;
}
