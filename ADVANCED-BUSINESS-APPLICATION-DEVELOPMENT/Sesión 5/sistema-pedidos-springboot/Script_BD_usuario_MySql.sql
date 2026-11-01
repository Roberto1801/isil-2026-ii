/*
  PA3 - Plantilla de instalación inicial, NO ejecutada.
  Destino: pedidosdb en MariaDB 10.4 / MySQL 8.
  Fuente: Pedido.java y src/main/resources/application.properties.

  ANTES DE EJECUTAR:
  1. Confirmar que pedidosdb y todas las cuentas spedidos_app siguen sin existir.
  2. Trabajar sobre una copia privada FUERA del repositorio.
  3. Reemplazar <CONTRASENA_LOCAL_ENTRE_COMILLAS_SQL> por un literal SQL
     entre comillas simples con la contraseña local elegida. Escapar una
     comilla simple interna duplicándola. No guardar secretos en este archivo.
     El marcador SIN comillas es deliberadamente SQL inválido: no puede crear
     una cuenta con una contraseña pública si se olvida reemplazarlo.
  4. Ejecutar con un administrador local, sentencia por sentencia, deteniéndose
     ante cualquier error. No utilizar --force ni continuar tras un error.
     Si aparece un objeto existente, detenerse y revisar; no reutilizarlo.
     El DDL y los permisos no se revierten como una transacción: una ejecución
     parcial requiere diagnóstico antes de repetirla.

  ALCANCE:
  - Solo crea pedidosdb, pedidosdb.pedido y spedidos_app@localhost.
  - Sin datos iniciales ni cambios a cuentas o bases existentes.
  - Sin permisos globales, privilegios delegables ni acceso remoto con %.

  COMPATIBILIDAD:
  - InnoDB, utf8mb4_unicode_ci, AUTO_INCREMENT, DECIMAL y DATETIME(6).
  - DATETIME(6) conserva fecha/hora local, sin conversión de zona por la columna.
    La entidad asigna fecha; no se añade un valor predeterminado en la base.
  - No utiliza collations exclusivas de MySQL 8 ni plugins de autenticación
    específicos de ese motor. IDENTIFIED BY usa el mecanismo del servidor.
  - La compatibilidad del DDL no certifica la del runtime: el proyecto conserva
    MySQL Connector/J y Hibernate. MariaDB 10.4 está por debajo del mínimo 10.6
    del dialecto MariaDB de Hibernate ORM 7.4; verificar arranque y operaciones.
    Referencia: https://docs.hibernate.org/orm/7.4/dialect/
  - H2 genera su esquema desde JPA en el perfil test: no ejecutar este script
    allí. El test optativo MySQL usa una tabla temporal independiente con
    fecha TIMESTAMP; comprueba JDBC, no valida el esquema JPA persistente.

  DESPUÉS, CUANDO SE AUTORICE:
  - Configurar DB_URL apuntando a localhost:3306/pedidosdb,
    DB_USERNAME=spedidos_app y DB_PASSWORD con la contraseña privada.
    DB_PASSWORD debe reemplazar el valor demo predeterminado del proyecto.
  - Conectar primero como spedidos_app y comprobar SELECT DATABASE(),
    CURRENT_USER(), SHOW GRANTS y SHOW CREATE TABLE pedidosdb.pedido.
  - No es necesario FLUSH PRIVILEGES al usar CREATE USER y GRANT.
*/

CREATE DATABASE `pedidosdb`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE TABLE `pedidosdb`.`pedido` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `cliente` VARCHAR(120) NOT NULL,
  `producto_codigo` VARCHAR(30) NOT NULL,
  `producto_nombre` VARCHAR(120) NOT NULL,
  `precio_unitario` DECIMAL(12, 2) NOT NULL,
  `cantidad` INT NOT NULL,
  `total` DECIMAL(12, 2) NOT NULL,
  `fecha` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'spedidos_app'@'localhost'
  IDENTIFIED BY <CONTRASENA_LOCAL_ENTRE_COMILLAS_SQL>;

-- CRUD de JPA y lecturas Spring JDBC.
-- CREATE, ALTER e INDEX: mantenimiento de esquema con ddl-auto=update.
-- CREATE TEMPORARY TABLES: exclusivamente para el test optativo de PA3.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX,
      CREATE TEMPORARY TABLES
  ON `pedidosdb`.* TO 'spedidos_app'@'localhost';
