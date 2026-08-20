/** Convierte un valor a una celda CSV válida, entrecomillando si hace falta. */
function celda(valor) {
  const texto = valor == null ? '' : String(valor);
  return /[",\n]/.test(texto) ? `"${texto.replace(/"/g, '""')}"` : texto;
}

/** `filas` es un array de arrays; cada sub-array es una fila (usar [] para dejar una línea en blanco). */
export function descargarCsv(nombreArchivo, filas) {
  const contenido = filas.map((fila) => fila.map(celda).join(',')).join('\r\n');
  const blob = new Blob([`﻿${contenido}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}
