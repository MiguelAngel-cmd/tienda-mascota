import { api } from './api.js';
import { esqueleto, hidratarIconos, icon, toastError } from './ui.js';
import * as dashboard from './views/dashboard.js';
import * as productos from './views/products.js';
import * as clientes from './views/customers.js';
import * as inventario from './views/inventory.js';
import * as ventas from './views/sales.js';

const RUTAS = { dashboard, productos, clientes, inventario, ventas };
const vista = document.getElementById('view');
let renderActual = 0;

/** Router por hash: #/productos, #/ventas, ... */
async function navegar() {
  const ruta = location.hash.replace(/^#\/?/, '').split('?')[0] || 'dashboard';
  const modulo = RUTAS[ruta] || dashboard;
  document.querySelectorAll('#nav a').forEach((a) => a.classList.toggle('active', a.dataset.route === ruta));
  document.body.classList.remove('menu-abierto');

  const id = ++renderActual;
  vista.innerHTML = esqueleto();
  try {
    const html = document.createElement('div');
    html.className = 'view-enter';
    await modulo.render(html, { refrescar: navegar });
    if (id !== renderActual) return; // el usuario ya cambió de pantalla
    vista.replaceChildren(html);
    vista.focus({ preventScroll: true });
    window.scrollTo({ top: 0 });
  } catch (err) {
    if (id !== renderActual) return;
    console.error(err);
    toastError(err);
  }
  revisarServicios();
}

// ---------- Estado de los microservicios (pie del menú) ----------
const SERVICIOS = [
  { nombre: 'Productos', probar: api.products.list },
  { nombre: 'Clientes', probar: api.customers.list },
  { nombre: 'Inventario', probar: api.inventory.alerts },
  { nombre: 'Ventas', probar: api.sales.list },
];

async function revisarServicios() {
  const cont = document.getElementById('services');
  const res = await Promise.allSettled(SERVICIOS.map((s) => s.probar()));
  cont.innerHTML = `<p class="services-title">Microservicios</p>` + SERVICIOS.map((s, i) => {
    const ok = res[i].status === 'fulfilled';
    return `<span class="service ${ok ? 'up' : 'down'}" title="${ok ? 'En línea' : 'Sin conexión'}"><i></i>${s.nombre}</span>`;
  }).join('');
  const alertas = res[2].status === 'fulfilled' ? res[2].value.length : 0;
  const badge = document.getElementById('badge-alertas');
  badge.hidden = alertas === 0;
  badge.textContent = alertas;
}

// ---------- Tema claro / oscuro ----------
function temaActual() {
  const forzado = document.documentElement.dataset.theme;
  if (forzado) return forzado;
  return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}
function pintarBotonTema() {
  const oscuro = temaActual() === 'dark';
  document.getElementById('theme-toggle').innerHTML = `${icon(oscuro ? 'sun' : 'moon')}<span>${oscuro ? 'Modo claro' : 'Modo oscuro'}</span>`;
}
document.getElementById('theme-toggle').addEventListener('click', () => {
  const nuevo = temaActual() === 'dark' ? 'light' : 'dark';
  document.documentElement.dataset.theme = nuevo;
  try { localStorage.setItem('tema', nuevo); } catch { /* sin almacenamiento */ }
  pintarBotonTema();
});

// ---------- Menú en pantallas pequeñas ----------
document.getElementById('menu-btn').addEventListener('click', () => document.body.classList.toggle('menu-abierto'));
document.getElementById('sidebar-backdrop').addEventListener('click', () => document.body.classList.remove('menu-abierto'));

hidratarIconos();
pintarBotonTema();
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', pintarBotonTema);
window.addEventListener('hashchange', navegar);
window.addEventListener('tienda:cambio', revisarServicios); // las vistas avisan cuando cambian datos
setInterval(revisarServicios, 30000);
navegar();
