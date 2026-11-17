import { api, cargar } from '../api.js';
import {
  avisoErrores, badgeVenta, cabecera, confirmar, debounce, dinero, esc, fecha, formulario, icon, iniciales, modal,
  nombreCliente, normalizar, toast, toastError, vacio,
} from '../ui.js';

export async function render(el, { refrescar }) {
  const d = await cargar(['clientes', 'ventas']);
  // Resumen de compras por cliente (cruce Customer-Service + Sales-Service)
  const compras = new Map();
  d.ventas.filter((v) => v.estado === 'FACTURADA').forEach((v) => {
    const x = compras.get(v.customerId) || { cantidad: 0, total: 0, ultima: null };
    x.cantidad++; x.total += Number(v.total);
    if (!x.ultima || v.fechaVenta > x.ultima) x.ultima = v.fechaVenta;
    compras.set(v.customerId, x);
  });
  let texto = '';

  el.innerHTML = `
    ${cabecera({
      titulo: 'Clientes',
      subtitulo: `${d.clientes.length} clientes registrados`,
      acciones: `<button class="btn btn-primary" data-nuevo>${icon('plus')}Nuevo cliente</button>`,
    })}
    ${avisoErrores(d.errores)}
    <div class="toolbar">
      <label class="search">${icon('search')}<input type="search" placeholder="Buscar por nombre, documento, teléfono o correo" data-buscar></label>
    </div>
    <div class="card card-flush" data-lista></div>`;

  const lista = el.querySelector('[data-lista]');

  function pintar() {
    const t = normalizar(texto);
    const visibles = d.clientes
      .filter((c) => !t || normalizar(`${c.nombre} ${c.apellido} ${c.documento} ${c.telefono} ${c.correo}`).includes(t))
      .sort((a, b) => nombreCliente(a).localeCompare(nombreCliente(b)));
    if (!d.clientes.length) {
      lista.innerHTML = vacio({
        icono: 'users', titulo: 'Aún no hay clientes', texto: 'Registra a tu primer cliente para poder venderle.',
        accion: `<button class="btn btn-primary" data-nuevo>${icon('plus')}Registrar cliente</button>`,
      });
    } else if (!visibles.length) {
      lista.innerHTML = vacio({ icono: 'search', titulo: 'Sin resultados', texto: 'Ningún cliente coincide con la búsqueda.' });
    } else {
      lista.innerHTML = `<div class="table-wrap"><table class="table">
        <thead><tr><th>Cliente</th><th>Documento</th><th>Contacto</th><th>Dirección</th><th class="num">Compras</th><th>Registro</th><th></th></tr></thead>
        <tbody>${visibles.map((c) => {
          const x = compras.get(c.id);
          return `<tr>
            <td><span class="person"><span class="avatar">${esc(iniciales(c))}</span><span><strong>${esc(nombreCliente(c))}</strong><small class="block muted">ID ${c.id}</small></span></span></td>
            <td>${esc(c.documento) || '<span class="muted">—</span>'}</td>
            <td>${esc(c.telefono)}${c.correo ? `<small class="block muted">${esc(c.correo)}</small>` : ''}</td>
            <td class="truncate">${esc(c.direccion) || '<span class="muted">—</span>'}</td>
            <td class="num">${x ? `<strong>${dinero(x.total)}</strong><small class="block muted">${x.cantidad} ${x.cantidad === 1 ? 'compra' : 'compras'}</small>` : '<span class="muted">Sin compras</span>'}</td>
            <td>${fecha(c.fechaRegistro, false)}</td>
            <td class="row-actions">
              <button class="btn btn-icon btn-ghost btn-sm" data-ver="${c.id}" title="Ver compras" aria-label="Ver compras">${icon('eye')}</button>
              <button class="btn btn-icon btn-ghost btn-sm" data-editar="${c.id}" title="Editar" aria-label="Editar">${icon('edit')}</button>
              <button class="btn btn-icon btn-ghost btn-sm danger" data-eliminar="${c.id}" title="Eliminar" aria-label="Eliminar">${icon('trash')}</button>
            </td>
          </tr>`;
        }).join('')}</tbody></table></div>`;
    }
    const buscar = (id) => d.clientes.find((c) => c.id === Number(id));
    lista.querySelectorAll('[data-editar]').forEach((b) => (b.onclick = () => abrirFormulario(buscar(b.dataset.editar))));
    lista.querySelectorAll('[data-eliminar]').forEach((b) => (b.onclick = () => eliminar(buscar(b.dataset.eliminar))));
    lista.querySelectorAll('[data-ver]').forEach((b) => (b.onclick = () => verCompras(buscar(b.dataset.ver))));
    lista.querySelectorAll('[data-nuevo]').forEach((b) => (b.onclick = () => abrirFormulario()));
  }

  el.querySelector('[data-nuevo]').onclick = () => abrirFormulario();
  el.querySelector('[data-buscar]').addEventListener('input', debounce((e) => { texto = e.target.value; pintar(); }));
  pintar();

  function abrirFormulario(c) {
    const nuevo = !c;
    formulario({
      titulo: nuevo ? 'Nuevo cliente' : 'Editar cliente',
      subtitulo: nuevo ? 'El documento y el correo no se pueden repetir.' : `Cliente #${c.id} · registrado el ${fecha(c.fechaRegistro, false)}`,
      textoEnviar: nuevo ? 'Registrar cliente' : 'Guardar cambios',
      campos: [
        { nombre: 'nombre', etiqueta: 'Nombre', requerido: true, valor: c?.nombre, autofocus: true },
        { nombre: 'apellido', etiqueta: 'Apellido', valor: c?.apellido },
        { nombre: 'documento', etiqueta: 'Documento', valor: c?.documento, placeholder: 'Cédula o NIT' },
        { nombre: 'telefono', etiqueta: 'Teléfono', tipo: 'tel', requerido: true, valor: c?.telefono, placeholder: 'Ej. 3001234567' },
        { nombre: 'correo', etiqueta: 'Correo electrónico', tipo: 'email', valor: c?.correo, completo: true, placeholder: 'nombre@correo.com' },
        { nombre: 'direccion', etiqueta: 'Dirección', valor: c?.direccion, completo: true },
      ],
      alEnviar: async (v) => {
        if (nuevo) await api.customers.create(v);
        else await api.customers.update(c.id, v);
        toast(nuevo ? `Cliente ${v.nombre} registrado.` : 'Cliente actualizado.');
        refrescar();
      },
    });
  }

  async function eliminar(c) {
    const x = compras.get(c.id);
    const ok = await confirmar({
      titulo: 'Eliminar cliente',
      mensaje: `¿Seguro que quieres eliminar a <strong>${esc(nombreCliente(c))}</strong>?${x ? `<br><br><span class="muted">Tiene ${x.cantidad} compras registradas; seguirán en el historial de ventas.</span>` : ''}`,
      textoConfirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    try {
      await api.customers.remove(c.id);
      toast('Cliente eliminado.');
      refrescar();
    } catch (err) {
      toastError(err);
    }
  }

  function verCompras(c) {
    const ventas = d.ventas.filter((v) => v.customerId === c.id);
    const x = compras.get(c.id);
    modal({
      titulo: nombreCliente(c),
      subtitulo: [c.documento && `Doc. ${esc(c.documento)}`, esc(c.telefono), esc(c.correo)].filter(Boolean).join(' · '),
      contenido: `
        <div class="mini-kpis">
          <div><span class="muted">Total comprado</span><strong>${dinero(x?.total)}</strong></div>
          <div><span class="muted">Compras facturadas</span><strong>${x?.cantidad || 0}</strong></div>
          <div><span class="muted">Última compra</span><strong>${x ? fecha(x.ultima, false) : '—'}</strong></div>
        </div>
        ${ventas.length ? `<ul class="list">${ventas.map((v) => `<li>
            <span><strong>Venta #${v.id}</strong><small class="block muted">${fecha(v.fechaVenta)}</small></span>
            <span class="grow"></span>${badgeVenta(v.estado)}<strong class="num">${dinero(v.total)}</strong></li>`).join('')}</ul>`
          : vacio({ icono: 'receipt', titulo: 'Sin compras', texto: 'Este cliente todavía no tiene ventas.' })}`,
    });
  }
}
