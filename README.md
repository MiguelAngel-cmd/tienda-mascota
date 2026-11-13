# Sistema de Gestión para una Tienda de Mascotas

Proyecto 6 - Programación II (Ingeniería de Sistemas). Arquitectura de microservicios con Java 17,
Spring Boot 3 y PostgreSQL.

## Avance actual: Fases 1 a 4

### Fase 1 - Product-Service (puerto 8081)
CRUD de productos con validaciones y reglas de negocio:
- No se permiten códigos de barras duplicados
- No se permiten precios negativos
- Todo producto tiene categoría y nombre

| Método | Ruta | Descripción |
|---|---|---|
| GET | /products | Listar productos |
| GET | /products/{id} | Consultar un producto |
| POST | /products | Crear producto |
| PUT | /products/{id} | Actualizar producto |
| DELETE | /products/{id} | Eliminar producto |

### Fase 2 - Customer-Service (puerto 8082)
CRUD de clientes con validaciones y reglas de negocio:
- No se permiten documentos duplicados
- No se permiten correos repetidos
- Nombre y teléfono son obligatorios
- Se valida el formato del correo electrónico

| Método | Ruta | Descripción |
|---|---|---|
| GET | /customers | Listar clientes |
| GET | /customers/{id} | Consultar un cliente |
| POST | /customers | Registrar cliente |
| PUT | /customers/{id} | Actualizar cliente |
| DELETE | /customers/{id} | Eliminar cliente |

### Fase 3 - Inventory-Service (puerto 8083)
Existencias y movimientos de inventario. Consulta al Product-Service para validar los productos:
- No se registra inventario para productos inexistentes
- No se permite inventario negativo
- Todos los movimientos (ENTRADA, SALIDA, AJUSTE) quedan registrados
- Se genera una alerta cuando el stock es inferior al mínimo

| Método | Ruta | Descripción |
|---|---|---|
| GET | /inventory | Listar el inventario |
| GET | /inventory/{productId} | Consultar el inventario de un producto |
| POST | /inventory | Registrar inventario de un producto |
| PUT | /inventory/{id} | Actualizar stock (registra un AJUSTE) |
| POST | /inventory/{productId}/movements | Registrar ENTRADA, SALIDA o AJUSTE |
| GET | /inventory/{productId}/movements | Historial de movimientos |
| GET | /inventory/alerts | Productos con stock bajo el mínimo |

### Fase 4 - Sales-Service (puerto 8084)
Registro de ventas. Consulta a Customer, Product e Inventory Service (validar cliente, validar productos, verificar stock, registrar la venta y actualizar el inventario):
- No se permiten ventas sin un cliente válido
- No se permiten ventas sin stock disponible (responde 400)
- Subtotales y total se calculan automáticamente con el precio actual del producto
- El inventario se descuenta después de registrar la venta; si falla a la mitad, se devuelve lo descontado y la venta queda CANCELADA
- Estados de venta: PENDIENTE, FACTURADA, CANCELADA

| Método | Ruta | Descripción |
|---|---|---|
| GET | /sales | Historial de ventas |
| GET | /sales/{id} | Consultar una venta con su detalle |
| POST | /sales | Registrar una venta |
| PUT | /sales/{id} | Cancelar una venta (devuelve el stock si estaba facturada) |

Ejemplo de venta:
```json
{ "customerId": 1, "items": [ { "productId": 1, "cantidad": 2 }, { "productId": 2, "cantidad": 1 } ] }
```

## Cómo ejecutarlo
```bash
docker compose up --build -d
```
Swagger Product-Service: http://localhost:8081/swagger-ui.html

Swagger Customer-Service: http://localhost:8082/swagger-ui.html

Swagger Inventory-Service: http://localhost:8083/swagger-ui.html

Swagger Sales-Service: http://localhost:8084/swagger-ui.html

Pruebas unitarias: `mvn test` dentro de cada carpeta de servicio (`product-service`, `customer-service`, `inventory-service` y `sales-service`).

### Sin Docker (base de datos en memoria H2)
Abrir una terminal por servicio. Inventory-Service necesita que Product-Service esté corriendo, y Sales-Service necesita a los otros tres, así que se inicia al final.
```bash
ccd
cd customer-service  && mvn spring-boot:run -Dspring-boot.run.profiles=h2
cd inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=h2
cd sales-service     && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Próximas fases
Fase 5: pruebas de integración y validación del despliegue completo con Docker Compose.

## Equipo
Miguel Angel Barinas Vanegas
Andrey Triana Torres
Daniel Alejandro Rojas
