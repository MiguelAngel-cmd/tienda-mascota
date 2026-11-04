# Sistema de Gestión para una Tienda de Mascotas

Proyecto 6 - Programación II (Ingeniería de Sistemas). Arquitectura de microservicios con Java 17,
Spring Boot 3 y PostgreSQL.

## Avance actual: Fase 1 - Product-Service
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

## Cómo ejecutarlo
```bash
docker compose up --build -d
```
Swagger: http://localhost:8081/swagger-ui.html

Pruebas unitarias: `cd product-service && mvn test`

## Próximas fases
Customer-Service, Inventory-Service, Sales-Service y despliegue completo con Docker Compose.

## Equipo
| Integrante | Responsabilidad |
|---|---|
| (nombre) | (tarea) |
