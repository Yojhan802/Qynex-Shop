// Departamento/provincia/distrito de todo el Perú — dataset público real
// (joseluisq/ubigeos-peru) embebido como JSON estático, no una llamada en
// vivo a un tercero: el checkout no debe depender de que ese sitio esté
// disponible en el momento de la compra.

let cache = null;

async function cargar() {
  if (cache) return cache;
  const response = await fetch('js/store/data/peru-ubigeo.json');
  cache = await response.json();
  return cache;
}

export async function getDepartamentos() {
  const data = await cargar();
  return data.departamentos;
}

export async function getProvincias(departamentoId) {
  const data = await cargar();
  return data.provincias[departamentoId] ?? [];
}

export async function getDistritos(provinciaId) {
  const data = await cargar();
  return data.distritos[provinciaId] ?? [];
}
