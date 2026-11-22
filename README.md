# PA4 — Desarrollo de Aplicaciones Empresariales Avanzado

Entrega individual en la rama `feature/pa4-inca-villagomez`.

- [Índice de evidencias y casos verificados](evidencias-pa4/INDICE_EVIDENCIAS.md).
- [Documentación del proyecto principal](<ADVANCED-BUSINESS-APPLICATION-DEVELOPMENT/Sesión 5/sistema-pedidos-springboot/README.md>).

Esta guía se basa en el código y los resultados existentes. La consigna oficial no está disponible en el repositorio revisado; queda pendiente contrastar la entrega con ese documento.

## Requisitos y arranque

Java 21, Maven 3.9+ en PATH (o wrapper disponible), PowerShell 5.1+, MySQL en 3307 y SQL Server en 1433. Conservar las rutas de sesiones 4, 5 y 12: las pruebas del principal incorporan fuentes de reportes desde su ubicación relativa.

Para la ejecución funcional se necesitan `pedidosdb`, `InventarioDB`, usuarios con permisos correspondientes y WildFly previamente provisionado con inventario desplegado en `Sesión 4/inventario-service/target/server`. El iniciador no provisiona ni redespliega WildFly. Copiar la plantilla `config/database.example.properties` a `config/database.properties` en inventario y configurar solo URL, usuario y JNDI; el archivo local está excluido de Git.

Orden: bases de datos disponibles → WildFly/inventario 8082 → reportes 8083 → principal 8081. Desde la raíz:

```powershell
.\estado-pa4.ps1
.\iniciar-pa4.ps1
```

El iniciador verifica puertos y respuestas HTTP, solicita claves mediante entrada oculta y conserva registros locales en `.pa4-runtime`. Admite `-MavenPath` y `-WildFlyHome` para ubicaciones alternativas y `-Servicios WildFly` para limitar el arranque. No detiene instancias existentes ni ejecuta migraciones. Los servicios deben incorporar el código actual para una comprobación manual válida.

## Variables y roles

No hay claves de producción predeterminadas. No escribirlas en comandos guardados ni archivos publicables. El iniciador configura las variables por proceso; fuera de él deben proporcionarse en el entorno.

| Servicio | Variables |
|---|---|
| Principal y reportes | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, con credenciales independientes |
| Principal: acceso web | `PEDIDOS_OPERADOR_PASSWORD`, `PEDIDOS_CONSULTA_PASSWORD`, `SUPERVISOR_PASSWORD` |
| WildFly | `MSSQLSERVER_URL`, `MSSQLSERVER_USER`, `MSSQLSERVER_JNDI`, `MSSQLSERVER_PASSWORD` |

El principal conserva MySQL 3306 como valor predeterminado: el iniciador lo sustituye por 3307. Reportes usa el usuario de lectura `spedidos_reportes`; requiere SELECT sobre `pedido` y `pedido_revision`.

Los usuarios en memoria son `operador` (OPERADOR), `consulta` (CONSULTA) y `supervisor` (SUPERVISOR). Operador registra pedidos; consulta accede a consultas; `/supervisor` y `/supervisor/**` requieren SUPERVISOR. Los formularios POST conservan CSRF. El servicio REST separado de reportes no incorpora la autenticación del principal: su exposición debe limitarse al entorno previsto.

## Esquema y transacción

[Script manual de fase 1](<ADVANCED-BUSINESS-APPLICATION-DEVELOPMENT/Sesión 5/sistema-pedidos-springboot/database/01_pa4_revision_pedidos.sql>): incorpora `pedido.revisado BOOLEAN NOT NULL DEFAULT FALSE` y `pedido_revision` con ID BIGINT, pedido_id BIGINT único y FK, observacion VARCHAR(500), fecha_revision DATETIME(6), motor InnoDB. Conserva los pedidos anteriores con revisado=false.

El esquema ya fue aplicado en el entorno de trabajo. No repetirlo. En una instalación nueva, preparar primero el esquema base y realizar las verificaciones del script: base seleccionada, motor, tipos de claves, ausencia de objetos nuevos y respaldo. MySQL confirma DDL implícitamente; las sentencias no son una migración atómica.

La revisión JDBC usa bloqueo de fila y `@Transactional`: validación, UPDATE e INSERT en una misma transacción, con UNIQUE como protección adicional. No llama servicios remotos dentro de esa transacción. La observación es obligatoria y admite hasta 500 caracteres.

El iniciador desactiva inicialización SQL/migraciones y usa validación de esquema. Un arranque manual del principal conserva `ddl-auto=update` de su configuración base: no es equivalente al arranque protegido del script.

## Pantallas y API

- Principal: `http://localhost:8081/sistema-pedidos/pedidos`.
- Supervisión: `http://localhost:8081/sistema-pedidos/supervisor`.
- Reportes MVC: `http://localhost:8081/sistema-pedidos/reportes/pedidos`.
- POST de revisión: `/sistema-pedidos/supervisor/pedidos/{id}/revision`, formulario con observacion y CSRF. Éxito redirige; entrada inválida 400, inexistente 404, duplicado 409.
- GET `http://localhost:8083/pedidos-reportes-service/api/v1/pedidos/revisiones?estado=PENDIENTE` (también REVISADO).
- GET `http://localhost:8083/pedidos-reportes-service/api/v1/pedidos/estadisticas/revisiones`.

Ejemplos ilustrativos del contrato, no capturas ni resultados obtenidos del servidor. El tipo JSON de revisado puede depender del mapeo JDBC/MySQL:

```json
[{"pedidoId":7,"cliente":"Cliente de ejemplo","productoNombre":"Producto","cantidad":2,"total":20,"revisado":false,"observacion":null,"fechaRevision":null}]
```

```json
{"totalPedidos":4,"totalRevisados":1,"totalPendientes":3,"porcentajeRevisado":25.00}
```

Estado no admitido devuelve 400 con `{"error":"Estado invalido. Use PENDIENTE o REVISADO."}`; estado ausente también devuelve 400. Las pruebas comprueban JSON y delegación mediante MockMvc/Mockito, no consultas reales a 8083.

## Pruebas Maven

Comandos para futuras ejecuciones desde la raíz del repositorio; no se ejecutaron durante este cierre documental. Ambas suites usan H2 en memoria para sus pruebas de contexto/persistencia; las pruebas REST aisladas simulan el servicio.

Sesión 5:

```powershell
Push-Location 'ADVANCED-BUSINESS-APPLICATION-DEVELOPMENT/Sesión 5/sistema-pedidos-springboot'
cmd /d /c "mvn -B -Dmaven.repo.local=%USERPROFILE%\.m2\repository test > maven-test.log 2>&1"
$LASTEXITCODE
Pop-Location
```

Sesión 12:

```powershell
Push-Location 'ADVANCED-BUSINESS-APPLICATION-DEVELOPMENT/Sesión 12/pedidos-reportes-service'
cmd /d /c "mvn -B -Dmaven.repo.local=%USERPROFILE%\.m2\repository test > maven-test.log 2>&1"
$LASTEXITCODE
Pop-Location
```

Si la ruta del perfil contiene espacios, usar comillas escapadas de PowerShell en el argumento:

```powershell
cmd /d /c "mvn -B `"-Dmaven.repo.local=%USERPROFILE%\.m2\repository`" test > maven-test.log 2>&1"
```

Puede sustituirse `mvn` por `.\mvnw.cmd` si el wrapper está disponible. `cmd` redirige stdout y stderr al mismo registro: las advertencias Mockito no se interpretan como errores PowerShell. Comprobar el código de salida, el resumen Maven y `target/surefire-reports`. Para empaquetar después de verificar, el ciclo Maven es `mvn verify` desde cada proyecto; no usar el arranque de aplicaciones para ejecutar pruebas.

`-Dmaven.repo.local` evita el problema detectado cuando Java informa `user.home=C:\`: Maven intentaba escribir en `C:\.m2\repository`. Se elige el repositorio del usuario solo para esa ejecución, sin cambiar settings globales. En invocación directa PowerShell, la sintaxis equivalente es `"-Dmaven.repo.local=$env:USERPROFILE\.m2\repository"`.

| Ejecución confirmada del 22/11/2026 | Aprobadas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| Sesión 5 — BUILD SUCCESS, 06:49:29 -05:00 | 44 | 0 | 0 | 0 |
| Sesión 12 — BUILD SUCCESS, 12:40:01 -05:00 | 11 | 0 | 0 | 0 |

Sesión 12 incluye diez casos REST y una carga de contexto H2. Sesión 5 incluye pruebas históricas de funcionalidades anteriores además de las nuevas de PA4; 44 no significa 44 pruebas nuevas.

## Limitaciones y evidencias pendientes

Los resultados son históricos. La ejecución del principal precede a la limpieza final de configuración/estilo; no certifica una revalidación posterior. Las pruebas de rollback comprueban escrituras reales en H2, no MySQL/InnoDB. Las pruebas REST comprueban controladores con servicio simulado, no el SQL de reportes. MockMvc no verifica la renderización visual de JSP.

Faltan capturas reales de supervisión y validaciones, respuestas desplegadas de los endpoints y evidencia específica de la interfaz del supervisor con reportes indisponible. La evidencia MySQL queda pendiente si la consigna exige ese motor. Consultar el índice para el detalle, sin provocar fallos sobre datos reales. No publicar credenciales ni registros completos del entorno; secretos retirados pueden persistir en el historial Git.
