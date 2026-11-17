import { cargar } from '../api.js';
import { cargarDemo } from '../demo.js';
import {
  avisoErrores, badgeVenta, cabecera, categoria, dinero, esc, haceCuanto, icon, iniciales, nombreCliente,
  numero, toast, toastError, vacio,
} from '../ui.js';
import { abrirMovimiento } from './inventory.js';

export async function render(el, { refrescar }) {
  const d = await cargar(['productos', 'clientes', 'inventario', 'ventas']);
  const prodPorId = new Map(d.productos.map((p) => [p.id, p]));
  const cliPorId = new Map(d.clientes.map((c) => [c.id, c]));
  const facturadas = d.ventas.filter((v) => v.estado === 'FACTURADA');

  const hoy = new Date().toDateString();
  const ingresos = facturadas.reduce((s, v) => s + Number(v.total), 0);
  const ingresosHoy = facturadas.filter((v) => new Date(v.fechaVenta).toDateString() === hoy)
    .reduce((s, v) => s + Number(v.total), 0);
  const ventasHoy = facturadas.filter((v) => new Date(v.fechaVenta).toDateString() === hoy).length;
  const activos = d.productos.filter((p) => p.estado === 'ACTIVO').length;
  const mes = new Date(); mes.setDate(1); mes.setHours(0, 0, 0, 0);
  const clientesMes = d.clientes.filter((c) => new Date(c.fechaRegistro) >= mes).length;
  const alertas = d.inventario.filter((i) => i.alertaStockBajo).sort((a, b) => a.stockActual - b.stockActual);
  const ticket = facturadas.length ? ingresos / facturadas.length : 0;

  const saludo = (() => {
    const h = new Date().getHours();
    return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
  })();
  const hoyTexto = new Date().toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long' });

  el.innerHTML = `
    ${cabecera({
      titulo: `${saludo} 🐾`,
      subtitulo: `Así va la tienda este ${esc(hoyTexto)}.`,
      acciones: `
        <button class="btn btn-ghost" data-refrescar>${icon('refresh')}Actualizar</button>
        <a class="btn btn-primary" href="#/ventas">${icon('cart')}Nueva venta</a>`,
    })}
    ${avisoErrores(d.errores)}
    ${d.productos.length === 0 && !d.errores.length ? bienvenida() : ''}

    <section class="kpis">
      ${kpi('money', 'Ingresos facturados', dinero(ingresos), `${dinero(ingresosHoy)} hoy`, 'teal')}
      ${kpi('receipt', 'Ventas facturadas', numero(facturadas.length), `${ventasHoy} hoy · ticket prom. ${dinero(ticket)}`, 'orange')}
      ${kpi('box', 'Productos activos', numero(activos), `${d.productos.length} en el catálogo`, 'violet')}
      ${kpi('users', 'Clientes', numero(d.clientes.length), `${clientesMes} nuevos este mes`, 'sky')}
    </section>

    <section class="grid-dash">
      <article class="card span-2">
        <header class="card-header"><h2>Ventas de los últimos 7 días</h2><span class="muted">Solo ventas facturadas</span></header>
        ${graficoSemana(facturadas)}
      </article>

      <article class="card">
        <header class="card-header">
          <h2>${icon('alert', 'text-warn')} Alertas de stock</h2>
          <a class="link" href="#/inventario">Ver inventario</a>
        </header>
        ${alertas.length ? `<ul class="alert-list">${alertas.slice(0, 6).map((i) => {
          const p = prodPorId.get(i.productId);
          return `<li>
            <span class="avatar avatar-${categoria(p?.categoria).color}">${categoria(p?.categoria).emoji}</span>
            <div class="grow"><strong>${esc(p?.nombre || `Producto #${i.productId}`)}</strong>
              <small>${i.stockActual === 0 ? '<b class="text-danger">Agotado</b>' : `Quedan <b>${i.stockActual}</b>`} · mínimo ${i.stockMinimo}</small></div>
            <button class="btn btn-sm btn-soft" data-reabastecer="${i.productId}">${icon('plus')}Reabastecer</button>
          </li>`;
        }).join('')}</ul>` : vacio({ icono: 'check', titulo: 'Todo en orden', texto: 'Ningún producto está por debajo de su stock mínimo.' })}
      </article>

      <article class="card span-2">
        <header class="card-header"><h2>Últimas ventas</h2><a class="link" href="#/ventas?tab=historial">Ver historial</a></header>
        ${d.ventas.length ? `<div class="table-wrap"><table class="table">
          <thead><tr><th>Venta</th><th>Cliente</th><th>Productos</th><th class="num">Total</th><th>Estado</th></tr></thead>
          <tbody>${d.ventas.slice(0, 6).map((v) => {
            const c = cliPorId.get(v.customerId);
            const unidades = v.detalles.reduce((s, x) => s + x.cantidad, 0);
            return `<tr>
              <td><strong>#${v.id}</strong><small class="block muted">${haceCuanto(v.fechaVenta)}</small></td>
              <td><span class="person"><span class="avatar avatar-sm">${esc(iniciales(c))}</span>${esc(nombreCliente(c))}</span></td>
              <td>${unidades} ${unidades === 1 ? 'unidad' : 'unidades'}</td>
              <td class="num"><strong>${dinero(v.total)}</strong></td>
              <td>${badgeVenta(v.estado)}</td>
            </tr>`;
          }).join('')}</tbody></table></div>`
          : vacio({ icono: 'receipt', titulo: 'Aún no hay ventas', texto: 'Las ventas que registres aparecerán aquí.' })}
      </article>

      <article class="card">
        <header class="card-header"><h2>${icon('trend')} Más vendidos</h2></header>
        ${masVendidos(facturadas, prodPorId)}
      </article>
    </section>`;

  el.querySelector('[data-refrescar]').onclick = refrescar;
  el.querySelectorAll('[data-reabastecer]').forEach((b) => {
    b.onclick = () => {
      const pid = Number(b.dataset.reabastecer);
      abrirMovimiento({
        inventario: d.inventario.find((i) => i.productId === pid),
        producto: prodPorId.get(pid),
        tipo: 'ENTRADA',
        alTerminar: refrescar,
      });
    };
  });
  el.querySelector('[data-demo]')?.addEventListener('click', async (e) => {
    const boton = e.currentTarget;
    boton.disabled = true;
    boton.classList.add('cargando');
    try {
      const r = await cargarDemo();
      toast(`Datos de demostración cargados: ${r.productos} productos, ${r.clientes} clientes y ${r.ventas} ventas.`);
      window.dispatchEvent(new Event('tienda:cambio'));
      refrescar();
    } catch (err) {
      toastError(err);
      boton.disabled = false;
      boton.classList.remove('cargando');
    }
  });
}

function kpi(ico, titulo, valor, sub, color) {
  return `<article class="kpi kpi-${color}">
      <span class="kpi-icon">${icon(ico)}</span>
      <div><p class="kpi-label">${titulo}</p><p class="kpi-value">${valor}</p><p class="kpi-sub">${sub}</p></div>
    </article>`;
}

function bienvenida() {
  return `<section class="welcome">
      <div class="welcome-art" aria-hidden="true">🐶🐱🐰</div>
      <div class="grow">
        <h2>¡Bienvenido a Huellitas!</h2>
        <p>La tienda está vacía. Puedes crear productos y clientes desde el menú, o cargar un conjunto de datos de
          demostración (productos, clientes, existencias y algunas ventas) para probar todo el flujo.</p>
      </div>
      <button class="btn btn-primary btn-lg" data-demo>${icon('sparkles')}Cargar datos de demostración</button>
    </section>`;
}

function graficoSemana(facturadas) {
  const dias = [];
  for (let i = 6; i >= 0; i--) {
    const f = new Date(); f.setHours(0, 0, 0, 0); f.setDate(f.getDate() - i);
    dias.push({ fecha: f, total: 0, cantidad: 0 });
  }
  facturadas.forEach((v) => {
    const f = new Date(v.fechaVenta); f.setHours(0, 0, 0, 0);
    const dia = dias.find((x) => x.fecha.getTime() === f.getTime());
    if (dia) { dia.total += Number(v.total); dia.cantidad++; }
  });
  const max = Math.max(...dias.map((x) => x.total), 1);
  const semana = dias.reduce((s, x) => s + x.total, 0);
  return `<div class="chart-summary"><span class="kpi-value">${dinero(semana)}</span><span class="muted">en los últimos 7 días</span></div>
    <div class="bars" role="img" aria-label="Ventas por día de los últimos 7 días">
      ${dias.map((x, i) => `<div class="bar-col ${i === 6 ? 'today' : ''}" title="${x.cantidad} ventas · ${dinero(x.total)}">
          <span class="bar-value">${x.total ? dinero(x.total).replace(/\s/g, ' ') : ''}</span>
          <div class="bar"><div class="bar-fill" style="height:${Math.max((x.total / max) * 100, x.total ? 4 : 0)}%"></div></div>
          <span class="bar-label">${i === 6 ? 'Hoy' : x.fecha.toLocaleDateString('es-CO', { weekday: 'short' })}</span>
        </div>`).join('')}
    </div>`;
}

function masVendidos(facturadas, prodPorId) {
  const conteo = new Map();
  facturadas.forEach((v) => v.detalles.forEach((d) => {
    const x = conteo.get(d.productId) || { unidades: 0, total: 0 };
    x.unidades += d.cantidad; x.total += Number(d.subtotal);
    conteo.set(d.productId, x);
  }));
  const top = [...conteo.entries()].sort((a, b) => b[1].unidades - a[1].unidades).slice(0, 5);
  if (!top.length) return vacio({ icono: 'trend', titulo: 'Sin datos todavía', texto: 'Aquí verás los productos que más se venden.' });
  const max = top[0][1].unidades;
  return `<ul class="rank">${top.map(([pid, x], i) => {
    const p = prodPorId.get(pid);
    return `<li>
      <span class="rank-pos">${i + 1}</span>
      <div class="grow">
        <div class="rank-top"><strong>${esc(p?.nombre || `Producto #${pid}`)}</strong><span>${x.unidades} u.</span></div>
        <div class="meter"><div style="width:${(x.unidades / max) * 100}%"></div></div>
        <small class="muted">${dinero(x.total)}</small>
      </div>
    </li>`;
  }).join('')}</ul>`;
}
