# Evidencia - Pruebas unitarias y de integración (JUnit 5)

Generado a partir de los reportes de Maven Surefire (`target/surefire-reports`) el 2026-11-17 20:17.

| Servicio | Clase | Tipo | Pruebas | Fallidas | Tiempo (s) |
|---|---|---|---|---|---|
| product-service | `ProductRequestValidationTest` | Validación DTO | 4 | 0 | 0.44 |
| product-service | `ProductIntegrationTest` | Integración | 5 | 0 | 6.33 |
| product-service | `ProductServiceTest` | Unitaria | 9 | 0 | 0.42 |
| customer-service | `CustomerRequestValidationTest` | Validación DTO | 6 | 0 | 0.41 |
| customer-service | `CustomerIntegrationTest` | Integración | 5 | 0 | 6.26 |
| customer-service | `CustomerServiceTest` | Unitaria | 11 | 0 | 0.43 |
| inventory-service | `InventoryRequestValidationTest` | Validación DTO | 6 | 0 | 0.39 |
| inventory-service | `InventoryIntegrationTest` | Integración | 6 | 0 | 7.06 |
| inventory-service | `InventoryServiceTest` | Unitaria | 10 | 0 | 0.29 |
| sales-service | `SaleRequestValidationTest` | Validación DTO | 6 | 0 | 0.60 |
| sales-service | `SaleIntegrationTest` | Integración | 6 | 0 | 6.93 |
| sales-service | `SaleServiceTest` | Unitaria | 16 | 0 | 0.16 |
| **Total** | | | **90** | **0** | |

## Detalle de cada prueba

### product-service · `ProductRequestValidationTest`

- ✅ nombreVacio_esInvalido
- ✅ sinCategoria_esInvalido
- ✅ productoValido_noTieneErrores
- ✅ precioNegativo_esInvalido

### product-service · `ProductIntegrationTest`

- ✅ precioNegativo_responde400ConDetalle
- ✅ caso1_registrarProducto_responde201
- ✅ categoriaInvalida_responde400
- ✅ crudCompleto_consultarActualizarYEliminar
- ✅ codigoDeBarrasDuplicado_responde409

### product-service · `ProductServiceTest`

- ✅ consultarProductos_devuelveLaLista
- ✅ crearProducto_guardaYDevuelveRespuesta
- ✅ crearProducto_conCodigoDuplicado_lanzaExcepcion
- ✅ eliminarProducto_existente_loBorra
- ✅ actualizarProducto_cambiaLosDatos
- ✅ actualizarProducto_conCodigoDeOtroProducto_lanzaExcepcion
- ✅ consultarProducto_inexistente_lanzaNotFound
- ✅ consultarProducto_existente_devuelveSusDatos
- ✅ eliminarProducto_inexistente_lanzaNotFound

### customer-service · `CustomerRequestValidationTest`

- ✅ nombreVacio_esInvalido
- ✅ correoSinDominio_esInvalido
- ✅ telefonoVacio_esInvalido
- ✅ correoSinArroba_esInvalido
- ✅ clienteValido_noTieneErrores
- ✅ sinCorreo_esValido

### customer-service · `CustomerIntegrationTest`

- ✅ correoRepetido_sinImportarMayusculas_responde409
- ✅ crudCompleto_consultarActualizarYEliminar
- ✅ documentoDuplicado_responde409
- ✅ registrarCliente_responde201ConFechaDeRegistro
- ✅ correoMalFormadoYSinTelefono_responde400

### customer-service · `CustomerServiceTest`

- ✅ actualizarCliente_cambiaLosDatos
- ✅ eliminarCliente_existente_loBorra
- ✅ registrarCliente_conCorreoDuplicado_lanzaExcepcion
- ✅ registrarCliente_guardaYDevuelveRespuesta
- ✅ consultarClientes_devuelveLaLista
- ✅ consultarCliente_inexistente_lanzaNotFound
- ✅ eliminarCliente_inexistente_lanzaNotFound
- ✅ registrarCliente_conDocumentoDuplicado_lanzaExcepcion
- ✅ actualizarCliente_conSusPropiosDatos_noEsDuplicado
- ✅ actualizarCliente_conCorreoDeOtroCliente_lanzaExcepcion
- ✅ registrarCliente_sinDocumentoNiCorreo_noConsultaDuplicados

### inventory-service · `InventoryRequestValidationTest`

- ✅ stockMinimoNegativo_esInvalido
- ✅ inventarioValido_noTieneErrores
- ✅ movimientoSinTipo_esInvalido
- ✅ stockNegativo_esInvalido
- ✅ movimientoConCantidadNegativa_esInvalido
- ✅ sinProductId_esInvalido

### inventory-service · `InventoryIntegrationTest`

- ✅ inventarioDuplicado_responde409
- ✅ inventarioDeProductoInexistente_responde400
- ✅ actualizarStock_registraAjuste
- ✅ stockBajoElMinimo_apareceEnAlertas
- ✅ caso2_agregarExistencias
- ✅ salidaMayorAlStock_responde400YNoCambiaElStock

### inventory-service · `InventoryServiceTest`

- ✅ ajuste_dejaElStockEnLaCantidadIndicada
- ✅ registrarInventario_deProductoInexistente_lanzaExcepcion
- ✅ movimientoSobreProductoSinInventario_lanzaNotFound
- ✅ movimientoDeSalida_restaAlStock
- ✅ movimientoDeEntrada_sumaAlStock
- ✅ actualizarStock_registraUnAjusteConElNuevoValor
- ✅ registrarInventario_guardaYRegistraMovimientoDeEntrada
- ✅ actualizarStock_deInventarioInexistente_lanzaNotFound
- ✅ stockPorDebajoDelMinimo_activaLaAlerta
- ✅ salidaMayorAlStock_noPermiteInventarioNegativo

### sales-service · `SaleRequestValidationTest`

- ✅ ventaSinProductos_esInvalida
- ✅ actualizacionSinEstado_esInvalida
- ✅ ventaValida_noTieneErrores
- ✅ ventaSinCliente_esInvalida
- ✅ productoSinId_esInvalido
- ✅ productoConCantidadCero_esInvalido

### sales-service · `SaleIntegrationTest`

- ✅ cancelarVentaFacturada_devuelveElStock
- ✅ caso4_ventaSinExistencias_responde400
- ✅ caso3_registrarVenta
- ✅ ventaSinItems_responde400
- ✅ caso5_historialDeVentas
- ✅ ventaConClienteInexistente_responde400

### sales-service · `SaleServiceTest`

- ✅ registrarVenta_sinStockSuficiente_lanzaExcepcionYNoDescuenta
- ✅ cancelarVenta_siFallaLaDevolucionDeStock_deshaceLoRepuestoYSigueFacturada
- ✅ registrarVenta_conClienteInexistente_lanzaExcepcion
- ✅ consultarVentaInexistente_lanzaNotFound
- ✅ consultarHistorial_devuelveLasVentas
- ✅ registrarVenta_conProductoInactivo_lanzaExcepcion
- ✅ cambiarEstadoADistintoDeCancelada_noEstaPermitido
- ✅ registrarVenta_calculaSubtotalesYTotal_yDescuentaElInventario
- ✅ registrarVenta_siFallaElInventario_devuelveLoDescontadoYCancelaLaVenta
- ✅ registrarVenta_deProductoSinInventario_lanzaExcepcion
- ✅ cancelarVentaYaCancelada_lanzaExcepcion
- ✅ registrarVenta_conProductoInexistente_lanzaExcepcion
- ✅ cancelarVentaFacturada_devuelveElStockAlInventario
- ✅ cancelarVentaPendiente_noTocaElInventario
- ✅ registrarVenta_conProductoRepetido_sumaLasCantidades
- ✅ consultarVenta_conDetalle
