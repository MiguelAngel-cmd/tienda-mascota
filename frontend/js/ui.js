/* Utilidades de interfaz compartidas por todas las pantallas. */

// ---------- Íconos (SVG en línea, trazo de 24x24) ----------
const ICONOS = {
  home: '<path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/><path d="M10 21v-6h4v6"/>',
  box: '<path d="M21 8 12 3 3 8v8l9 5 9-5z"/><path d="m3 8 9 5 9-5"/><path d="M12 13v8"/>',
  users: '<circle cx="9" cy="8" r="4"/><path d="M2 21c0-3.9 3.1-7 7-7s7 3.1 7 7"/><path d="M16 4a4 4 0 0 1 0 8"/><path d="M22 21c0-3-1.8-5.5-4.5-6.5"/>',
  layers: '<path d="m12 2 10 5-10 5L2 7z"/><path d="m2 12 10 5 10-5"/><path d="m2 17 10 5 10-5"/>',
  cart: '<circle cx="9" cy="20" r="1.5"/><circle cx="18" cy="20" r="1.5"/><path d="M2 3h3l2.7 12.4a2 2 0 0 0 2 1.6h7.6a2 2 0 0 0 2-1.6L21 7H6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  minus: '<path d="M5 12h14"/>',
  edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
  trash: '<path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m6 6 1 15h10l1-15"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
  x: '<path d="M18 6 6 18M6 6l12 12"/>',
  alert: '<path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/><path d="M12 9v4"/><path d="M12 17h.01"/>',
  check: '<path d="m5 12 5 5L20 7"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 3"/>',
  arrowDown: '<path d="M12 5v14"/><path d="m6 13 6 6 6-6"/>',
  arrowUp: '<path d="M12 19V5"/><path d="m6 11 6-6 6 6"/>',
  sliders: '<path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12"/><circle cx="16" cy="6" r="2"/><circle cx="10" cy="12" r="2"/><circle cx="18" cy="18" r="2"/>',
  moon: '<path d="M21 13A9 9 0 1 1 11 3a7 7 0 0 0 10 10z"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  eye: '<path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
  ban: '<circle cx="12" cy="12" r="9"/><path d="m5.6 5.6 12.8 12.8"/>',
  refresh: '<path d="M20 11a8 8 0 1 0-2.3 5.7"/><path d="M20 4v7h-7"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  receipt: '<path d="M5 3h14v18l-3-2-2 2-2-2-2 2-2-2-3 2z"/><path d="M9 8h6M9 12h6M9 16h3"/>',
  sparkles: '<path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"/><path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z"/>',
  money: '<rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="3"/><path d="M6 12h.01M18 12h.01"/>',
  trend: '<path d="m3 17 6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
  paw: '<g fill="currentColor" stroke="none"><circle cx="6" cy="9" r="2.2"/><circle cx="10" cy="5" r="2.2"/><circle cx="14" cy="5" r="2.2"/><circle cx="18" cy="9" r="2.2"/><path d="M12 11c-3 0-6 4-6 6.5C6 19 7.2 20 8.7 20c1.2 0 2-.6 3.3-.6s2.1.6 3.3.6c1.5 0 2.7-1 2.7-2.5C18 15 15 11 12 11z"/></g>',
};

export function icon(nombre, clase = '') {
  return `<svg class="icon ${clase}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONOS[nombre] || ''}</svg>`;
}

/** Reemplaza los <span data-icon="..."> del HTML estático por su SVG. */
export function hidratarIconos(raiz = document) {
  raiz.querySelectorAll('[data-icon]').forEach((el) => {
    el.innerHTML = icon(el.dataset.icon);
    el.removeAttribute('data-icon');
  });
}

// ---------- Formato ----------
const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
/** Escapa texto que viene de la API antes de meterlo en el HTML. */
export const esc = (v) => (v === null || v === undefined ? '' : String(v).replace(/[&<>"']/g, (c) => ESCAPES[c]));

const fmtMoneda = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
export const dinero = (n) => fmtMoneda.format(Number(n || 0));
const fmtNumero = new Intl.NumberFormat('es-CO');
export const numero = (n) => fmtNumero.format(Number(n || 0));

export function fecha(iso, conHora = true) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('es-CO', conHora
    ? { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }
    : { day: '2-digit', month: 'short', year: 'numeric' });
}

export function haceCuanto(iso) {
  if (!iso) return '';
  const seg = Math.round((Date.now() - new Date(iso).getTime()) / 1000);
  if (seg < 60) return 'hace un momento';
  const min = Math.round(seg / 60);
  if (min < 60) return `hace ${min} min`;
  const h = Math.round(min / 60);
  if (h < 24) return `hace ${h} h`;
  const d = Math.round(h / 24);
  return d === 1 ? 'ayer' : `hace ${d} días`;
}

export const nombreCliente = (c) => (c ? [c.nombre, c.apellido].filter(Boolean).join(' ') : 'Cliente eliminado');
export const iniciales = (c) => nombreCliente(c).split(/\s+/).slice(0, 2).map((p) => p[0] || '').join('').toUpperCase();

// ---------- Dominio ----------
export const CATEGORIAS = {
  ALIMENTOS: { nombre: 'Alimentos', emoji: '🦴', color: 'amber' },
  MEDICAMENTOS: { nombre: 'Medicamentos', emoji: '💊', color: 'rose' },
  ACCESORIOS: { nombre: 'Accesorios', emoji: '🎀', color: 'violet' },
  JUGUETES: { nombre: 'Juguetes', emoji: '🧸', color: 'sky' },
  HIGIENE: { nombre: 'Higiene', emoji: '🫧', color: 'teal' },
};
export const categoria = (c) => CATEGORIAS[c] || { nombre: c || 'Sin categoría', emoji: '🐾', color: 'teal' };

const ESTADOS_VENTA = {
  FACTURADA: { texto: 'Facturada', clase: 'ok', icono: 'check' },
  PENDIENTE: { texto: 'Pendiente', clase: 'warn', icono: 'clock' },
  CANCELADA: { texto: 'Cancelada', clase: 'danger', icono: 'ban' },
};
export function badgeVenta(estado) {
  const e = ESTADOS_VENTA[estado] || { texto: estado, clase: '', icono: 'clock' };
  return `<span class="badge badge-${e.clase}">${icon(e.icono)}${e.texto}</span>`;
}

/** Estado de stock de un registro de inventario (o null si el producto no tiene inventario). */
export function estadoStock(inv) {
  if (!inv) return { texto: 'Sin inventario', clase: 'muted' };
  if (inv.stockActual === 0) return { texto: 'Agotado', clase: 'danger' };
  if (inv.alertaStockBajo) return { texto: 'Stock bajo', clase: 'warn' };
  return { texto: 'Disponible', clase: 'ok' };
}

// ---------- Toasts ----------
export function toast(mensaje, tipo = 'ok', detalles = []) {
  const cont = document.getElementById('toasts');
  const el = document.createElement('div');
  el.className = `toast toast-${tipo}`;
  el.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
  const ico = { ok: 'check', error: 'alert', info: 'sparkles' }[tipo] || 'check';
  el.innerHTML = `
    <span class="toast-icon">${icon(ico)}</span>
    <div class="toast-body"><p>${esc(mensaje)}</p>${detalles.length ? `<ul>${detalles.map((d) => `<li>${esc(d)}</li>`).join('')}</ul>` : ''}</div>
    <button class="toast-close" type="button" aria-label="Cerrar">${icon('x')}</button>`;
  const cerrar = () => { el.classList.add('saliendo'); setTimeout(() => el.remove(), 200); };
  el.querySelector('.toast-close').onclick = cerrar;
  cont.appendChild(el);
  setTimeout(cerrar, tipo === 'error' ? 7000 : 3800);
}

export const toastError = (err) => toast(err?.message || 'Ocurrió un error inesperado', 'error', err?.details || []);

// ---------- Modales ----------
export function modal({ titulo, subtitulo = '', contenido = '', tamano = 'md', alMontar, alCerrar } = {}) {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `
    <div class="modal modal-${tamano}" role="dialog" aria-modal="true" aria-label="${esc(titulo)}">
      <header class="modal-header">
        <div><h2>${esc(titulo)}</h2>${subtitulo ? `<p>${subtitulo}</p>` : ''}</div>
        <button class="btn btn-icon btn-ghost" type="button" data-cerrar aria-label="Cerrar">${icon('x')}</button>
      </header>
      <div class="modal-body">${contenido}</div>
    </div>`;
  const anterior = document.activeElement;
  let cerrado = false;
  const cerrar = () => {
    if (cerrado) return;
    cerrado = true;
    alCerrar?.();
    overlay.classList.add('saliendo');
    document.removeEventListener('keydown', onKey);
    setTimeout(() => { overlay.remove(); anterior?.focus?.(); }, 160);
  };
  const onKey = (e) => { if (e.key === 'Escape') cerrar(); };
  overlay.addEventListener('mousedown', (e) => { if (e.target === overlay) cerrar(); });
  overlay.querySelectorAll('[data-cerrar]').forEach((b) => b.addEventListener('click', cerrar));
  document.addEventListener('keydown', onKey);
  document.body.appendChild(overlay);
  const raiz = overlay.querySelector('.modal');
  alMontar?.(raiz, cerrar);
  setTimeout(() => (raiz.querySelector('[autofocus], input, select, textarea, .btn-primary') || raiz).focus(), 30);
  return { raiz, cerrar };
}

export function confirmar({ titulo, mensaje, textoConfirmar = 'Confirmar', peligro = false }) {
  return new Promise((resolve) => {
    let respuesta = false;
    modal({
      titulo,
      tamano: 'sm',
      contenido: `
        <p class="confirm-text">${mensaje}</p>
        <footer class="modal-footer">
          <button class="btn btn-ghost" type="button" data-no>Volver</button>
          <button class="btn ${peligro ? 'btn-danger' : 'btn-primary'}" type="button" data-si>${esc(textoConfirmar)}</button>
        </footer>`,
      alMontar: (raiz, cerrar) => {
        raiz.querySelector('[data-si]').onclick = () => { respuesta = true; cerrar(); };
        raiz.querySelector('[data-no]').onclick = cerrar;
      },
      // Escape, clic afuera o "Volver" cuentan como "no"
      alCerrar: () => resolve(respuesta),
    });
  });
}

/**
 * Modal con formulario. `campos` describe cada input; `alEnviar(valores)` debe devolver una promesa.
 * Los errores de validación del backend ("campo: mensaje") se muestran debajo de cada campo.
 */
export function formulario({ titulo, subtitulo, campos, textoEnviar = 'Guardar', alEnviar, tamano = 'md' }) {
  const html = campos.map((c) => {
    if (c.tipo === 'seccion') return `<div class="form-section full">${esc(c.etiqueta)}</div>`;
    const id = `f-${c.nombre}`;
    const req = c.requerido ? 'required' : '';
    const valor = c.valor ?? '';
    let input;
    if (c.tipo === 'select') {
      input = `<select id="${id}" name="${c.nombre}" ${req}>${c.opciones.map((o) =>
        `<option value="${esc(o.valor)}" ${String(o.valor) === String(valor) ? 'selected' : ''}>${esc(o.texto)}</option>`).join('')}</select>`;
    } else if (c.tipo === 'textarea') {
      input = `<textarea id="${id}" name="${c.nombre}" rows="3" placeholder="${esc(c.placeholder || '')}" ${req}>${esc(valor)}</textarea>`;
    } else {
      const extra = [c.min !== undefined ? `min="${c.min}"` : '', c.paso ? `step="${c.paso}"` : '', c.autofocus ? 'autofocus' : ''].join(' ');
      input = `<input id="${id}" name="${c.nombre}" type="${c.tipo || 'text'}" value="${esc(valor)}" placeholder="${esc(c.placeholder || '')}" ${extra} ${req} ${c.soloLectura ? 'readonly' : ''}>`;
    }
    if (c.prefijo) input = `<div class="input-prefix"><span>${esc(c.prefijo)}</span>${input}</div>`;
    return `<label class="field ${c.completo ? 'full' : ''}" for="${id}">
        <span class="field-label">${esc(c.etiqueta)}${c.requerido ? ' <em>*</em>' : ''}</span>
        ${input}
        ${c.ayuda ? `<small class="field-hint">${c.ayuda}</small>` : ''}
        <small class="field-error" data-error="${c.nombre}"></small>
      </label>`;
  }).join('');

  return modal({
    titulo, subtitulo, tamano,
    contenido: `
      <form class="form-grid" novalidate>
        ${html}
        <div class="form-alert full" hidden></div>
        <footer class="modal-footer full">
          <button class="btn btn-ghost" type="button" data-cerrar>Cancelar</button>
          <button class="btn btn-primary" type="submit">${esc(textoEnviar)}</button>
        </footer>
      </form>`,
    alMontar: (raiz, cerrar) => {
      raiz.querySelector('[data-cerrar]').onclick = cerrar;
      const form = raiz.querySelector('form');
      const alerta = form.querySelector('.form-alert');
      form.addEventListener('submit', async (e) => {
        e.preventDefault();
        form.querySelectorAll('.field-error').forEach((x) => (x.textContent = ''));
        form.querySelectorAll('.invalid').forEach((x) => x.classList.remove('invalid'));
        alerta.hidden = true;

        // Validación básica en el navegador (el backend valida de nuevo)
        let ok = true;
        campos.filter((c) => c.requerido).forEach((c) => {
          const el = form.elements[c.nombre];
          if (!String(el.value).trim()) { marcar(c.nombre, 'Este campo es obligatorio'); ok = false; }
        });
        if (!ok) return;

        const valores = {};
        campos.filter((c) => c.nombre).forEach((c) => {
          const v = form.elements[c.nombre].value.trim();
          valores[c.nombre] = c.tipo === 'number' ? (v === '' ? null : Number(v)) : (v === '' ? null : v);
        });

        const boton = form.querySelector('[type=submit]');
        boton.disabled = true;
        boton.classList.add('cargando');
        try {
          await alEnviar(valores);
          cerrar();
        } catch (err) {
          let sinCampo = [];
          (err.details || []).forEach((d) => {
            const [campo, ...resto] = d.split(':');
            if (form.querySelector(`[data-error="${campo.trim()}"]`)) marcar(campo.trim(), resto.join(':').trim());
            else sinCampo.push(d);
          });
          alerta.innerHTML = `${icon('alert')}<div><strong>${esc(err.message)}</strong>${sinCampo.length ? `<br>${sinCampo.map(esc).join('<br>')}` : ''}</div>`;
          alerta.hidden = false;
        } finally {
          boton.disabled = false;
          boton.classList.remove('cargando');
        }
      });
      function marcar(nombre, msg) {
        const err = form.querySelector(`[data-error="${nombre}"]`);
        if (err) err.textContent = msg;
        form.elements[nombre]?.classList.add('invalid');
      }
    },
  });
}

// ---------- Fragmentos ----------
export function vacio({ icono = 'paw', titulo, texto = '', accion = '' }) {
  return `<div class="empty">
      <div class="empty-icon">${icon(icono)}</div>
      <h3>${esc(titulo)}</h3>
      ${texto ? `<p>${texto}</p>` : ''}
      ${accion}
    </div>`;
}

export function avisoErrores(errores) {
  if (!errores?.length) return '';
  const unicos = [...new Set(errores.map((e) => e.message))];
  return `<div class="banner banner-error">${icon('alert')}<div><strong>Algunos datos no se pudieron cargar.</strong><br>${unicos.map(esc).join('<br>')}</div></div>`;
}

export function cabecera({ titulo, subtitulo = '', acciones = '' }) {
  return `<header class="page-header">
      <div><h1>${esc(titulo)}</h1>${subtitulo ? `<p>${subtitulo}</p>` : ''}</div>
      <div class="page-actions">${acciones}</div>
    </header>`;
}

export function esqueleto() {
  return `<div class="skeleton-page">
      <div class="sk sk-title"></div>
      <div class="sk-row">${'<div class="sk sk-card"></div>'.repeat(4)}</div>
      <div class="sk sk-block"></div>
    </div>`;
}

/** Debounce simple para los buscadores. */
export function debounce(fn, ms = 150) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

/** Normaliza texto para búsquedas sin tildes ni mayúsculas. */
export const normalizar = (s) => String(s ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
