-- PA4, fase 1. Script manual para MySQL; ejecutar UNA SOLA VEZ.
-- No se ejecuta automaticamente desde Spring Boot.
-- Esquema esperado segun Pedido.java: pedido, PK id Long / IDENTITY
-- (BIGINT con signo AUTO_INCREMENT). No se ha inspeccionado la BD real.
--
-- VERIFICACIONES PREVIAS OBLIGATORIAS (ejecutar por separado):
-- 1. Obtener respaldo y detener escrituras de ambas aplicaciones.
--    Aplicar antes de arrancar la entidad modificada: ddl-auto=update
--    podria crear revisado sin el DEFAULT requerido por este script.
-- 2. Seleccionar explicitamente la BD correcta (pedidosdb por defecto;
--    DB_URL puede sobrescribirla). Este archivo no cambia de base.
--    SELECT DATABASE();
--    SHOW CREATE TABLE pedido;
--    SHOW COLUMNS FROM pedido;
--    SHOW TABLES;
-- 3. Confirmar que pedido usa InnoDB, que id es la PRIMARY KEY y es
--    BIGINT con signo AUTO_INCREMENT, que revisado NO existe y que
--    pedido_revision NO existe. Si algo difiere, DETENERSE y adaptar
--    el script tras revisar el esquema; no borrar ni recrear tablas.
--    Confirmar permisos ALTER, CREATE y REFERENCES y nombres de
--    restricciones disponibles: uk_pedido_revision_pedido_id y
--    fk_pedido_revision_pedido.
-- 4. Registrar SELECT COUNT(*) FROM pedido; para comparar al finalizar.
--
-- MySQL confirma DDL implicitamente: estas dos sentencias NO forman
-- una transaccion atomica. Usar un cliente que se detenga ante errores,
-- sin opcion --force. Si falla una sentencia, inspeccionar el esquema
-- y completar solo lo pendiente; no volver a ejecutar todo el archivo.
-- No se usa IF NOT EXISTS para no ocultar una aplicacion previa/parcial.

ALTER TABLE pedido
    ADD COLUMN revisado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE pedido_revision (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT NOT NULL,
    observacion VARCHAR(500) NOT NULL,
    fecha_revision DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_pedido_revision_pedido_id UNIQUE (pedido_id),
    CONSTRAINT fk_pedido_revision_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedido (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;

-- VERIFICACIONES POSTERIORES (ejecutar por separado):
-- SHOW CREATE TABLE pedido;
-- SHOW CREATE TABLE pedido_revision;
-- SELECT COUNT(*) FROM pedido; -- debe coincidir con el conteo previo
-- SELECT COUNT(*) FROM pedido WHERE revisado IS NULL OR revisado <> FALSE;
-- Debe devolver 0: ADD COLUMN asigna FALSE a todos los pedidos existentes.
-- SELECT COUNT(*) FROM pedido_revision; -- debe devolver 0
-- La fecha de revision debera proporcionarse al insertar en una fase posterior.
