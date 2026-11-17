import { api } from './api.js';

/* Datos de demostración: pasan por las mismas APIs que usa el resto de la aplicación,
   así que también sirven para comprobar la integración de los cuatro microservicios. */

const PRODUCTOS = [
  ['Concentrado Perro Adulto 8 kg', 'ALIMENTOS', 'Dog Chow', 'Croquetas para perros adultos de raza mediana.', 128900, '7701000000011', 24, 6],
  ['Alimento Gato Salmón 3 kg', 'ALIMENTOS', 'Whiskas', 'Alimento seco sabor salmón para gatos adultos.', 62500, '7701000000028', 18, 5],
  ['Snacks Dentales x7', 'ALIMENTOS', 'Pedigree', 'Barras que ayudan a limpiar los dientes.', 15900, '7701000000035', 40, 10],
  ['Antipulgas Pipeta 10-20 kg', 'MEDICAMENTOS', 'Frontline', 'Protección contra pulgas y garrapatas por 30 días.', 54000, '7701000000042', 4, 8],
  ['Desparasitante Tabletas x4', 'MEDICAMENTOS', 'Drontal', 'Tabletas para perros y gatos.', 38700, '7701000000059', 12, 4],
  ['Collar Reflectivo Talla M', 'ACCESORIOS', 'PetSafe', 'Collar ajustable con banda reflectiva.', 24900, '7701000000066', 15, 5],
  ['Cama Acolchada Mediana', 'ACCESORIOS', 'Huellitas', 'Cama lavable con bordes acolchados.', 119000, '7701000000073', 2, 3],
  ['Pelota de Caucho con Sonido', 'JUGUETES', 'Kong', 'Pelota resistente para morder.', 18500, '7701000000080', 30, 8],
  ['Rascador para Gato 60 cm', 'JUGUETES', 'Catit', 'Poste rascador con plataforma.', 89900, '7701000000097', 6, 2],
  ['Shampoo Avena 500 ml', 'HIGIENE', 'Petys', 'Shampoo hipoalergénico para piel sensible.', 22800, '7701000000103', 20, 6],
  ['Arena Sanitaria 10 kg', 'HIGIENE', 'Cat Litter', 'Arena aglomerante con control de olores.', 34900, '7701000000110', 0, 5],
];

const CLIENTES = [
  ['Laura', 'Gómez', '1032456789', '3001234567', 'laura.gomez@correo.com', 'Calle 45 # 12-30, Bogotá'],
  ['Carlos', 'Pérez', '80123456', '3109876543', 'carlos.perez@correo.com', 'Carrera 7 # 80-15, Bogotá'],
  ['Valentina', 'Ríos', '1019988776', '3205551234', 'vale.rios@correo.com', 'Av. Boyacá # 63-20, Bogotá'],
  ['Andrés', 'Martínez', '79654321', '3114443322', 'andres.m@correo.com', 'Calle 26 # 68-10, Bogotá'],
  ['Sofía', 'Herrera', '1000123987', '3157778899', '', 'Transversal 22 # 5-40, Soacha'],
];

export async function cargarDemo() {
  const existentes = await api.products.list();
  const codigos = new Set(existentes.map((p) => p.codigoBarras));
  const creados = [];
  for (const [nombre, categoria, marca, descripcion, precio, codigoBarras, stock, minimo] of PRODUCTOS) {
    if (codigos.has(codigoBarras)) continue;
    const p = await api.products.create({ nombre, categoria, marca, descripcion, precio, codigoBarras, estado: 'ACTIVO' });
    await api.inventory.create({ productId: p.id, stockActual: stock, stockMinimo: minimo });
    creados.push(p);
  }

  const clientes = [];
  for (const [nombre, apellido, documento, telefono, correo, direccion] of CLIENTES) {
    try {
      clientes.push(await api.customers.create({ nombre, apellido, documento, telefono, correo, direccion }));
    } catch (e) {
      if (e.status !== 409) throw e; // ya existía: se ignora
    }
  }

  // Algunas ventas reales (descuentan inventario a través de Sales-Service)
  let ventas = 0;
  if (creados.length >= 6 && clientes.length >= 3) {
    const pedidos = [
      [clientes[0], [[creados[0], 1], [creados[2], 2]]],
      [clientes[1], [[creados[7], 3]]],
      [clientes[2], [[creados[1], 1], [creados[9], 1], [creados[5], 1]]],
      [clientes[0], [[creados[4], 1]]],
    ];
    for (const [cliente, items] of pedidos) {
      await api.sales.create({
        customerId: cliente.id,
        items: items.map(([p, cantidad]) => ({ productId: p.id, cantidad })),
      });
      ventas++;
    }
  }
  return { productos: creados.length, clientes: clientes.length, ventas };
}
