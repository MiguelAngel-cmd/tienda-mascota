# Evidencia - Pruebas de integración de punta a punta

- Fecha de ejecución: 2026-11-17 20:08:23
- Servicios: product=http://localhost:8081, customer=http://localhost:8082, inventory=http://localhost:8083, sales=http://localhost:8084
- Resultado: **22/22 verificaciones correctas**

## Caso 1 - Registrar un producto (POST /products)

| Resultado | Verificación |
|---|---|
| ✅ | Responde HTTP 201 CREATED |
| ✅ | Devuelve la cabecera Location |
| ✅ | Código de barras duplicado -> HTTP 409 |
| ✅ | Precio negativo -> HTTP 400 |

## Caso 2 - Agregar existencias (Inventory-Service -> Product-Service)

| Resultado | Verificación |
|---|---|
| ✅ | Producto validado: inventario creado (HTTP 201) |
| ✅ | Inventario actualizado con una ENTRADA (10 + 5 = 15) |
| ✅ | Producto inexistente -> HTTP 400 (lo confirma Product-Service) |
| ✅ | Todos los movimientos quedan registrados |

## Caso 3 - Registrar una venta (Sales -> Customer -> Product -> Inventory)

| Resultado | Verificación |
|---|---|
| ✅ | Cliente registrado (HTTP 201) |
| ✅ | Cliente validado, stock verificado: venta registrada (HTTP 201) |
| ✅ | Estado FACTURADA |
| ✅ | Total calculado automáticamente (62.500 x 4 = 250.000) |
| ✅ | Inventario descontado automáticamente (15 - 4 = 11) |
| ✅ | Cliente inexistente -> HTTP 400 |

## Caso 4 - Vender un producto sin existencias

| Resultado | Verificación |
|---|---|
| ✅ | Responde HTTP 400 BAD REQUEST |
| ✅ | Mensaje 'Stock insuficiente' |
| ✅ | El intento fallido no descuenta stock (sigue en 11) |

## Caso 5 - Consultar historial de ventas (GET /sales)

| Resultado | Verificación |
|---|---|
| ✅ | Responde HTTP 200 con un listado |
| ✅ | El listado incluye la venta del Caso 3 |
| ✅ | GET /sales/{id} trae el detalle con subtotales |

## Extra - Cancelar una venta devuelve el stock

| Resultado | Verificación |
|---|---|
| ✅ | Venta CANCELADA (HTTP 200) |
| ✅ | Las 4 unidades vuelven al inventario (11 + 4 = 15) |
