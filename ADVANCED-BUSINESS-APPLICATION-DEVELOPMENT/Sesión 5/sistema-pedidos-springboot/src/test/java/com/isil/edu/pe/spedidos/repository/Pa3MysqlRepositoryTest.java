package com.isil.edu.pe.spedidos.repository;

import static org.junit.jupiter.api.Assertions.*;
import com.isil.edu.pe.spedidos.repository.mapper.PedidoRowMapper;
import java.math.BigDecimal;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Comprobación optativa contra MySQL, sin modificar la tabla persistente pedido. */
@EnabledIfSystemProperty(named = "pa3.mysql", matches = "true")
class Pa3MysqlRepositoryTest {
  @Test
  void consultasFuncionanEnMysqlConTablaTemporalDeSesion() throws Exception {
    // Arrange: las variables DB_* se resuelven igual que en application.properties.
    var properties = new Properties();
    try (var in = getClass().getResourceAsStream("/application.properties")) {
      properties.load(in);
    }
    var env = new StandardEnvironment();
    env.getPropertySources().addLast(new PropertiesPropertySource("app", properties));
    var dataSource = new SingleConnectionDataSource(
        env.getRequiredProperty("spring.datasource.url"),
        env.getRequiredProperty("spring.datasource.username"),
        env.getRequiredProperty("spring.datasource.password"), true);
    try {
      var jdbc = new JdbcTemplate(dataSource);
      // MySQL oculta la tabla persistente homónima solo en esta conexión.
      // La tabla temporal desaparece automáticamente al cerrar la conexión.
      jdbc.execute("""
          CREATE TEMPORARY TABLE pedido (
            id BIGINT AUTO_INCREMENT PRIMARY KEY, cliente VARCHAR(120) NOT NULL,
            producto_codigo VARCHAR(30) NOT NULL, producto_nombre VARCHAR(120) NOT NULL,
            precio_unitario DECIMAL(12,2) NOT NULL, cantidad INT NOT NULL,
            total DECIMAL(12,2) NOT NULL, fecha TIMESTAMP NOT NULL)
          """);
      insertar(jdbc, "Ana Torres", 2, "100.00");
      insertar(jdbc, "Carlos Perez", 1, "50.00");
      insertar(jdbc, "Ana Torres", 4, "200.00");
      var repository = new PedidoReporteJdbcRepository(jdbc,
          new NamedParameterJdbcTemplate(jdbc), new PedidoRowMapper());
      // Act
      var pedidos = repository.buscarPedidosPorClienteYTotalMinimo("ana", new BigDecimal("200"));
      var clientes = repository.listarClientesFrecuentes(2);
      // Assert: misma consulta y RowMapper que en H2.
      assertEquals(1, pedidos.size());
      assertEquals(0, new BigDecimal("200").compareTo(pedidos.getFirst().getTotal()));
      assertNotNull(pedidos.getFirst().getFecha());
      assertEquals(1, clientes.size());
      assertEquals(2, clientes.getFirst().totalPedidos());
      assertEquals(6, clientes.getFirst().totalUnidades());
      assertEquals(0, new BigDecimal("300").compareTo(clientes.getFirst().importeAcumulado()));
      assertEquals(0, new BigDecimal("150").compareTo(clientes.getFirst().ticketPromedio()));
    } finally {
      dataSource.destroy();
    }
  }

  private void insertar(JdbcTemplate jdbc, String cliente, int cantidad, String total) {
    jdbc.update("""
        INSERT INTO pedido (cliente, producto_codigo, producto_nombre, precio_unitario,
          cantidad, total, fecha) VALUES (?, 'P001', 'Producto', 50, ?, ?, CURRENT_TIMESTAMP)
        """, cliente, cantidad, new BigDecimal(total));
  }
}
