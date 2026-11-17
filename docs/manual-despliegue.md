# Manual de despliegue

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| Docker Desktop (o Docker Engine + Compose v2) | reciente | despliegue completo |
| Git | reciente | clonar el repositorio |
| Java JDK 17 o 21 + Maven 3.9 | opcional | ejecutar sin Docker y correr las pruebas |
| Python 3.8+ | opcional | pruebas de punta a punta (`integration-tests/e2e.py`) |
| Postman | opcional | colección de pruebas |

Puertos que deben estar libres: **8080** (frontend), **8081-8084** (servicios) y **5433-5436** (PostgreSQL).

## 2. Despliegue con Docker Compose

```bash
git clone https://github.com/MiguelAngel-cmd/tienda-mascota.git
```
```bash
cd tienda-mascota
```
```bash
docker compose up --build -d
```

La primera vez tarda unos minutos porque Maven descarga las dependencias dentro de cada imagen.
Compose arranca los contenedores en orden y espera a que cada uno esté `healthy`:

```
postgres-*  →  product-service, customer-service  →  inventory-service  →  sales-service  →  frontend
```

Ver el estado:

```bash
docker compose ps
```

Todos deben aparecer como `Up (healthy)` excepto `frontend`, que aparece como `Up`.

| Componente | URL |
|---|---|
| Aplicación web | http://localhost:8080 |
| Swagger de cada servicio | http://localhost:8081/swagger-ui.html (8082, 8083, 8084) |
| Salud de un servicio | http://localhost:8081/actuator/health |

### Variables de entorno

Copiar `.env.example` como `.env` para cambiar usuario/contraseña de PostgreSQL o el puerto del
frontend. Docker Compose lee `.env` automáticamente.

```bash
cp .env.example .env
```

### Contenedores y red

| Contenedor | Imagen | Puerto host → contenedor |
|---|---|---|
| postgres-products | postgres:16 | 5433 → 5432 |
| postgres-customers | postgres:16 | 5434 → 5432 |
| postgres-inventory | postgres:16 | 5435 → 5432 |
| postgres-sales | postgres:16 | 5436 → 5432 |
| product-service | build `./product-service` | 8081 → 8081 |
| customer-service | build `./customer-service` | 8082 → 8082 |
| inventory-service | build `./inventory-service` | 8083 → 8083 |
| sales-service | build `./sales-service` | 8084 → 8084 |
| frontend | build `./frontend` (nginx) | 8080 → 80 |

Todos están en la red `tienda-net`; dentro de ella se llaman por el nombre del contenedor
(por ejemplo `http://product-service:8081`). Los datos de PostgreSQL quedan en volúmenes con nombre
(`postgres-*-data`), así que sobreviven a `docker compose down`.

Cada microservicio se construye en dos etapas (`Dockerfile`): una imagen con Maven compila el JAR y
la imagen final solo lleva el JRE 17 y el JAR.

## 3. Verificar el despliegue

1. Abrir http://localhost:8080. En el menú lateral, el bloque **Microservicios** debe mostrar los
   cuatro servicios en verde.
2. Pulsar **Cargar datos de demostración** en el Resumen.
3. Ejecutar las pruebas de punta a punta:

```bash
python3 integration-tests/e2e.py
```

   Debe terminar con `22/22 verificaciones correctas`. Para probar también el proxy de nginx:

```bash
python3 integration-tests/e2e.py --gateway http://localhost:8080/api
```

4. O, en Postman: importar `postman/tienda-mascota.postman_collection.json` y el entorno
   `postman/local.postman_environment.json`, y ejecutar la colección con el **Collection Runner**.

## 4. Operación

| Acción | Comando |
|---|---|
| Ver logs de un servicio | `docker compose logs -f sales-service` |
| Reiniciar un servicio | `docker compose restart inventory-service` |
| Reconstruir después de cambiar código | `docker compose up --build -d` |
| Detener todo (conserva los datos) | `docker compose down` |
| Detener y **borrar los datos** | `docker compose down -v` |
| Entrar a una base de datos | `docker exec -it postgres-sales psql -U postgres -d sales_db` |

Las alertas de stock bajo también quedan en el log de Inventory-Service (`ALERTA DE STOCK BAJO`):

```bash
docker compose logs inventory-service | grep ALERTA
```

## 5. Ejecución sin Docker (perfil H2)

Cada servicio puede correr con una base de datos en memoria; los datos se pierden al detenerlo.
Abrir una terminal por servicio, en este orden:

```bash
cd product-service   && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
```bash
cd customer-service  && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
```bash
cd inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
```bash
cd sales-service     && mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Frontend (en otra terminal):

```bash
cd frontend && python3 -m http.server 5500
```

Abrir http://localhost:5500. En los puertos 5500, 5173, 3000 y 8000 el frontend llama directo a
`localhost:8081-8084` (los servicios tienen CORS habilitado). También se puede forzar con `?directo`
en la URL.

## 6. Pruebas automatizadas

```bash
cd product-service && mvn test
```

Repetir en `customer-service`, `inventory-service` y `sales-service`. Los reportes quedan en
`target/surefire-reports/`.

## 7. Solución de problemas

| Síntoma | Causa probable y solución |
|---|---|
| `port is already allocated` | Otro programa usa el puerto. Cambiar `FRONTEND_PORT` en `.env` o liberar el puerto (p. ej. un PostgreSQL local en 5433). |
| Un servicio queda `unhealthy` | Revisar `docker compose logs <servicio>`. Suele ser la base de datos todavía arrancando; `docker compose up -d` lo reintenta. |
| En el frontend un servicio aparece en rojo | Ese contenedor está caído o reiniciando: `docker compose ps` y `docker compose logs`. |
| Venta rechazada con 503 | Sales-Service no pudo hablar con otro servicio; verificar que los cuatro estén `healthy`. |
| Venta rechazada con 400 "Stock insuficiente" | Es la regla de negocio: registrar una ENTRADA en Inventario. |
| "El producto X no tiene inventario registrado" | Registrar su inventario en la pantalla Inventario (o crear el producto desde el frontend, que lo registra automáticamente). |
| Cambios en el código no se ven | Reconstruir con `docker compose up --build -d`. |
| Datos viejos o inconsistentes de pruebas anteriores | `docker compose down -v` y volver a levantar. |
