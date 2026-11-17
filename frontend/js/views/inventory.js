import { api, cargar } from '../api.js';
import {
  avisoErrores, cabecera, categoria, debounce, esc, estadoStock, fecha, formulario, haceCuanto, icon, modal,
  normalizar, numero, toast, toastError, vacio,
} from '../ui.js';

const TIPOS = {
  ENTRADA: { texto: 'Entrada', icono: 'arrowDown', ayuda: 'Suma unidades al stock (compra a proveedor, devolución).', clase: 'ok' },
  SALIDA: { texto: 'Salida', icono: 'arrowUp', ayuda: 'Resta unidades del stock (daño, vencimiento, uso interno).', clase: 'danger' },
  AJUSTE: { texto: 'Ajuste', icono: 'sliders', ayuda: 'Deja el stock exactamente en la cantidad indicada (conteo físico).', clase: 'info' },
};

export async function render(el, { refrescar }) {
  const d = await cargar(['productos', 'inventario']);
  const prodPorId = new Map(d.productos.map((p) => [p.id, p]));
  const sinInventario = d.productos.filter((p) => !d.inventario.some((i) => i.productId === p.id));
  const filtro = { texto: '', soloAlertas: new URLSearchParams(location.hash.split('?')[1]).has('alertas') };

  const unidades = d.inventario.reduce((s, i) => s + i.stockActual, 0);
  const alertas = d.inventario.filter((i) => i.alertaStockBajo).length;
  const agotados = d.inventario.filter((i) => i.stockActual === 0).length;

  el.innerHTML = `
    ${cabecera({
      titulo: 'Inventario',
      subtitulo: 'Existencias y movimientos de cada producto',
      acciones: `<button class="btn btn-primary" data-registrar ${sinInventario.length ? '' : 'disabled'}>${icon('plus')}Registrar inventario</button>`,
    })}
    ${avisoErrores(d.errores)}
    <section class="kpis kpis-sm">
      <article class="kpi kpi-teal"><span class="kpi-icon">${icon('layers')}</span><div><p class="kpi-label">Unidades en bodega</p><p class="kpi-value">${numero(unidades)}</p></div></article>
      <article class="kpi kpi-violet"><span class="kpi-icon">${icon('box')}</span><div><p class="kpi-label">Productos con inventario</p><p class="kpi-value">${d.inventario.length}</p></div></article>
      <article class="kpi kpi-orange"><span class="kpi-icon">${icon('alert')}</span><div><p class="kpi-label">Bajo el mínimo</p><p class="kpi-value">${alertas}</p></div></article>
      <article class="kpi kpi-rose"><span class="kpi-icon">${icon('ban')}</span><div><p class="kpi-label">Agotados</p><p class="kpi-value">${agotados}</p></div></article>
    </section>
    ${sinInventario.length ? `<div class="banner banner-info">${icon('sparkles')}<div>
      <strong>${sinInventario.length} ${sinInventario.length === 1 ? 'producto no tiene' : 'productos no tienen'} inventario registrado:</strong>
      ${sinInventario.slice(0, 4).map((p) => esc(p.nombre)).join(', ')}${sinInventario.length > 4 ? '…' : ''}.
      No se podrán vender hasta registrarlo.</div></div>` : ''}
    <div class="toolbar">
      <label class="search">${icon('search')}<input type="search" placeholder="Buscar producto" data-buscar></label>
      <label class="switch"><input type="checkbox" data-alertas ${filtro.soloAlertas ? 'checked' : ''}><span></span>Solo alertas</label>
    </div>
    <div class="card card-flush" data-lista></div>`;

  const lista = el.querySelector('[data-lista]');

  function pintar() {
    const t = normalizar(filtro.texto);
    const filas = d.inventario
      .map((i) => ({ i, p: prodPorId.get(i.productId) }))
      .filter(({ i }) => !filtro.soloAlertas || i.alertaStockBajo)
      .filter(({ i, p }) => !t || normalizar(`${p?.nombre} ${p?.marca} ${p?.codigoBarras} ${i.productId}`).includes(t))
      .sort((a, b) => Number(b.i.alertaStockBajo) - Number(a.i.alertaStockBajo) || (a.p?.nombre || '').localeCompare(b.p?.nombre || ''));

    if (!d.inventario.length) {
      lista.innerHTML = vacio({ icono: 'layers', titulo: 'Sin inventario registrado', texto: 'Registra las existencias de tus productos para empezar a vender.' });
      return;
    }
    if (!filas.length) {
      lista.innerHTML = vacio({ icono: 'check', titulo: filtro.soloAlertas ? 'No hay alertas' : 'Sin resultados', texto: filtro.soloAlertas ? 'Todos los productos están sobre su stock mínimo.' : 'Prueba con otra búsqueda.' });
      return;
    }
    lista.innerHTML = `<div class="table-wrap"><table class="table">
      <thead><tr><th>Producto</th><th>Existencias</th><th class="num">Mínimo</th><th>Estado</th><th>Actualizado</th><th class="right">Movimientos</th></tr></thead>
      <tbody>${filas.map(({ i, p }) => {
        const c = categoria(p?.categoria);
        const s = estadoStock(i);
        const tope = Math.max(i.stockMinimo * 2, i.stockActual, 1);
        return `<tr class="${i.alertaStockBajo ? 'row-alert' : ''}">
          <td><span class="person"><span class="avatar avatar-${c.color}">${c.emoji}</span><span>
            <strong>${esc(p?.nombre || `Producto #${i.productId}`)}</strong>
            <small class="block muted">${p ? `${c.nombre} · ${esc(p.codigoBarras)}` : 'No existe en Product-Service'}</small></span></span></td>
          <td class="stock-cell">
            <div class="stock-num"><strong>${numero(i.stockActual)}</strong> u.</div>
            <div class="stock-bar ${s.clase}"><div style="width:${Math.min((i.stockActual / tope) * 100, 100)}%"></div><i style="left:${(i.stockMinimo / tope) * 100}%" title="Mínimo"></i></div>
          </td>
          <td class="num">${i.stockMinimo}</td>
          <td><span class="badge badge-${s.clase}">${s.texto}</span></td>
          <td><span title="${fecha(i.ultimaActualizacion)}">${haceCuanto(i.ultimaActualizacion)}</span></td>
          <td class="row-actions">
            <button class="btn btn-sm btn-soft ok" data-mov="ENTRADA" data-pid="${i.productId}" title="Entrada">${icon('arrowDown')}<span class="hide-sm">Entrada</span></button>
            <button class="btn btn-sm btn-soft danger" data-mov="SALIDA" data-pid="${i.productId}" title="Salida">${icon('arrowUp')}<span class="hide-sm">Salida</span></button>
            <button class="btn btn-icon btn-ghost btn-sm" data-editar="${i.productId}" title="Editar stock y mínimo (PUT)" aria-label="Editar">${icon('sliders')}</button>
            <button class="btn btn-icon btn-ghost btn-sm" data-historial="${i.productId}" title="Historial de movimientos" aria-label="Historial">${icon('clock')}</button>
          </td>
        </tr>`;
      }).join('')}</tbody></table></div>`;

    const inv = (pid) => d.inventario.find((x) => x.productId === Number(pid));
    lista.querySelectorAll('[data-mov]').forEach((b) => (b.onclick = () => abrirMovimiento({
      inventario: inv(b.dataset.pid), producto: prodPorId.get(Number(b.dataset.pid)), tipo: b.dataset.mov, alTerminar: refrescar,
    })));
    lista.querySelectorAll('[data-editar]').forEach((b) => (b.onclick = () => editar(inv(b.dataset.editar))));
    lista.querySelectorAll('[data-historial]').forEach((b) => (b.onclick = () => historial(inv(b.dataset.historial), prodPorId.get(Number(b.dataset.historial)))));
  }

  el.querySelector('[data-buscar]').addEventListener('input', debounce((e) => { filtro.texto = e.target.value; pintar(); }));
  el.querySelector('[data-alertas]').addEventListener('change', (e) => { filtro.soloAlertas = e.target.checked; pintar(); });
  el.querySelector('[data-registrar]').onclick = () => registrar(sinInventario);
  pintar();

  function registrar(productos) {
    formulario({
      titulo: 'Registrar inventario',
      subtitulo: 'Inventory-Service valida el producto contra Product-Service.',
      textoEnviar: 'Registrar',
      campos: [
        { nombre: 'productId', etiqueta: 'Producto', tipo: 'select', requerido: true, completo: true,
          opciones: productos.map((p) => ({ valor: p.id, texto: `${categoria(p.categoria).emoji} ${p.nombre}` })) },
        { nombre: 'stockActual', etiqueta: 'Stock inicial', tipo: 'number', min: 0, requerido: true, valor: 0 },
        { nombre: 'stockMinimo', etiqueta: 'Stock mínimo', tipo: 'number', min: 0, requerido: true, valor: 5 },
      ],
      alEnviar: async (v) => {
        await api.inventory.create({ ...v, productId: Number(v.productId) });
        toast('Inventario registrado.');
        window.dispatchEvent(new Event('tienda:cambio'));
        refrescar();
      },
    });
  }

  function editar(i) {
    const p = prodPorId.get(i.productId);
    formulario({
      titulo: 'Editar existencias',
      subtitulo: `${esc(p?.nombre || `Producto #${i.productId}`)} · queda registrado como AJUSTE`,
      textoEnviar: 'Guardar',
      campos: [
        { nombre: 'stockActual', etiqueta: 'Stock actual', tipo: 'number', min: 0, requerido: true, valor: i.stockActual, autofocus: true },
        { nombre: 'stockMinimo', etiqueta: 'Stock mínimo', tipo: 'number', min: 0, requerido: true, valor: i.stockMinimo },
      ],
      alEnviar: async (v) => {
        await api.inventory.update(i.id, v);
        toast('Existencias actualizadas.');
        window.dispatchEvent(new Event('tienda:cambio'));
        refrescar();
      },
    });
  }
}

/** Modal para registrar ENTRADA / SALIDA / AJUSTE. Lo usan Inventario y el Resumen. */
export function abrirMovimiento({ inventario, producto, tipo = 'ENTRADA', alTerminar }) {
  if (!inventario) { toast('Este producto no tiene inventario registrado.', 'error'); return; }
  let actual = tipo;
  const nombre = producto?.nombre || `Producto #${inventario.productId}`;
  modal({
    titulo: 'Registrar movimiento',
    subtitulo: `${esc(nombre)} · stock actual <strong>${inventario.stockActual}</strong> (mínimo ${inventario.stockMinimo})`,
    contenido: `
      <form class="form-grid" novalidate>
        <div class="segmented full" role="radiogroup" aria-label="Tipo de movimiento">
          ${Object.entries(TIPOS).map(([k, t]) => `<button type="button" role="radio" data-tipo="${k}" class="seg-${t.clase}">${icon(t.icono)}${t.texto}</button>`).join('')}
        </div>
        <p class="field-hint full" data-ayuda></p>
        <label class="field full"><span class="field-label" data-etq>Cantidad</span>
          <div class="stepper">
            <button type="button" class="btn btn-icon btn-ghost" data-menos aria-label="Menos">${icon('minus')}</button>
            <input type="number" name="cantidad" min="0" value="1" autofocus>
            <button type="button" class="btn btn-icon btn-ghost" data-mas aria-label="Más">${icon('plus')}</button>
          </div>
        </label>
        <div class="preview full" data-preview></div>
        <div class="form-alert full" hidden></div>
        <footer class="modal-footer full">
          <button class="btn btn-ghost" type="button" data-cerrar>Cancelar</button>
          <button class="btn btn-primary" type="submit">Registrar movimiento</button>
        </footer>
      </form>`,
    alMontar: (raiz, cerrar) => {
      const form = raiz.querySelector('form');
      const input = form.elements.cantidad;
      raiz.querySelectorAll('[data-cerrar]').forEach((b) => (b.onclick = cerrar));
      const actualizar = () => {
        raiz.querySelectorAll('[data-tipo]').forEach((b) => b.setAttribute('aria-checked', b.dataset.tipo === actual));
        raiz.querySelector('[data-ayuda]').textContent = TIPOS[actual].ayuda;
        raiz.querySelector('[data-etq]').textContent = actual === 'AJUSTE' ? 'Nuevo stock' : 'Cantidad';
        const n = Number(input.value) || 0;
        const nuevo = actual === 'ENTRADA' ? inventario.stockActual + n : actual === 'SALIDA' ? inventario.stockActual - n : n;
        const bajo = nuevo < inventario.stockMinimo;
        raiz.querySelector('[data-preview]').innerHTML = nuevo < 0
          ? `<span class="text-danger">${icon('alert')} No hay suficientes unidades: el stock no puede quedar negativo.</span>`
          : `Stock resultante: <strong>${inventario.stockActual}</strong> → <strong class="${bajo ? 'text-warn' : 'text-ok'}">${nuevo}</strong>${bajo ? ' · quedará bajo el mínimo' : ''}`;
      };
      raiz.querySelectorAll('[data-tipo]').forEach((b) => (b.onclick = () => { actual = b.dataset.tipo; actualizar(); }));
      raiz.querySelector('[data-menos]').onclick = () => { input.value = Math.max(0, (Number(input.value) || 0) - 1); actualizar(); };
      raiz.querySelector('[data-mas]').onclick = () => { input.value = (Number(input.value) || 0) + 1; actualizar(); };
      input.addEventListener('input', actualizar);
      actualizar();

      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const alerta = form.querySelector('.form-alert');
        const boton = form.querySelector('[type=submit]');
        boton.disabled = true;
        try {
          const r = await api.inventory.move(inventario.productId, actual, Number(input.value));
          toast(`${TIPOS[actual].texto} registrada. Stock actual de ${nombre}: ${r.stockActual}.`);
          if (r.alertaStockBajo) toast(`${nombre} quedó por debajo de su stock mínimo.`, 'info');
          window.dispatchEvent(new Event('tienda:cambio'));
          cerrar();
          alTerminar?.();
        } catch (err) {
          alerta.innerHTML = `${icon('alert')}<div><strong>${esc(err.message)}</strong>${(err.details || []).map((x) => `<br>${esc(x)}`).join('')}</div>`;
          alerta.hidden = false;
        } finally {
          boton.disabled = false;
        }
      });
    },
  });
}

async function historial(inv, producto) {
  let movimientos;
  try {
    movimientos = await api.inventory.movements(inv.productId);
  } catch (err) {
    toastError(err);
    return;
  }
  modal({
    titulo: 'Historial de movimientos',
    subtitulo: `${esc(producto?.nombre || `Producto #${inv.productId}`)} · ${movimientos.length} movimientos`,
    contenido: movimientos.length ? `<ol class="timeline">${movimientos.map((m) => {
      const t = TIPOS[m.tipoMovimiento];
      const signo = m.tipoMovimiento === 'ENTRADA' ? '+' : m.tipoMovimiento === 'SALIDA' ? '−' : '=';
      return `<li class="tl-${t.clase}">
        <span class="tl-dot">${icon(t.icono)}</span>
        <div class="grow"><strong>${t.texto}</strong><small class="block muted">${fecha(m.fechaMovimiento)}</small></div>
        <span class="tl-qty">${signo}${m.cantidad}</span>
      </li>`;
    }).join('')}</ol>` : vacio({ icono: 'clock', titulo: 'Sin movimientos' }),
  });
}
