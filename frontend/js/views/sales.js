import { api, cargar } from '../api.js';
import {
  CATEGORIAS, avisoErrores, badgeVenta, cabecera, categoria, confirmar, debounce, dinero, esc, fecha, icon, iniciales,
  modal, nombreCliente, normalizar, toast, toastError, vacio,
} from '../ui.js';

// El carrito sobrevive a los refrescos de la pantalla (p. ej. si se registra un cliente en medio)
const carrito = new Map(); // productId -> cantidad
let clienteSeleccionado = '';

export async function render(el, { refrescar }) {
  const tab = new URLSearchParams(location.hash.split('?')[1]).get('tab') === 'historial' ? 'historial' : 'nueva';
  const d = await cargar(['productos', 'clientes', 'inventario', 'ventas']);
  const prodPorId = new Map(d.productos.map((p) => [p.id, p]));
  const cliPorId = new Map(d.clientes.map((c) => [c.id, c]));
  const invPorProducto = new Map(d.inventario.map((i) => [i.productId, i]));

  el.innerHTML = `
    ${cabecera({
      titulo: 'Ventas',
      subtitulo: 'Sales-Service valida el cliente, verifica el stock y descuenta el inventario automáticamente.',
    })}
    ${avisoErrores(d.errores)}
    <nav class="tabs" role="tablist">
      <a href="#/ventas" role="tab" class="${tab === 'nueva' ? 'active' : ''}">${icon('cart')}Nueva venta</a>
      <a href="#/ventas?tab=historial" role="tab" class="${tab === 'historial' ? 'active' : ''}">${icon('receipt')}Historial <span class="count">${d.ventas.length}</span></a>
    </nav>
    <div data-tab></div>`;

  const cont = el.querySelector('[data-tab]');
  if (tab === 'nueva') nuevaVenta(cont);
  else historial(cont);

  // ------------------------------------------------------------------ Nueva venta (punto de venta)
  function nuevaVenta(cont) {
    // Limpia del carrito lo que ya no se puede vender
    for (const [pid] of carrito) {
      const p = prodPorId.get(pid);
      if (!p || p.estado !== 'ACTIVO' || !invPorProducto.get(pid)?.stockActual) carrito.delete(pid);
    }
    if (clienteSeleccionado && !cliPorId.has(Number(clienteSeleccionado))) clienteSeleccionado = '';

    const filtro = { texto: '', categoria: '' };
    cont.innerHTML = `
      <div class="pos">
        <section class="pos-catalog">
          <div class="toolbar">
            <label class="search">${icon('search')}<input type="search" placeholder="Buscar producto o escanear código" data-buscar></label>
          </div>
          <div class="chips chips-sm">
            <button class="chip active" data-cat="">Todas</button>
            ${Object.entries(CATEGORIAS).map(([k, c]) => `<button class="chip" data-cat="${k}">${c.emoji} ${c.nombre}</button>`).join('')}
          </div>
          <div class="pos-grid" data-catalogo></div>
        </section>

        <aside class="card pos-cart">
          <header class="card-header"><h2>${icon('cart')} Venta actual</h2><button class="btn btn-ghost btn-sm" data-vaciar>Vaciar</button></header>
          <label class="field">
            <span class="field-label">Cliente <em>*</em></span>
            <select data-cliente>
              <option value="">Selecciona un cliente…</option>
              ${[...d.clientes].sort((a, b) => nombreCliente(a).localeCompare(nombreCliente(b))).map((c) =>
                `<option value="${c.id}" ${String(c.id) === String(clienteSeleccionado) ? 'selected' : ''}>${esc(nombreCliente(c))}${c.documento ? ` · ${esc(c.documento)}` : ''}</option>`).join('')}
            </select>
            ${d.clientes.length ? '' : '<small class="field-hint">No hay clientes. <a class="link" href="#/clientes">Registra uno primero</a>.</small>'}
          </label>
          <div class="cart-items" data-items></div>
          <div class="cart-total" data-total></div>
          <div class="form-alert" hidden data-alerta></div>
          <button class="btn btn-primary btn-lg btn-block" data-cobrar>${icon('check')}Registrar venta</button>
          <p class="cart-note">${icon('sparkles')}El precio se toma de Product-Service al registrar la venta.</p>
        </aside>
      </div>`;

    const catalogo = cont.querySelector('[data-catalogo]');
    const items = cont.querySelector('[data-items]');

    function pintarCatalogo() {
      const t = normalizar(filtro.texto);
      const vendibles = d.productos
        .filter((p) => p.estado === 'ACTIVO')
        .filter((p) => !filtro.categoria || p.categoria === filtro.categoria)
        .filter((p) => !t || normalizar(`${p.nombre} ${p.marca} ${p.codigoBarras}`).includes(t))
        .sort((a, b) => a.nombre.localeCompare(b.nombre));
      if (!vendibles.length) {
        catalogo.innerHTML = vacio({ icono: 'box', titulo: d.productos.length ? 'Sin resultados' : 'No hay productos', texto: d.productos.length ? 'Prueba con otra búsqueda.' : 'Crea productos para poder venderlos.' });
        return;
      }
      catalogo.innerHTML = vendibles.map((p) => {
        const c = categoria(p.categoria);
        const stock = invPorProducto.get(p.id)?.stockActual ?? 0;
        const enCarrito = carrito.get(p.id) || 0;
        const disponible = stock - enCarrito;
        const sinInv = !invPorProducto.has(p.id);
        return `<button class="pos-item ${enCarrito ? 'selected' : ''}" data-agregar="${p.id}" ${disponible <= 0 ? 'disabled' : ''}>
            <span class="avatar avatar-${c.color}">${c.emoji}</span>
            <span class="pos-item-name">${esc(p.nombre)}</span>
            <span class="pos-item-price">${dinero(p.precio)}</span>
            <span class="pos-item-stock ${stock === 0 ? 'text-danger' : ''}">${sinInv ? 'Sin inventario' : stock === 0 ? 'Agotado' : `${disponible} disponibles`}</span>
            ${enCarrito ? `<span class="pos-item-qty">${enCarrito}</span>` : ''}
          </button>`;
      }).join('');
      catalogo.querySelectorAll('[data-agregar]').forEach((b) => (b.onclick = () => cambiar(Number(b.dataset.agregar), +1)));
    }

    function pintarCarrito() {
      const lineas = [...carrito.entries()].map(([pid, cant]) => ({ p: prodPorId.get(pid), cant, stock: invPorProducto.get(pid)?.stockActual ?? 0 }));
      const total = lineas.reduce((s, l) => s + Number(l.p.precio) * l.cant, 0);
      const unidades = lineas.reduce((s, l) => s + l.cant, 0);
      items.innerHTML = lineas.length ? lineas.map(({ p, cant, stock }) => `
        <div class="cart-line">
          <span class="avatar avatar-sm avatar-${categoria(p.categoria).color}">${categoria(p.categoria).emoji}</span>
          <div class="grow"><strong>${esc(p.nombre)}</strong><small class="block muted">${dinero(p.precio)} c/u</small></div>
          <div class="qty">
            <button class="btn btn-icon btn-ghost btn-xs" data-menos="${p.id}" aria-label="Quitar uno">${icon('minus')}</button>
            <span>${cant}</span>
            <button class="btn btn-icon btn-ghost btn-xs" data-mas="${p.id}" ${cant >= stock ? 'disabled' : ''} aria-label="Agregar uno">${icon('plus')}</button>
          </div>
          <strong class="line-total">${dinero(Number(p.precio) * cant)}</strong>
        </div>`).join('')
        : `<div class="cart-empty">${icon('cart')}<p>Toca un producto del catálogo para agregarlo.</p></div>`;
      cont.querySelector('[data-total]').innerHTML = `
        <div><span>Productos</span><span>${unidades} ${unidades === 1 ? 'unidad' : 'unidades'}</span></div>
        <div class="grand"><span>Total</span><strong>${dinero(total)}</strong></div>`;
      cont.querySelector('[data-cobrar]').disabled = !lineas.length;
      items.querySelectorAll('[data-menos]').forEach((b) => (b.onclick = () => cambiar(Number(b.dataset.menos), -1)));
      items.querySelectorAll('[data-mas]').forEach((b) => (b.onclick = () => cambiar(Number(b.dataset.mas), +1)));
    }

    function cambiar(pid, delta) {
      const stock = invPorProducto.get(pid)?.stockActual ?? 0;
      const nueva = Math.min((carrito.get(pid) || 0) + delta, stock);
      if (nueva <= 0) carrito.delete(pid); else carrito.set(pid, nueva);
      cont.querySelector('[data-alerta]').hidden = true;
      pintarCatalogo();
      pintarCarrito();
    }

    cont.querySelector('[data-buscar]').addEventListener('input', debounce((e) => { filtro.texto = e.target.value; pintarCatalogo(); }));
    // Lector de código de barras: Enter con un código exacto agrega el producto
    cont.querySelector('[data-buscar]').addEventListener('keydown', (e) => {
      if (e.key !== 'Enter') return;
      const p = d.productos.find((x) => x.codigoBarras === e.target.value.trim() && x.estado === 'ACTIVO');
      if (p) { cambiar(p.id, +1); e.target.value = ''; filtro.texto = ''; pintarCatalogo(); }
    });
    cont.querySelectorAll('[data-cat]').forEach((b) => b.addEventListener('click', () => {
      cont.querySelectorAll('[data-cat]').forEach((x) => x.classList.toggle('active', x === b));
      filtro.categoria = b.dataset.cat;
      pintarCatalogo();
    }));
    cont.querySelector('[data-cliente]').addEventListener('change', (e) => { clienteSeleccionado = e.target.value; });
    cont.querySelector('[data-vaciar]').onclick = () => { carrito.clear(); pintarCatalogo(); pintarCarrito(); };

    cont.querySelector('[data-cobrar]').onclick = async (e) => {
      const alerta = cont.querySelector('[data-alerta]');
      alerta.hidden = true;
      if (!clienteSeleccionado) {
        alerta.innerHTML = `${icon('alert')}<div><strong>Selecciona el cliente de la venta.</strong></div>`;
        alerta.hidden = false;
        return;
      }
      const boton = e.currentTarget;
      boton.disabled = true;
      boton.classList.add('cargando');
      try {
        const venta = await api.sales.create({
          customerId: Number(clienteSeleccionado),
          items: [...carrito.entries()].map(([productId, cantidad]) => ({ productId, cantidad })),
        });
        carrito.clear();
        toast(`Venta #${venta.id} registrada por ${dinero(venta.total)}.`);
        window.dispatchEvent(new Event('tienda:cambio'));
        await refrescar();
        verDetalle(venta, { recienCreada: true });
      } catch (err) {
        alerta.innerHTML = `${icon('alert')}<div><strong>No se pudo registrar la venta</strong><br>${esc(err.message)}${(err.details || []).map((x) => `<br>${esc(x)}`).join('')}</div>`;
        alerta.hidden = false;
        boton.disabled = false;
        boton.classList.remove('cargando');
      }
    };

    pintarCatalogo();
    pintarCarrito();
  }

  // ------------------------------------------------------------------ Historial
  function historial(cont) {
    const filtro = { estado: '', texto: '' };
    const total = (estado) => d.ventas.filter((v) => !estado || v.estado === estado).length;
    cont.innerHTML = `
      <div class="toolbar">
        <label class="search">${icon('search')}<input type="search" placeholder="Buscar por número de venta o cliente" data-buscar></label>
        <div class="chips chips-inline">
          <button class="chip active" data-estado="">Todas <span>${total()}</span></button>
          <button class="chip" data-estado="FACTURADA">Facturadas <span>${total('FACTURADA')}</span></button>
          <button class="chip" data-estado="CANCELADA">Canceladas <span>${total('CANCELADA')}</span></button>
          ${total('PENDIENTE') ? `<button class="chip" data-estado="PENDIENTE">Pendientes <span>${total('PENDIENTE')}</span></button>` : ''}
        </div>
      </div>
      <div class="card card-flush" data-lista></div>`;
    const lista = cont.querySelector('[data-lista]');

    function pintar() {
      const t = normalizar(filtro.texto).replace('#', '');
      const visibles = d.ventas
        .filter((v) => !filtro.estado || v.estado === filtro.estado)
        .filter((v) => !t || String(v.id) === t || normalizar(nombreCliente(cliPorId.get(v.customerId))).includes(t));
      if (!d.ventas.length) {
        lista.innerHTML = vacio({ icono: 'receipt', titulo: 'Aún no hay ventas', texto: 'Registra la primera desde la pestaña "Nueva venta".', accion: `<a class="btn btn-primary" href="#/ventas">${icon('cart')}Nueva venta</a>` });
        return;
      }
      if (!visibles.length) { lista.innerHTML = vacio({ icono: 'search', titulo: 'Sin resultados' }); return; }
      lista.innerHTML = `<div class="table-wrap"><table class="table table-hover">
        <thead><tr><th>Venta</th><th>Fecha</th><th>Cliente</th><th>Detalle</th><th class="num">Total</th><th>Estado</th><th></th></tr></thead>
        <tbody>${visibles.map((v) => {
          const c = cliPorId.get(v.customerId);
          const nombres = v.detalles.map((x) => prodPorId.get(x.productId)?.nombre || `#${x.productId}`);
          return `<tr data-ver="${v.id}" tabindex="0">
            <td><strong>#${v.id}</strong></td>
            <td>${fecha(v.fechaVenta)}</td>
            <td><span class="person"><span class="avatar avatar-sm">${esc(iniciales(c))}</span>${esc(nombreCliente(c))}</span></td>
            <td class="truncate" title="${esc(nombres.join(', '))}">${esc(nombres.slice(0, 2).join(', '))}${nombres.length > 2 ? ` y ${nombres.length - 2} más` : ''}</td>
            <td class="num"><strong>${dinero(v.total)}</strong></td>
            <td>${badgeVenta(v.estado)}</td>
            <td class="row-actions"><button class="btn btn-icon btn-ghost btn-sm" aria-label="Ver detalle">${icon('eye')}</button></td>
          </tr>`;
        }).join('')}</tbody></table></div>`;
      lista.querySelectorAll('[data-ver]').forEach((tr) => {
        const abrir = () => verDetalle(d.ventas.find((v) => v.id === Number(tr.dataset.ver)));
        tr.onclick = abrir;
        tr.onkeydown = (e) => { if (e.key === 'Enter') abrir(); };
      });
    }
    cont.querySelector('[data-buscar]').addEventListener('input', debounce((e) => { filtro.texto = e.target.value; pintar(); }));
    cont.querySelectorAll('[data-estado]').forEach((b) => b.addEventListener('click', () => {
      cont.querySelectorAll('[data-estado]').forEach((x) => x.classList.toggle('active', x === b));
      filtro.estado = b.dataset.estado;
      pintar();
    }));
    pintar();
  }

  // ------------------------------------------------------------------ Detalle / recibo
  function verDetalle(v, { recienCreada = false } = {}) {
    const c = cliPorId.get(v.customerId);
    modal({
      titulo: recienCreada ? '¡Venta registrada!' : `Venta #${v.id}`,
      subtitulo: recienCreada ? `Venta #${v.id} · el inventario ya fue actualizado` : fecha(v.fechaVenta),
      contenido: `
        <div class="receipt">
          <div class="receipt-head">
            <span class="brand-logo">${icon('paw')}</span>
            <div><strong>Huellitas</strong><small class="block muted">Tienda de mascotas</small></div>
            <span class="grow"></span>${badgeVenta(v.estado)}
          </div>
          <dl class="receipt-meta">
            <div><dt>Cliente</dt><dd>${esc(nombreCliente(c))}${c?.documento ? `<small class="block muted">Doc. ${esc(c.documento)}</small>` : ''}</dd></div>
            <div><dt>Fecha</dt><dd>${fecha(v.fechaVenta)}</dd></div>
          </dl>
          <table class="table receipt-table">
            <thead><tr><th>Producto</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Subtotal</th></tr></thead>
            <tbody>${v.detalles.map((x) => `<tr>
              <td>${esc(prodPorId.get(x.productId)?.nombre || `Producto #${x.productId}`)}</td>
              <td class="num">${x.cantidad}</td><td class="num">${dinero(x.precioUnitario)}</td><td class="num">${dinero(x.subtotal)}</td></tr>`).join('')}</tbody>
          </table>
          <div class="receipt-total"><span>Total</span><strong>${dinero(v.total)}</strong></div>
        </div>
        <footer class="modal-footer">
          ${v.estado !== 'CANCELADA' ? `<button class="btn btn-danger-ghost" type="button" data-cancelar>${icon('ban')}Cancelar venta</button>` : ''}
          <span class="grow"></span>
          ${recienCreada ? `<a class="btn btn-ghost" href="#/ventas?tab=historial" data-cerrar>Ver historial</a>` : ''}
          <button class="btn btn-primary" type="button" data-cerrar>${recienCreada ? 'Nueva venta' : 'Cerrar'}</button>
        </footer>`,
      alMontar: (raiz, cerrar) => {
        raiz.querySelectorAll('[data-cerrar]').forEach((b) => b.addEventListener('click', cerrar));
        raiz.querySelector('[data-cancelar]')?.addEventListener('click', async () => {
          const ok = await confirmar({
            titulo: `Cancelar venta #${v.id}`,
            mensaje: v.estado === 'FACTURADA'
              ? 'Las unidades vendidas se devolverán al inventario (movimiento de ENTRADA). Esta acción no se puede deshacer.'
              : 'La venta quedará como cancelada.',
            textoConfirmar: 'Sí, cancelar venta',
            peligro: true,
          });
          if (!ok) return;
          try {
            await api.sales.cancel(v.id);
            toast(`Venta #${v.id} cancelada${v.estado === 'FACTURADA' ? ' y stock devuelto al inventario' : ''}.`);
            window.dispatchEvent(new Event('tienda:cambio'));
            cerrar();
            refrescar();
          } catch (err) {
            toastError(err);
          }
        });
      },
    });
  }
}
