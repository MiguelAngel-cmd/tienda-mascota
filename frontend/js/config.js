/*
 * URLs de las APIs.
 * - Con Docker Compose el frontend se sirve desde nginx (http://localhost:8080) y nginx reenvía
 *   /api/... a cada microservicio, así que se usan rutas relativas.
 * - Sin Docker (perfil H2) el frontend se sirve con cualquier servidor estático, por ejemplo
 *   `python3 -m http.server 5500` dentro de /frontend, y se llama directo a cada puerto (CORS habilitado).
 */
const params = new URLSearchParams(location.search);
const PUERTOS_DESARROLLO = ['5500', '5173', '3000', '8000'];
const directo = params.has('directo') || PUERTOS_DESARROLLO.includes(location.port);
const host = location.hostname || 'localhost';

export const API = directo
  ? {
      products: `http://${host}:8081/products`,
      customers: `http://${host}:8082/customers`,
      inventory: `http://${host}:8083/inventory`,
      sales: `http://${host}:8084/sales`,
    }
  : {
      products: '/api/products',
      customers: '/api/customers',
      inventory: '/api/inventory',
      sales: '/api/sales',
    };

export const MODO = directo ? 'directo' : 'proxy';
