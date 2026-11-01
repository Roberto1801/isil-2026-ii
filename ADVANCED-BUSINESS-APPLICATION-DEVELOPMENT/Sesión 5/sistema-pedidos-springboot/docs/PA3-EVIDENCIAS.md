# PA3 — escenarios y registro de evidencias

## Alcance verificable
Las pruebas automáticas son evidencia ejecutable, no capturas de una demostración manual.
No se incluyen capturas fabricadas. El conjunto original tenía 20 pruebas;
Pa3ReportesIntegrationTest aporta 12 y Pa3JspHttpIntegrationTest aporta una.
Pa3MysqlRepositoryTest es una comprobación optativa adicional de MySQL.
Los archivos TXT/XML de Surefire son la fuente del resultado de la última ejecución.

## Preparación manual
1. Usar una base MySQL de laboratorio y configurar DB_URL, DB_USERNAME y DB_PASSWORD
   fuera del repositorio. No mostrar contraseñas, cookies ni tokens en las evidencias.
2. Iniciar el servicio externo real de Sesión 4 y esta aplicación con Java 21.
   Desde el directorio del proyecto: `./mvnw.cmd spring-boot:run`.
   No activar test para la demostración MySQL.
3. Ingresar como OPERADOR. Registrar tres pedidos con nombres de cliente de laboratorio:
   dos para el mismo cliente y uno para otro. Elegir productos y cantidades disponibles.
   Los precios provienen de inventario: calcular las expectativas con esos precios reales.
4. Anotar los IDs y los totales en una tabla de evidencia sin datos personales reales.

Para reproducir las expectativas de las pruebas se usan filas de ensayo:
Ana Torres, cantidad 2, precio 50, total 100;
Carlos Perez, cantidad 1, precio 50, total 50;
Ana Torres, cantidad 4, precio 50, total 200.
Estas filas se insertan únicamente en H2 aislado o en la tabla temporal MySQL del test;
no se afirma que existan en la base persistente de la aplicación.

## Caso 1 — búsqueda avanzada
Consultar la JSP en
`/sistema-pedidos/reportes/pedidos?cliente=Ana&totalMinimo=200`
y JSON en
`/sistema-pedidos/api/v1/pedidos/busqueda-avanzada?cliente=Ana&totalMinimo=200`.
Con las tres filas de ensayo debe aparecer únicamente el pedido Ana/200.
Verificar el límite inclusivo y el orden descendente con un cuarto pedido de mayor ID.
La prueba JDBC adicional incluye ese cuarto pedido.

SQL de contraste de solo lectura, ajustando el nombre al usado en la demostración:
```sql
SELECT id, cliente, cantidad, total, fecha
FROM pedido
WHERE LOWER(cliente) LIKE LOWER('%Ana%') AND total >= 200
ORDER BY id DESC;
```

Evidencia automatizada: filtros y orden JDBC, contrato JSON y modelo MVC.
La prueba HTTP comprueba HTML real y escape de un nombre con caracteres especiales.
Pendiente manual: capturas de JSP y JSON con los mismos IDs que la consulta sobre MySQL persistente.

## Caso 2 — clientes frecuentes
Consultar `/api/v1/pedidos/clientes-frecuentes?minPedidos=2` y la sección JSP.
Con las filas de ensayo, solo Ana cumple:
totalPedidos=2, totalUnidades=6, importeAcumulado=300, ticketPromedio=150.
Carlos no cumple HAVING. Con minPedidos=3 no hay grupos.
```sql
SELECT cliente, COUNT(*) AS totalPedidos, SUM(cantidad) AS totalUnidades,
       SUM(total) AS importeAcumulado, AVG(total) AS ticketPromedio
FROM pedido
GROUP BY cliente
HAVING COUNT(*) >= 2
ORDER BY totalPedidos DESC, cliente ASC;
```
Evidencia automatizada: los cuatro indicadores y exclusión por umbral.
Pendiente manual: contrastar JSP/JSON y resultados SQL con los valores reales del catálogo.

## Caso 3 — validaciones
Solicitar búsqueda con cliente vacío, monto -1 y clientes frecuentes con minPedidos=0.
Se espera 400 y mensaje útil. Probar además números mal formados y parámetros
REST omitidos. En Service también se rechazan null.
La JSP presenta el mensaje; la API devuelve un objeto JSON con campo error.
Evidencia automatizada: servicio, REST y modelo JSP inválido.
Pendiente manual: capturas del estado HTTP y mensaje, sin incluir información sensible.

## Caso 4 — inventario no disponible
Con pedidos locales existentes, detener inventario-service real.
Como OPERADOR y con token válido, intentar registrar un pedido: se espera 503 y
vista error. Abrir directamente /reportes/pedidos y las APIs de reportes: deben
seguir funcionando con datos locales.
No utilizar /pedidos como sustituto de la página de reportes: el listado original
carga el catálogo externo y puede devolver 503 cuando inventario está caído.
Evidencia automatizada: prueba HTTP con adaptador Mockito que lanza
InventarioNoDisponibleException, estado 503, recuento local sin nuevos registros
y reportes 200; verifyNoInteractions antes del registro demuestra independencia.
Pendiente manual: detener y reiniciar el servicio real, guardar capturas de ambas
respuestas y verificar que no hubo reserva remota ni pedido persistente adicional.
Un timeout de red real no se considera demostrado mediante el mock.

## Caso 5 — seguridad
En sesión anónima pedir la JSP y las APIs: redirección al login.
Con CONSULTA consultar ambas APIs/JSP: 200. Enviar POST /pedidos con token válido:
403 por rol. Con OPERADOR enviar POST sin token o con token inválido: 403 por CSRF.
Con OPERADOR y token válido, el test HTTP alcanza el negocio y recibe 503 del
inventario simulado, distinguiendo seguridad de indisponibilidad.
Evidencia automatizada: pruebas originales de CONSULTA+CSRF y OPERADOR sin CSRF;
nuevas pruebas de reportes, rol ajeno y token inválido.
Pendiente manual: capturas de estados usando sesiones separadas para no mezclar roles.

## Ejecución
```powershell
.\mvnw.cmd test
.\mvnw.cmd '-Dpa3.mysql=true' '-Dtest=Pa3MysqlRepositoryTest' test
```
La primera usa H2 y omite el test optativo MySQL. La segunda necesita MySQL y
permiso CREATE TEMPORARY TABLES en la base configurada. La tabla temporal pedido
oculta la persistente solo dentro de su conexión y desaparece al cerrarla;
no se ejecuta DELETE, ALTER ni DROP sobre tablas persistentes.
No equivale a demostrar el flujo de navegador con MySQL: comprueba SQL y RowMapper.

## Registro final
El 30/10/2026, con Java 21.0.11, Maven verify generó el WAR y terminó en BUILD SUCCESS:
34 casos registrados, 33 aprobados, cero fallos, cero errores y uno omitido (MySQL optativo).
La ejecución separada de MySQL falló con Access denied antes de crear la tabla temporal.
Se necesita configurar acceso autorizado mediante DB_* para repetir esa comprobación.
No se cambiaron credenciales ni datos persistentes para intentar resolverlo.

Ver el resumen de ejecución en README. Advertencias no bloqueantes observadas:
autoasociación del agente de Mockito en Java 21 y H2Dialect explícito del docente.
No se cambia configuración ajena a los requisitos para ocultarlas.
Se debe adjuntar a la entrega académica la salida Surefire y completar las capturas
manuales anteriores. No presentar escenarios pendientes como aprobados.
