# Sistema de Gestión para una Tienda de Mascotas

Proyecto 6 - Programación II (Ingeniería de Sistemas). Arquitectura de microservicios con Java 17,
Spring Boot 3 y PostgreSQL.

## Avance actual: Fases 1 y 2

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

## Cómo ejecutarlo
```bash
docker compose up --build -d
```
Swagger Product-Service: http://localhost:8081/swagger-ui.html

Swagger Customer-Service: http://localhost:8082/swagger-ui.html

Swagger Inventory-Service: http://localhost:8083/swagger-ui.html

Pruebas unitarias: `mvn test` dentro de cada carpeta de servicio (`product-service`, `customer-service` e `inventory-service`).

### Sin Docker (base de datos en memoria H2)
Abrir una terminal por servicio. Inventory-Service necesita que Product-Service esté corriendo, así que se inicia después.
```bash
cd product-service   && mvn spring-boot:run -Dspring-boot.run.profiles=h2
cd customer-service  && mvn spring-boot:run -Dspring-boot.run.profiles=h2
cd inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

## Próximas fases
Inventory-Service, Sales-Service y despliegue completo con Docker Compose.

## Equipo
Miguel Angel Barinas Vanegas
Andrey Triana Torres
Daniel Alejandro Rojas
