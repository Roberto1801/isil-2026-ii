CREATE TABLE IF NOT EXISTS pedido_revision (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 pedido_id BIGINT NOT NULL UNIQUE,
 observacion VARCHAR(500) NOT NULL,
 fecha_revision DATETIME(6) NOT NULL,
 FOREIGN KEY (pedido_id) REFERENCES pedido(id)
);
DELETE FROM pedido_revision;
DELETE FROM pedido;
