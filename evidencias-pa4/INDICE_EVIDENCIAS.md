# Índice de evidencias PA4

Organizado el 22/11/2026 a partir de archivos existentes. No se ejecutaron pruebas, servicios ni consultas a bases de datos. Las evidencias son históricas: no certifican una ejecución nueva del código actual.

## Resultados Maven encontrados

| Proyecto | Último log exitoso localizado en target | Finalización registrada | Resultado |
|---|---|---|---|
| Sesión 5, sistema-pedidos-springboot | maven-pa4-test-verificacion.log | 2026-11-22 06:49:29 -05:00 | BUILD SUCCESS; 44 ejecutadas, 44 aprobadas, 0 fallos, errores y omitidas |
| Sesión 12, pedidos-reportes-service | maven-pa4-reportes-test-20261122-123921.log | 2026-11-22 12:40:01 -05:00 | BUILD SUCCESS; 11 ejecutadas, 11 aprobadas, 0 fallos, errores y omitidas |

- [Principal: extracto Maven](principal/maven-resultados.txt) y [casos individuales](principal/casos.csv).
- [Reportes: extracto Maven](reportes/maven-resultados.txt) y [casos individuales](reportes/casos.csv).
- Las carpetas `principal/surefire` y `reportes/surefire` contienen copias literales de los 13 resúmenes TXT por clase, revisados antes de copiar.
- [Manifiesto SHA256](copias-sha256.csv): procedencia y hash de cada copia TXT. Cada extracto Maven también identifica el original y su hash.
- Los extractos contienen únicamente líneas originales de resultados y una cabecera de procedencia. No son copias completas de los logs. Los CSV se derivaron de los elementos testcase de los XML existentes; no incluyen propiedades de entorno, system-out ni system-err. No se copiaron XML completos ni configuraciones con credenciales.
- El intento fallido `maven-pa4-reportes-test-20261122-122816.log` no demuestra pruebas ejecutadas y no se incluyó como evidencia de aprobación.

## Archivos fuente de las pruebas

Rutas relativas a la raíz del repositorio; S5 y S12 están dentro de `ADVANCED-BUSINESS-APPLICATION-DEVELOPMENT`.

| Clave | Archivo exacto |
|---|---|
| Servicio | `Sesión 5/sistema-pedidos-springboot/src/test/java/com/isil/edu/pe/spedidos/service/PedidoRevisionServiceIntegrationTest.java` |
| Supervisor MVC | `Sesión 5/sistema-pedidos-springboot/src/test/java/com/isil/edu/pe/spedidos/controller/PedidoSupervisorControllerIntegrationTest.java` |
| Cliente HTTP | `Sesión 5/sistema-pedidos-springboot/src/test/java/com/isil/edu/pe/spedidos/client/reportes/PedidoReporteRestClientTest.java` |
| Reporte MVC | `Sesión 5/sistema-pedidos-springboot/src/test/java/com/isil/edu/pe/spedidos/controller/PedidoReporteWebControllerTest.java` |
| REST revisiones | `Sesión 12/pedidos-reportes-service/src/test/java/com/isil/edu/pe/pedidos/reportes/controller/PedidoRevisionControllerTest.java` |

## Casos y assertions comprobadas

Todos los casos de esta tabla figuran aprobados en los resultados existentes. La ubicación del resultado individual es `principal/casos.csv` para S5 y `reportes/casos.csv` para S12; los TXT de `surefire` llevan el nombre completo de cada clase.

| Caso | Archivo (clave anterior) y método exacto | Assertions / alcance de la evidencia |
|---|---|---|
| Commit de revisión | Servicio: `confirmaAmbasEscrituras` | Antes revisado=false; después true, una revisión, observación recortada, fecha no nula, sin pendientes y una revisión listada. La clase no envuelve el test en @Transactional: comprueba el estado después del retorno del servicio en H2. |
| Resultado MVC correcto | Supervisor MVC: `confirmaYRedirige` | Redirección a /supervisor, mensaje flash de éxito y revisado=true en H2. |
| Duplicado | Servicio: `rechazaRevisionDuplicadaSinSobrescribir` | Segunda revisión lanza PedidoRevisionException; queda una sola fila y la observación inicial. |
| Rechazo HTTP controlado | Supervisor MVC: `duplicadoDevuelve409` | Segundo POST con CSRF devuelve HTTP 409. |
| ID inválido | Servicio: `rechazaIdInvalido` | ID 0 lanza PedidoRevisionException y no registra revisiones. No afirma el motivo específico de la excepción. |
| Observación inválida | Servicio: `rechazaObservacionesInvalidas` | Null, vacía, espacios y 501 caracteres lanzan excepción; revisado=false y cero revisiones. |
| Validación MVC | Supervisor MVC: `entradaInvalidaConservaFormulario` | Observación de espacios devuelve 400, vista supervisor-pedidos y atributos error/pedidoFallido. |
| Pedido inexistente | Servicio: `rechazaPedidoInexistente` | ID 99 produce motivo NO_ENCONTRADO y cero revisiones. |
| Pedido inexistente HTTP | Supervisor MVC: `pedidoInexistenteDevuelve404` | POST con ID 99 devuelve 404. |
| Rollback de segunda escritura | Servicio: `rollbackRealCuandoFallaInsertDespuesDelUpdate` | Prepara una fila incompatible en H2; el INSERT viola UNIQUE después del UPDATE. Hay excepción, revisado vuelve a false, queda una revisión y conserva observación Anterior. Es rollback SQL real en H2, no evidencia de MySQL. |
| Concurrencia | Servicio: `dosSolicitudesConcurrentesSoloConfirmanUnaRevision` | Dos solicitudes simultáneas: resultados diferentes, perdedora con YA_REVISADO, revisado=true y una sola revisión. |
| Reportes no disponible: transporte | Cliente HTTP: `convierteErrorDeComunicacionSinContactar8083` | MockRestServiceServer provoca IOException en /estadisticas/resumen; se obtiene PedidoReporteNoDisponibleException con causa y se verifica la solicitud esperada. No apaga ni contacta 8083. |
| Reportes no disponible: interfaz | Reporte MVC: `muestra503CuandoReportesNoEstaDisponible` | Cliente simulado falla; GET /reportes/pedidos devuelve 503, vista error y atributo error. No demuestra por sí solo la recuperación de la JSP del supervisor. |
| GET /api/v1/pedidos/revisiones | REST revisiones: `listaRevisionesPorEstado` | Dos invocaciones: PENDIENTE y REVISADO. HTTP 200, JSON, una fila, ID, cliente, revisado y observación; verifica delegación exacta y ausencia de otras llamadas. |
| Normalización de estado | REST revisiones: `normalizaEstadoAntesDeDelegar` | Entrada con espacios y minúsculas se delega como REVISADO; 200 y JSON vacío. |
| Estado inválido | REST revisiones: `rechazaEstadoInvalidoSinConsultarServicio` | Cuatro invocaciones: vacío, espacios, TODOS y REVISADOS. HTTP 400, JSON con mensaje exacto y ninguna interacción con servicio. |
| Estado ausente | REST revisiones: `exigeParametroEstado` | HTTP 400 y ninguna interacción con servicio. |
| GET /api/v1/pedidos/estadisticas/revisiones | REST revisiones: `devuelveEstadisticasDeRevision` | HTTP 200 y JSON con total=4, revisados=1, pendientes=3, porcentaje=25; delegación única al servicio. |
| Estadísticas vacías | REST revisiones: `devuelveEstadisticasSinPedidos` | HTTP 200, los cuatro valores JSON en cero y delegación única al servicio. |

Las diez invocaciones REST usan MockMvc y Mockito. La undécima prueba S12 es `PedidosReportesServiceApplicationTests.contextLoads`, con perfil H2. No se atribuyen las otras pruebas históricas de PA1/PA2/PA3 a funcionalidades nuevas de PA4.

## Evidencias todavía pendientes

El usuario confirma siete capturas reales E01–E07, que se incorporarán al informe por separado. No se inspeccionaron sus imágenes ni se asignan aquí a casos específicos. Las respuestas REST reales obtenidas en esta revisión se describen abajo.

| Evidencia | Estado y límite |
|---|---|
| Pantalla JSP de supervisión antes/después, observación y confirmación | E01–E07 disponibles por separado según el usuario; pendiente vincular la captura correspondiente. MockMvc no renderiza visualmente la JSP. |
| Mensajes visibles de validación y duplicado | E01–E07 disponibles por separado según el usuario; pendiente vincularlas con estos casos. Existen assertions automatizadas 400/404/409. |
| Respuestas desplegadas para PENDIENTE, REVISADO y estadísticas | DISPONIBLE: tres GET reales HTTP 200 con JSON válido en rest-real. No son respuestas MockMvc. |
| Integridad y rollback en MySQL/InnoDB | PENDIENTE si el informe exige demostración en ese motor. H2 ya aporta evidencia transaccional; no se debe provocar un fallo sobre pedidos reales para obtener capturas. |
| Interfaz del supervisor cuando reportes no responde | CAPTURADA según confirmación del usuario; se incorporará por separado. Imagen no inspeccionada aquí; pendiente asociar su identificador al informe. |
| Rúbrica oficial de entrega | No localizada en los archivos revisados. No se puede afirmar que todas estas capturas sean obligatorias; contrastar esta lista con la rúbrica. |

## Respuestas REST reales y disponibilidad

Capturadas el 22/11/2026 mediante GET sin credenciales, cookies ni redirecciones. Cada archivo registra fecha con zona -05:00, URL, código HTTP, Content-Type y cuerpo JSON recibido. No contiene cabeceras de autenticación ni Set-Cookie. Los cuerpos contienen pedidos de validación PA4 con nombres genéricos de prueba. Se revisaron antes de su publicación: no se detectaron credenciales, cookies ni datos personales sensibles.

El login real `http://localhost:8081/sistema-pedidos/login` respondió HTTP 200. Esto comprueba disponibilidad HTTP, no una sesión autenticada del supervisor. El puerto 8083 respondió a los tres endpoints siguientes:

| Evidencia | Resultado real | Archivo |
|---|---|---|
| GET /api/v1/pedidos/revisiones?estado=PENDIENTE | HTTP 200, JSON válido, 1 registro | [pendiente.txt](rest-real/pendiente.txt) |
| GET /api/v1/pedidos/revisiones?estado=REVISADO | HTTP 200, JSON válido, 1 registro | [revisado.txt](rest-real/revisado.txt) |
| GET /api/v1/pedidos/estadisticas/revisiones | HTTP 200; totalPedidos=2, totalRevisados=1, totalPendientes=1, porcentajeRevisado=50.00 | [estadisticas.txt](rest-real/estadisticas.txt) |

Base real utilizada: `http://localhost:8083/pedidos-reportes-service`. Estas respuestas acreditan consultas al servicio desplegado, a diferencia de las diez invocaciones MockMvc/Mockito. No se inspeccionó la conexión del proceso ni se ejecutaron consultas directas a MySQL; estos GET no constituyen una demostración de commit/rollback en ese motor.

## Evidencia de contingencia

El usuario confirmó que la captura real del mensaje de contingencia ya existe y se incorporará al informe por separado. No se inspeccionó esa imagen ni se asignó un número de evidencia.

Mensaje esperado según la vista: «El servicio de reportes no está disponible temporalmente. Puede continuar revisando pedidos.» Las pruebas automatizadas de cliente HTTP y pantalla de reportes siguen siendo evidencia distinta de esa captura visual.

En la comprobación posterior, estadísticas volvió a responder HTTP 200 con totalPedidos=2, totalRevisados=1, totalPendientes=1 y porcentajeRevisado=50.00. Reportes ya estaba activo: no fue necesario reiniciarlo. Esa verificación no equivale a comprobar visualmente la recuperación de la sesión del supervisor; no se guardó una nueva captura HTTP de ella.

Pendiente: vincular la captura de contingencia confirmada al informe. Una captura visual posterior con estadísticas disponibles no fue inspeccionada ni confirmada aquí.

## Cierre documental y alcance

La [guía principal](../README.md) y el [README de Sesión 5](<../ADVANCED-BUSINESS-APPLICATION-DEVELOPMENT/Sesión 5/sistema-pedidos-springboot/README.md>) enlazan este índice. Se documentaron las ejecuciones independientes de ambas suites, perfil H2, resultados 44/11, repositorio Maven local, requisitos, arranque, variables, esquema, roles, rutas y ejemplos JSON expresamente ilustrativos.

La consigna oficial no se encontró en el repositorio revisado. Queda pendiente contrastar los requisitos con ese documento; no se declara cumplimiento total de una rúbrica no disponible.

No se modificó código funcional ni se repitieron pruebas durante la preparación documental. La ejecución de 44 pruebas precede a la limpieza final de configuración/estilo: no representa una revalidación del commit publicado posteriormente. Los resultados de H2 y Mockito no acreditan comprobaciones reales en MySQL ni respuestas del servicio desplegado.
