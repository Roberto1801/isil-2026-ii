package com.isil.edu.pe.spedidos.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteClient;
import com.isil.edu.pe.spedidos.client.reportes.dto.ResumenPedidosResponse;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PedidoReporteWebControllerTest {
  @MockitoBean
  private PedidoReporteClient client;

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext context;

  @BeforeEach
  void setUp() {
    when(client.obtenerResumen()).thenReturn(new ResumenPedidosResponse(
        0, 0, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));
    when(client.buscarPorCliente("Ana")).thenReturn(java.util.List.of());
    when(client.listarProductosMasVendidos()).thenReturn(java.util.List.of());
    mockMvc = MockMvcBuilders.webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  @DisplayName("Render report view with expected model for CONSULTA user")
  void renderReportViewWithExpectedModelForConsultaUser() throws Exception {
    mockMvc.perform(get("/reportes/pedidos")
            .param("cliente", "Ana")
            .with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk())
        .andExpect(view().name("reportes-pedidos"))
        .andExpect(model().attributeExists("resumen", "productos", "pedidos", "cliente"));
  }

  @Test
  void muestra503CuandoReportesNoEstaDisponible() throws Exception {
    when(client.listarProductosMasVendidos()).thenThrow(
        new com.isil.edu.pe.spedidos.client.reportes.PedidoReporteNoDisponibleException(
            "Reportes no disponible", new java.io.IOException("Conexión rechazada")));
    mockMvc.perform(get("/reportes/pedidos").with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(view().name("error"))
        .andExpect(model().attributeExists("error"));
  }

}
