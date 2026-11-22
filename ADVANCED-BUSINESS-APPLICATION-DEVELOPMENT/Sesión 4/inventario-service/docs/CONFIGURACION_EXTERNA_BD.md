# Configuración externa de SQL Server

## Objetivo

La aplicación **no contiene** host, puerto, base de datos, usuario ni contraseña en el código Java ni en `persistence.xml`.

El flujo es:

```text
config/database.properties
        |
        v
WildFly Maven Plugin
        |
        v
WildFly + add-on mssqlserver
        |
        v
DataSource JNDI: java:jboss/datasources/InventarioDS
        |
        v
Jakarta Persistence / @PersistenceContext
```

## 1. Archivo externo

Copiar config/database.example.properties a config/database.properties (excluido de Git) y editar los datos no secretos:

```text
config/database.properties
```

Ejemplo:

```properties
org.wildfly.datasources.mssqlserver.connection-url=jdbc:sqlserver://localhost:1433;databaseName=InventarioDB;encrypt=true;trustServerCertificate=true
org.wildfly.datasources.mssqlserver.user-name=inventario_app
org.wildfly.datasources.mssqlserver.jndi-name=java:jboss/datasources/InventarioDS
org.wildfly.datasources.mssqlserver.min-pool-size=2
org.wildfly.datasources.mssqlserver.max-pool-size=10
org.wildfly.datasources.mssqlserver.jta=true
```

> La clave se entrega mediante MSSQLSERVER_PASSWORD. El iniciador PA4 la solicita con entrada oculta; no agregarla al archivo ni a comandos guardados. Para una instalación nueva, crear primero el login inventario_app desde el diálogo de SSMS con una clave privada.

## 2. `RestApplication`

Ya no contiene `@DataSourceDefinition`. Solo activa Jakarta REST:

```java
@ApplicationPath("/api")
public class RestApplication extends Application {
}
```

## 3. `persistence.xml`

Jakarta Persistence solo conoce el nombre JNDI:

```xml
<jta-data-source>java:jboss/datasources/InventarioDS</jta-data-source>
```

No contiene URL, usuario ni contraseña.

## 4. `pom.xml`

El `wildfly-maven-plugin` provisiona el add-on oficial `mssqlserver` y carga el archivo externo mediante `propertiesFile`.

```xml
<discover-provisioning-info>
    <version>${wildfly.version}</version>
    <addOns>
        <addOn>mssqlserver</addOn>
    </addOns>
</discover-provisioning-info>

<propertiesFile>${project.basedir}/config/database.properties</propertiesFile>
```

## 5. Ejecutar

```bash
mvn clean test
mvn wildfly:run
```

El servicio queda en:

```text
http://localhost:8082/inventario-service/
```

## 6. Por qué esta solución es preferible

- El WAR no contiene credenciales.
- `@PersistenceContext` continúa usando un DataSource JTA administrado por WildFly.
- No se crea manualmente `EntityManagerFactory`.
- Se conserva la separación entre configuración de infraestructura y código de negocio.
- Para cambiar de ambiente solo se reemplaza el archivo de propiedades.
