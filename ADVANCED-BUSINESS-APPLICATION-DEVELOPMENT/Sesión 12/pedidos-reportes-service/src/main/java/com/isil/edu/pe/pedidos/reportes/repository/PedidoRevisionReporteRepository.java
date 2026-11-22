package com.isil.edu.pe.pedidos.reportes.repository;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRevisionReporteRepository {

    private final JdbcTemplate jdbc;

    public PedidoRevisionReporteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarPorEstado(String estado) {
        String sql = """
            SELECT
                p.id AS pedidoId,
                p.cliente,
                p.producto_nombre AS productoNombre,
                p.cantidad,
                p.total,
                p.revisado,
                r.observacion,
                r.fecha_revision AS fechaRevision
            FROM pedido p
            LEFT JOIN pedido_revision r ON r.pedido_id = p.id
            WHERE p.revisado = ?
            ORDER BY p.id DESC
            """;

        boolean revisado = "REVISADO".equals(estado);
        return jdbc.queryForList(sql, revisado);
    }

    public Map<String, Object> obtenerEstadisticas() {
        String sql = """
            SELECT
                COUNT(*) AS totalPedidos,
                COALESCE(SUM(CASE WHEN revisado = TRUE THEN 1 ELSE 0 END), 0)
                    AS totalRevisados,
                COALESCE(SUM(CASE WHEN revisado = FALSE THEN 1 ELSE 0 END), 0)
                    AS totalPendientes,
                COALESCE(
                    ROUND(
                        100.0 * SUM(CASE WHEN revisado = TRUE THEN 1 ELSE 0 END)
                        / NULLIF(COUNT(*), 0),
                        2
                    ),
                    0
                ) AS porcentajeRevisado
            FROM pedido
            """;

        return jdbc.queryForMap(sql);
    }
}
