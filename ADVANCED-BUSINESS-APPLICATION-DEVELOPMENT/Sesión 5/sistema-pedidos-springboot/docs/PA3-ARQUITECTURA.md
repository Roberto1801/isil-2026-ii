# Arquitectura PA3

```mermaid
flowchart TD
  U[Navegador / cliente HTTP] --> S[Spring Security: sesión, roles y CSRF]
  S --> W[PedidoReporteWebController]
  S --> R[PedidoReporteController REST]
  S --> C[PedidoController / PedidoApiController existentes]
  W --> RS[PedidoReporteServiceImpl: validaciones]
  R --> RS
  RS --> JR[PedidoReporteJdbcRepository]
  JR --> NT[NamedParameterJdbcTemplate / JdbcTemplate]
  NT --> DB[(MySQL normal / H2 en pruebas)]
  JR --> P[Proyecciones y PedidoRowMapper]
  W --> M[Model + ViewResolver]
  M --> JSP[WEB-INF/views/reportes-pedidos.jsp]
  R --> DTO[DTO JSON]
  C --> PS[PedidoServiceImpl existente]
  PS --> JPA[PedidoRepository JPA]
  JPA --> DB
  PS --> HTTP[InventarioClient / RestClient existente]
  HTTP --> EXT[inventario-service externo: Sesión 4]
```

La ruta de reportes no usa inventario. Se mantienen las dependencias por constructor.
El repositorio devuelve proyecciones; el servicio las convierte a modelos de dominio
y REST a DTO. El RowMapper existente transforma filas de la búsqueda en Pedido.
Seguridad precede al controlador: autenticar no concede por sí solo permiso de registro.
