#!/usr/bin/env python3
"""
Pruebas de integración de punta a punta (Fase 5) contra los microservicios REALES.

Ejecuta los 5 casos del enunciado (y algunas reglas extra) llamando por HTTP a los cuatro
servicios desplegados con Docker Compose (o con el perfil H2). Solo usa la librería estándar.

Uso:
    python3 integration-tests/e2e.py                       # puertos directos 8081..8084
    python3 integration-tests/e2e.py --gateway http://localhost:8080/api   # a través de nginx
    python3 integration-tests/e2e.py --reporte docs/evidencias/e2e.md      # además guarda un reporte
"""
import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime

OK, FALLO = "\033[32m✔\033[0m", "\033[31m✘\033[0m"


class Cliente:
    def __init__(self, urls):
        self.urls = urls

    def llamar(self, servicio, metodo, ruta, cuerpo=None):
        url = self.urls[servicio] + ruta
        datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
        req = urllib.request.Request(url, data=datos, method=metodo,
                                     headers={"Content-Type": "application/json"} if datos else {})
        try:
            with urllib.request.urlopen(req, timeout=15) as r:
                texto = r.read().decode()
                return r.status, (json.loads(texto) if texto else None), dict(r.headers)
        except urllib.error.HTTPError as e:
            texto = e.read().decode()
            try:
                return e.code, json.loads(texto), dict(e.headers)
            except ValueError:
                return e.code, texto, dict(e.headers)


class Resultado:
    def __init__(self):
        self.casos = []  # (caso, [(descripcion, ok, detalle)])

    def caso(self, nombre):
        print(f"\n\033[1m{nombre}\033[0m")
        self.casos.append((nombre, []))

    def verificar(self, descripcion, condicion, detalle=""):
        self.casos[-1][1].append((descripcion, bool(condicion), detalle))
        print(f"  {OK if condicion else FALLO} {descripcion}" + ("" if condicion else f"  ->  {detalle}"))
        return condicion

    @property
    def total(self):
        return sum(len(c[1]) for c in self.casos)

    @property
    def fallidas(self):
        return sum(1 for c in self.casos for v in c[1] if not v[1])


def esperar_servicios(cli, segundos=120):
    """Espera a que los 4 servicios respondan (útil justo después de `docker compose up`)."""
    rutas = {"product": "/products", "customer": "/customers", "inventory": "/inventory", "sales": "/sales"}
    limite = time.time() + segundos
    pendientes = set(rutas)
    while pendientes and time.time() < limite:
        for s in list(pendientes):
            try:
                if cli.llamar(s, "GET", rutas[s])[0] == 200:
                    pendientes.discard(s)
            except OSError:
                pass
        if pendientes:
            time.sleep(3)
    if pendientes:
        sys.exit(f"Servicios sin responder: {', '.join(sorted(pendientes))}")


def ejecutar(cli):
    r = Resultado()
    sufijo = str(int(time.time() * 1000))

    # ------------------------------------------------------------------ Caso 1
    r.caso("Caso 1 - Registrar un producto (POST /products)")
    st, prod, h = cli.llamar("product", "POST", "/products", {
        "nombre": "Concentrado Gato Adulto 3 kg", "categoria": "ALIMENTOS", "marca": "Whiskas",
        "descripcion": "Prueba E2E", "precio": 62500, "codigoBarras": f"E2E-{sufijo}"})
    r.verificar("Responde HTTP 201 CREATED", st == 201, f"HTTP {st}: {prod}")
    r.verificar("Devuelve la cabecera Location", "/products/" in h.get("Location", ""), h)
    pid = prod["id"]
    st, cuerpo, _ = cli.llamar("product", "POST", "/products", {
        "nombre": "Duplicado", "categoria": "JUGUETES", "precio": 1000, "codigoBarras": f"E2E-{sufijo}"})
    r.verificar("Código de barras duplicado -> HTTP 409", st == 409, f"HTTP {st}")
    st, cuerpo, _ = cli.llamar("product", "POST", "/products", {
        "nombre": "Precio negativo", "categoria": "HIGIENE", "precio": -1, "codigoBarras": f"NEG-{sufijo}"})
    r.verificar("Precio negativo -> HTTP 400", st == 400, f"HTTP {st}")

    # ------------------------------------------------------------------ Caso 2
    r.caso("Caso 2 - Agregar existencias (Inventory-Service -> Product-Service)")
    st, inv, _ = cli.llamar("inventory", "POST", "/inventory", {"productId": pid, "stockActual": 10, "stockMinimo": 3})
    r.verificar("Producto validado: inventario creado (HTTP 201)", st == 201, f"HTTP {st}: {inv}")
    st, inv, _ = cli.llamar("inventory", "POST", f"/inventory/{pid}/movements", {"tipoMovimiento": "ENTRADA", "cantidad": 5})
    r.verificar("Inventario actualizado con una ENTRADA (10 + 5 = 15)", st == 201 and inv["stockActual"] == 15, f"HTTP {st}: {inv}")
    st, cuerpo, _ = cli.llamar("inventory", "POST", "/inventory", {"productId": 99999999, "stockActual": 1, "stockMinimo": 1})
    r.verificar("Producto inexistente -> HTTP 400 (lo confirma Product-Service)", st == 400, f"HTTP {st}: {cuerpo}")
    st, movs, _ = cli.llamar("inventory", "GET", f"/inventory/{pid}/movements")
    r.verificar("Todos los movimientos quedan registrados", st == 200 and len(movs) == 2, movs)

    # ------------------------------------------------------------------ Caso 3
    r.caso("Caso 3 - Registrar una venta (Sales -> Customer -> Product -> Inventory)")
    st, cli_, _ = cli.llamar("customer", "POST", "/customers", {
        "nombre": "Laura", "apellido": "E2E", "documento": sufijo, "telefono": "3001234567",
        "correo": f"e2e{sufijo}@correo.com"})
    r.verificar("Cliente registrado (HTTP 201)", st == 201, f"HTTP {st}: {cli_}")
    cid = cli_["id"]
    st, venta, _ = cli.llamar("sales", "POST", "/sales", {"customerId": cid, "items": [{"productId": pid, "cantidad": 4}]})
    r.verificar("Cliente validado, stock verificado: venta registrada (HTTP 201)", st == 201, f"HTTP {st}: {venta}")
    r.verificar("Estado FACTURADA", venta.get("estado") == "FACTURADA", venta)
    r.verificar("Total calculado automáticamente (62.500 x 4 = 250.000)", float(venta.get("total", 0)) == 250000, venta)
    vid = venta["id"]
    st, inv, _ = cli.llamar("inventory", "GET", f"/inventory/{pid}")
    r.verificar("Inventario descontado automáticamente (15 - 4 = 11)", inv["stockActual"] == 11, inv)
    st, cuerpo, _ = cli.llamar("sales", "POST", "/sales", {"customerId": 99999999, "items": [{"productId": pid, "cantidad": 1}]})
    r.verificar("Cliente inexistente -> HTTP 400", st == 400, f"HTTP {st}: {cuerpo}")

    # ------------------------------------------------------------------ Caso 4
    r.caso("Caso 4 - Vender un producto sin existencias")
    st, cuerpo, _ = cli.llamar("sales", "POST", "/sales", {"customerId": cid, "items": [{"productId": pid, "cantidad": 999}]})
    r.verificar("Responde HTTP 400 BAD REQUEST", st == 400, f"HTTP {st}: {cuerpo}")
    r.verificar("Mensaje 'Stock insuficiente'", isinstance(cuerpo, dict) and "Stock insuficiente" in cuerpo.get("message", ""), cuerpo)
    st, inv, _ = cli.llamar("inventory", "GET", f"/inventory/{pid}")
    r.verificar("El intento fallido no descuenta stock (sigue en 11)", inv["stockActual"] == 11, inv)

    # ------------------------------------------------------------------ Caso 5
    r.caso("Caso 5 - Consultar historial de ventas (GET /sales)")
    st, ventas, _ = cli.llamar("sales", "GET", "/sales")
    r.verificar("Responde HTTP 200 con un listado", st == 200 and isinstance(ventas, list) and ventas, f"HTTP {st}")
    r.verificar("El listado incluye la venta del Caso 3", any(v["id"] == vid for v in ventas), [v["id"] for v in ventas])
    st, detalle, _ = cli.llamar("sales", "GET", f"/sales/{vid}")
    r.verificar("GET /sales/{id} trae el detalle con subtotales", st == 200 and float(detalle["detalles"][0]["subtotal"]) == 250000, detalle)

    # ------------------------------------------------------------------ Extra
    r.caso("Extra - Cancelar una venta devuelve el stock")
    st, venta, _ = cli.llamar("sales", "PUT", f"/sales/{vid}", {"estado": "CANCELADA"})
    r.verificar("Venta CANCELADA (HTTP 200)", st == 200 and venta["estado"] == "CANCELADA", f"HTTP {st}: {venta}")
    st, inv, _ = cli.llamar("inventory", "GET", f"/inventory/{pid}")
    r.verificar("Las 4 unidades vuelven al inventario (11 + 4 = 15)", inv["stockActual"] == 15, inv)
    return r


def reporte_md(r, urls, ruta):
    lineas = [
        "# Evidencia - Pruebas de integración de punta a punta",
        "",
        f"- Fecha de ejecución: {datetime.now():%Y-%m-%d %H:%M:%S}",
        f"- Servicios: {', '.join(f'{k}={v}' for k, v in urls.items())}",
        f"- Resultado: **{r.total - r.fallidas}/{r.total} verificaciones correctas**",
        "",
    ]
    for nombre, checks in r.casos:
        lineas += [f"## {nombre}", "", "| Resultado | Verificación |", "|---|---|"]
        lineas += [f"| {'✅' if ok else '❌'} | {d} |" for d, ok, _ in checks]
        lineas.append("")
    os.makedirs(os.path.dirname(ruta) or ".", exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        f.write("\n".join(lineas))
    print(f"\nReporte guardado en {ruta}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default="http://localhost", help="host de los servicios (puertos 8081-8084)")
    ap.add_argument("--gateway", help="URL base del proxy nginx, p. ej. http://localhost:8080/api")
    ap.add_argument("--reporte", help="ruta de un archivo Markdown donde guardar la evidencia")
    args = ap.parse_args()
    if args.gateway:
        urls = {s: args.gateway.rstrip("/") for s in ("product", "customer", "inventory", "sales")}
    else:
        urls = {"product": f"{args.host}:8081", "customer": f"{args.host}:8082",
                "inventory": f"{args.host}:8083", "sales": f"{args.host}:8084"}
    cli = Cliente(urls)
    esperar_servicios(cli)
    r = ejecutar(cli)
    print(f"\n{r.total - r.fallidas}/{r.total} verificaciones correctas")
    if args.reporte:
        reporte_md(r, urls, args.reporte)
    sys.exit(1 if r.fallidas else 0)


if __name__ == "__main__":
    main()
