package com.isil.edu.pe.spedidos.repository;

import com.isil.edu.pe.spedidos.entity.Pedido;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRevisionJdbcRepository {
  private final JdbcTemplate jdbc;
  public PedidoRevisionJdbcRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  // El bloqueo se mantiene hasta commit/rollback del servicio.
  public Optional<Boolean> bloquearEstado(long id) {
    return jdbc.query("SELECT revisado FROM pedido WHERE id = ? FOR UPDATE",
        (rs, row) -> rs.getBoolean("revisado"), id).stream().findFirst();
  }

  public int marcarRevisado(long id) {
    return jdbc.update("UPDATE pedido SET revisado = TRUE WHERE id = ? AND revisado = FALSE", id);
  }

  public int registrarRevision(long id, String observacion) {
    return jdbc.update("""
        INSERT INTO pedido_revision (pedido_id, observacion, fecha_revision)
        VALUES (?, ?, CURRENT_TIMESTAMP)
        """, id, observacion);
  }

  public List<Pedido> listarPendientes() {
    return jdbc.query("SELECT * FROM pedido WHERE revisado = FALSE ORDER BY id DESC", (rs, row) -> {
      Pedido pedido = new Pedido(rs.getLong("id"), rs.getString("cliente"),
          rs.getString("producto_codigo"), rs.getString("producto_nombre"),
          rs.getBigDecimal("precio_unitario"), rs.getInt("cantidad"),
          rs.getBigDecimal("total"), rs.getTimestamp("fecha").toLocalDateTime());
      pedido.setRevisado(rs.getBoolean("revisado"));
      return pedido;
    });
  }

  public List<Map<String, Object>> listarRevisiones() {
    return jdbc.queryForList("""
        SELECT p.id, p.cliente, p.producto_nombre, r.observacion, r.fecha_revision
        FROM pedido p JOIN pedido_revision r ON r.pedido_id = p.id
        WHERE p.revisado = TRUE ORDER BY r.fecha_revision DESC, r.id DESC
        """);
  }
}
