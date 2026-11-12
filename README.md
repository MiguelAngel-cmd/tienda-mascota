# Sistema de Gestión para una Tienda de Mascotas

Proyecto 6 - Programación II (Ingeniería de Sistemas). Arquitectura de microservicios con Java 17,
Spring Boot 3 y PostgreSQL.

## Avance actual: Fases 1, 2 y 3

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
Control de existencias por producto, con historial de movimientos y alertas:
- Solo se registra inventario de productos que existen en Product-Service
- No se permite inventario negativo
- Todos los movimientos quedan registrados: ENTRADA, SALIDA y AJUSTE
- Alerta cuando el stock actual es menor que el stock mínimo

| Método | Ruta | Descripción |
|---|---|---|
| GET | /inventory | Listar el inventario |
| GET | /inventory/alerts | Productos con stock bajo el mínimo |
| GET | /inventory/{productId} | Inventario de un producto |
| POST | /inventory | Registrar inventario de un producto |
| PUT | /inventory/{id} | Actualizar stock actual y mínimo |
| POST | /inventory/{productId}/movements | Registrar un movimiento |
| GET | /inventory/{productId}/movements | Historial de movimientos |

## Cómo ejecutarlo
```bash
docker compose up --build -d
```
Swagger Product-Service: http://localhost:8081/swagger-ui.html

Swagger Customer-Service: http://localhost:8082/swagger-ui.html

<<<<<<< HEAD
Pruebas unitarias: `cd product-service && mvn test` y `cd customer-service && mvn test`
docker run --rm -v "${PWD}/product-service:/app" -w /app maven:3.9-eclipse-temurin-17 mvn test
docker run --rm -v "${PWD}/customer-service:/app" -w /app maven:3.9-eclipse-temurin-17 mvn test
=======
Swagger Inventory-Service: http://localhost:8083/swagger-ui.html

Pruebas unitarias: `mvn test` dentro de cada carpeta de servicio (`product-service`, `customer-service` e `inventory-service`).

### Sin Docker (base de datos en memoria H2)
Abrir una terminal por servicio. Inventory-Service necesita que Product-Service esté corriendo, así que se inicia después.
```bash
cd product-service   && mvn spring-boot:run -Dspring-boot.run.profiles=h2
cd customer-service  && mvn spring-boot:run -Dspring-boot.run.profiles=h2
cd inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
>>>>>>> 2de00d6a94c15509ddf0fe6abd48cc9d68b312cf

## Próximas fases
Sales-Service y despliegue completo con Docker Compose.

<<<<<<< HEAD
=======
## Equipo
Miguel Angel Barinas Vanegas
Andrey Triana Torres
Daniel Alejandro Rojas
>>>>>>> 2de00d6a94c15509ddf0fe6abd48cc9d68b312cf
