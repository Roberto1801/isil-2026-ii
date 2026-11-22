package com.isil.edu.pe.spedidos.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.springframework.context.annotation.Import;
import com.isil.edu.pe.pedidos.reportes.controller.PedidoReporteController;
import com.isil.edu.pe.pedidos.reportes.service.impl.PedidoReporteServiceImpl;
import com.isil.edu.pe.pedidos.reportes.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.pedidos.reportes.repository.mapper.PedidoRowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({PedidoReporteController.class, PedidoReporteServiceImpl.class, com.isil.edu.pe.pedidos.reportes.repository.PedidoRevisionReporteRepository.class,
    PedidoReporteJdbcRepository.class, PedidoRowMapper.class})
class PedidoReporteControllerIntegrationTest {

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();

    jdbcTemplate.update("DELETE FROM pedido");
    insertarPedido("Ana Torres", "LAP-001", "Laptop", "2500.00", 2, "5000.00");
    insertarPedido("Carlos Perez", "MOU-001", "Mouse", "50.00", 3, "150.00");
  }

  @Test
  @DisplayName("Return plain JSON and HTTP 200 when CONSULTA requests level 2 summary")
  void returnPlainJsonAndHttp200WhenConsultaRequestsLevelTwoSummary() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/estadisticas/resumen")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPedidos").value(2))
        .andExpect(jsonPath("$._links").doesNotExist());
  }

  @Test
  @DisplayName("Return plain JSON array when level 1 resource is filtered")
  void returnPlainJsonArrayWhenLevelOneResourceIsFiltered() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos")
            .param("cliente", "Ana")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0]").exists())
        .andExpect(jsonPath("$._links").doesNotExist());
  }

  @Test
  @DisplayName("Return current JSON product sales contract from extracted report service")
  void returnJsonProductSalesCollectionWhenReportIsRequested() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/estadisticas/productos")
            .with(user("operador").roles("OPERADOR")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].productoCodigo").value("MOU-001"))
        .andExpect(jsonPath("$[0].totalUnidades").value(3))
        .andExpect(jsonPath("$[1].productoCodigo").value("LAP-001"));
  }

  @Test
  @DisplayName("Reject legacy search URI: current report API expects a numeric ID")
  void rejectLegacyActionStyleSearchUri() throws Exception {
    mockMvc.perform(get("/api/v1/pedidos/buscar")
            .param("cliente", "Ana")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isBadRequest());
  }

  private void insertarPedido(
      String cliente,
      String productoCodigo,
      String productoNombre,
      String precioUnitario,
      int cantidad,
      String total) {

    jdbcTemplate.update(
        """
        INSERT INTO pedido (
          cliente,
          producto_codigo,
          producto_nombre,
          precio_unitario,
          cantidad,
          total,
          fecha, revisado
        ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, FALSE)
        """,
        cliente,
        productoCodigo,
        productoNombre,
        new BigDecimal(precioUnitario),
        cantidad,
        new BigDecimal(total)
    );
  }
}

