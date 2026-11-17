import { api, cargar } from '../api.js';
import {
  CATEGORIAS, avisoErrores, cabecera, categoria, confirmar, debounce, dinero, esc, estadoStock, formulario, icon,
  normalizar, toast, toastError, vacio,
} from '../ui.js';

export async function render(el, { refrescar }) {
  const d = await cargar(['productos', 'inventario']);
  const invPorProducto = new Map(d.inventario.map((i) => [i.productId, i]));
  const filtro = { texto: '', categoria: '', estado: '' };

  el.innerHTML = `
    ${cabecera({
      titulo: 'Productos',
      subtitulo: `${d.productos.length} productos en el catálogo`,
      acciones: `<button class="btn btn-primary" data-nuevo>${icon('plus')}Nuevo producto</button>`,
    })}
    ${avisoErrores(d.errores)}
    <div class="toolbar">
      <label class="search">${icon('search')}<input type="search" placeholder="Buscar por nombre, marca o código de barras" data-buscar></label>
      <select data-estado aria-label="Filtrar por estado">
        <option value="">Todos los estados</option><option value="ACTIVO">Activos</option><option value="INACTIVO">Inactivos</option>
      </select>
    </div>
    <div class="chips" role="tablist" aria-label="Categorías">
      <button class="chip active" data-cat="">Todas <span>${d.productos.length}</span></button>
      ${Object.entries(CATEGORIAS).map(([k, c]) => `<button class="chip" data-cat="${k}">${c.emoji} ${c.nombre}
        <span>${d.productos.filter((p) => p.categoria === k).length}</span></button>`).join('')}
    </div>
    <div data-lista></div>`;

  const lista = el.querySelector('[data-lista]');

  function pintar() {
    const t = normalizar(filtro.texto);
    const visibles = d.productos
      .filter((p) => !filtro.categoria || p.categoria === filtro.categoria)
      .filter((p) => !filtro.estado || p.estado === filtro.estado)
      .filter((p) => !t || normalizar(`${p.nombre} ${p.marca} ${p.codigoBarras} ${p.descripcion}`).includes(t))
      .sort((a, b) => a.nombre.localeCompare(b.nombre));

    if (!d.productos.length) {
      lista.innerHTML = vacio({
        icono: 'box', titulo: 'Todavía no hay productos',
        texto: 'Crea el primer producto del catálogo o carga los datos de demostración desde el Resumen.',
        accion: `<button class="btn btn-primary" data-nuevo>${icon('plus')}Crear producto</button>`,
      });
    } else if (!visibles.length) {
      lista.innerHTML = vacio({ icono: 'search', titulo: 'Sin resultados', texto: 'Prueba con otra búsqueda o categoría.' });
    } else {
      lista.innerHTML = `<div class="product-grid">${visibles.map((p) => tarjeta(p, invPorProducto.get(p.id))).join('')}</div>`;
    }
    lista.querySelectorAll('[data-editar]').forEach((b) => (b.onclick = () => abrirFormulario(d.productos.find((p) => p.id === Number(b.dataset.editar)))));
    lista.querySelectorAll('[data-eliminar]').forEach((b) => (b.onclick = () => eliminar(d.productos.find((p) => p.id === Number(b.dataset.eliminar)))));
    lista.querySelectorAll('[data-nuevo]').forEach((b) => (b.onclick = () => abrirFormulario()));
  }

  el.querySelector('[data-nuevo]').onclick = () => abrirFormulario();
  el.querySelector('[data-buscar]').addEventListener('input', debounce((e) => { filtro.texto = e.target.value; pintar(); }));
  el.querySelector('[data-estado]').addEventListener('change', (e) => { filtro.estado = e.target.value; pintar(); });
  el.querySelectorAll('[data-cat]').forEach((b) => b.addEventListener('click', () => {
    el.querySelectorAll('[data-cat]').forEach((x) => x.classList.toggle('active', x === b));
    filtro.categoria = b.dataset.cat;
    pintar();
  }));
  pintar();

  function abrirFormulario(p) {
    const nuevo = !p;
    const campos = [
      { nombre: 'nombre', etiqueta: 'Nombre', requerido: true, valor: p?.nombre, completo: true, autofocus: true, placeholder: 'Ej. Concentrado Perro Adulto 8 kg' },
      { nombre: 'categoria', etiqueta: 'Categoría', tipo: 'select', requerido: true, valor: p?.categoria || 'ALIMENTOS',
        opciones: Object.entries(CATEGORIAS).map(([k, c]) => ({ valor: k, texto: `${c.emoji} ${c.nombre}` })) },
      { nombre: 'marca', etiqueta: 'Marca', valor: p?.marca, placeholder: 'Ej. Dog Chow' },
      { nombre: 'precio', etiqueta: 'Precio', tipo: 'number', requerido: true, min: 0, paso: '0.01', valor: p?.precio, prefijo: '$' },
      { nombre: 'codigoBarras', etiqueta: 'Código de barras', requerido: true, valor: p?.codigoBarras, placeholder: 'Ej. 7701234567890' },
      { nombre: 'estado', etiqueta: 'Estado', tipo: 'select', valor: p?.estado || 'ACTIVO',
        opciones: [{ valor: 'ACTIVO', texto: 'Activo (se puede vender)' }, { valor: 'INACTIVO', texto: 'Inactivo' }] },
      { nombre: 'descripcion', etiqueta: 'Descripción', tipo: 'textarea', valor: p?.descripcion, completo: true },
    ];
    if (nuevo) {
      campos.push(
        { tipo: 'seccion', etiqueta: 'Inventario inicial (Inventory-Service)' },
        { nombre: 'stockActual', etiqueta: 'Stock inicial', tipo: 'number', min: 0, valor: 0, ayuda: 'Se registra como movimiento de ENTRADA.' },
        { nombre: 'stockMinimo', etiqueta: 'Stock mínimo', tipo: 'number', min: 0, valor: 5, ayuda: 'Por debajo de este valor se genera una alerta.' },
      );
    }
    formulario({
      titulo: nuevo ? 'Nuevo producto' : 'Editar producto',
      subtitulo: nuevo ? 'Se crea en Product-Service y su inventario en Inventory-Service.' : `Producto #${p.id}`,
      textoEnviar: nuevo ? 'Crear producto' : 'Guardar cambios',
      tamano: 'lg',
      campos,
      alEnviar: async (v) => {
        const { stockActual, stockMinimo, ...producto } = v;
        if (nuevo) {
          const creado = await api.products.create(producto);
          try {
            await api.inventory.create({ productId: creado.id, stockActual: stockActual ?? 0, stockMinimo: stockMinimo ?? 0 });
            toast(`Producto "${creado.nombre}" creado con ${stockActual ?? 0} unidades en inventario.`);
          } catch (err) {
            toast(`El producto se creó, pero no se pudo registrar su inventario: ${err.message}`, 'error', err.details);
          }
        } else {
          await api.products.update(p.id, producto);
          toast('Producto actualizado.');
        }
        window.dispatchEvent(new Event('tienda:cambio'));
        refrescar();
      },
    });
  }

  async function eliminar(p) {
    const inv = invPorProducto.get(p.id);
    const ok = await confirmar({
      titulo: 'Eliminar producto',
      mensaje: `¿Seguro que quieres eliminar <strong>${esc(p.nombre)}</strong>?${inv ? ` Tiene <strong>${inv.stockActual}</strong> unidades en inventario.` : ''}
        <br><br><span class="muted">Si el producto ya tiene ventas, es mejor marcarlo como <em>Inactivo</em> para conservar el historial.</span>`,
      textoConfirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    try {
      await api.products.remove(p.id);
      toast('Producto eliminado.');
      refrescar();
    } catch (err) {
      toastError(err);
    }
  }
}

function tarjeta(p, inv) {
  const c = categoria(p.categoria);
  const s = estadoStock(inv);
  return `<article class="product-card ${p.estado === 'INACTIVO' ? 'inactivo' : ''}">
      <div class="product-top">
        <span class="avatar avatar-lg avatar-${c.color}">${c.emoji}</span>
        <div class="product-actions">
          <button class="btn btn-icon btn-ghost btn-sm" data-editar="${p.id}" aria-label="Editar ${esc(p.nombre)}" title="Editar">${icon('edit')}</button>
          <button class="btn btn-icon btn-ghost btn-sm danger" data-eliminar="${p.id}" aria-label="Eliminar ${esc(p.nombre)}" title="Eliminar">${icon('trash')}</button>
        </div>
      </div>
      <p class="eyebrow">${c.nombre}${p.marca ? ` · ${esc(p.marca)}` : ''}</p>
      <h3>${esc(p.nombre)}</h3>
      ${p.descripcion ? `<p class="product-desc">${esc(p.descripcion)}</p>` : ''}
      <div class="product-bottom">
        <span class="price">${dinero(p.precio)}</span>
        <span class="badge badge-${s.clase}">${inv ? `${inv.stockActual} u. · ` : ''}${s.texto}</span>
      </div>
      <footer class="product-meta">
        <span title="Código de barras">▮▯▮ ${esc(p.codigoBarras)}</span>
        ${p.estado === 'INACTIVO' ? '<span class="badge badge-muted">Inactivo</span>' : ''}
      </footer>
    </article>`;
}
