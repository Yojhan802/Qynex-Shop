// Renderiza un EAN-13 real en SVG (patrón de barras estándar), sin
// dependencias externas. Coincide con los códigos que genera el backend
// (Ean13Generator, prefijo 775).

const L_CODE = {
  0: '0001101', 1: '0011001', 2: '0010011', 3: '0111101', 4: '0100011',
  5: '0110001', 6: '0101111', 7: '0111011', 8: '0110111', 9: '0001011',
};
const G_CODE = {
  0: '0100111', 1: '0110011', 2: '0011011', 3: '0100001', 4: '0011101',
  5: '0111001', 6: '0000101', 7: '0010001', 8: '0001001', 9: '0010111',
};
const R_CODE = {
  0: '1110010', 1: '1100110', 2: '1101100', 3: '1000010', 4: '1011100',
  5: '1001110', 6: '1010000', 7: '1000100', 8: '1001000', 9: '1110100',
};
const PARITY = {
  0: 'LLLLLL', 1: 'LLGLGG', 2: 'LLGGLG', 3: 'LLGGGL', 4: 'LGLLGG',
  5: 'LGGLLG', 6: 'LGGGLL', 7: 'LGLGLG', 8: 'LGLGGL', 9: 'LGGLGL',
};

function bitsFor(code) {
  const digits = code.split('').map(Number);
  const parity = PARITY[digits[0]];
  let bits = '101'; // guarda inicial
  for (let i = 0; i < 6; i++) {
    const digit = digits[i + 1];
    bits += parity[i] === 'L' ? L_CODE[digit] : G_CODE[digit];
  }
  bits += '01010'; // guarda central
  for (let i = 0; i < 6; i++) {
    bits += R_CODE[digits[i + 7]];
  }
  bits += '101'; // guarda final
  return bits;
}

/** Devuelve un <svg> como string. `code` debe ser un EAN-13 de 13 dígitos. */
export function renderEan13Svg(code, { moduleWidth = 2.2, height = 70 } = {}) {
  if (!/^\d{13}$/.test(code)) {
    return `<span class="table-cell-muted">Código inválido</span>`;
  }
  const bits = bitsFor(code);
  const quietZone = 10 * moduleWidth;
  const barsWidth = bits.length * moduleWidth;
  const totalWidth = barsWidth + quietZone * 2;
  const textHeight = 16;

  let x = quietZone;
  let bars = '';
  for (const bit of bits) {
    if (bit === '1') {
      bars += `<rect x="${x.toFixed(2)}" y="0" width="${moduleWidth}" height="${height - textHeight}" fill="#000" />`;
    }
    x += moduleWidth;
  }

  const digits = code.split('');
  const leftGroup = digits.slice(1, 7).join('');
  const rightGroup = digits.slice(7, 13).join('');

  return `
    <svg viewBox="0 0 ${totalWidth.toFixed(2)} ${height}" width="${totalWidth.toFixed(0)}" height="${height}"
         role="img" aria-label="Código de barras ${code}" xmlns="http://www.w3.org/2000/svg">
      <rect x="0" y="0" width="${totalWidth.toFixed(2)}" height="${height}" fill="#fff" />
      ${bars}
      <text x="${(quietZone - 4).toFixed(2)}" y="${height - 2}" font-family="monospace" font-size="11" fill="#000" text-anchor="end">${digits[0]}</text>
      <text x="${(quietZone + barsWidth * 0.28).toFixed(2)}" y="${height - 2}" font-family="monospace" font-size="11" fill="#000" text-anchor="middle" letter-spacing="1">${leftGroup}</text>
      <text x="${(quietZone + barsWidth * 0.74).toFixed(2)}" y="${height - 2}" font-family="monospace" font-size="11" fill="#000" text-anchor="middle" letter-spacing="1">${rightGroup}</text>
    </svg>
  `;
}
