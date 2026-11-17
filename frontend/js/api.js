import { API } from './config.js';

/** Error que devuelven las APIs (formato ApiError del backend: status, message, details). */
export class ApiError extends Error {
  constructor(status, message, details = []) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

const NOMBRES = {
  [API.products]: 'Product-Service',
  [API.customers]: 'Customer-Service',
  [API.inventory]: 'Inventory-Service',
  [API.sales]: 'Sales-Service',
};

function servicioDe(url) {
  const base = Object.keys(NOMBRES).find((b) => url.startsWith(b));
  return base ? NOMBRES[base] : 'el servidor';
}

async function request(method, url, body) {
  let res;
  try {
    res = await fetch(url, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, `No se pudo conectar con ${servicioDe(url)}. ¿Está en ejecución?`);
  }
  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) {
    if (!data?.message && res.status >= 502) {
      throw new ApiError(res.status, `${servicioDe(url)} no está disponible en este momento (HTTP ${res.status}).`);
    }
    throw new ApiError(res.status, data?.message || `Error HTTP ${res.status}`, data?.details || []);
  }
  return data;
}

const crud = (base) => ({
  list: () => request('GET', base),
  get: (id) => request('GET', `${base}/${id}`),
  create: (body) => request('POST', base, body),
  update: (id, body) => request('PUT', `${base}/${id}`, body),
  remove: (id) => request('DELETE', `${base}/${id}`),
});

export const api = {
  products: crud(API.products),
  customers: crud(API.customers),
  inventory: {
    list: () => request('GET', API.inventory),
    alerts: () => request('GET', `${API.inventory}/alerts`),
    get: (productId) => request('GET', `${API.inventory}/${productId}`),
    create: (body) => request('POST', API.inventory, body),
    update: (id, body) => request('PUT', `${API.inventory}/${id}`, body),
    move: (productId, tipoMovimiento, cantidad) =>
      request('POST', `${API.inventory}/${productId}/movements`, { tipoMovimiento, cantidad }),
    movements: (productId) => request('GET', `${API.inventory}/${productId}/movements`),
  },
  sales: {
    list: () => request('GET', API.sales),
    get: (id) => request('GET', `${API.sales}/${id}`),
    create: (body) => request('POST', API.sales, body),
    cancel: (id) => request('PUT', `${API.sales}/${id}`, { estado: 'CANCELADA' }),
  },
};

/**
 * Carga varias colecciones en paralelo. Si un servicio falla no se cae toda la pantalla:
 * su colección queda vacía y el error se reporta en `errores`.
 */
export async function cargar(nombres) {
  const fuentes = {
    productos: api.products.list,
    clientes: api.customers.list,
    inventario: api.inventory.list,
    ventas: api.sales.list,
  };
  const resultados = await Promise.allSettled(nombres.map((n) => fuentes[n]()));
  const datos = { errores: [] };
  resultados.forEach((r, i) => {
    datos[nombres[i]] = r.status === 'fulfilled' ? r.value : [];
    if (r.status === 'rejected') datos.errores.push(r.reason);
  });
  return datos;
}
