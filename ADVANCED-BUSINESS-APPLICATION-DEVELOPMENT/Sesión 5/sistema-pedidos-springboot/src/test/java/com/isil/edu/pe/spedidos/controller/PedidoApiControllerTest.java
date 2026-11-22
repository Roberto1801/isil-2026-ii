package com.isil.edu.pe.spedidos.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.isil.edu.pe.pedidos.reportes.service.domain.Pedido;
import com.isil.edu.pe.pedidos.reportes.controller.PedidoApiController;
import com.isil.edu.pe.pedidos.reportes.service.PedidoReporteService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PedidoApiControllerTest {

  private static final String ORDERS_PATH = "/api/v1/orders";

  private static final String SUMMARY_PATH = ORDERS_PATH + "/resumen";

  private PedidoReporteService pedidoService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
      pedidoService = mock(PedidoReporteService.class);
      mockMvc = MockMvcBuilders
          .standaloneSetup(new PedidoApiController(pedidoService))
          .build();
  }

  @Test
  @DisplayName("Return empty JSON array from PedidoApiController when no orders exist")
  void returnEmptyJsonArrayFromPedidoApiControllerWhenNoOrdersExist()
      throws Exception {
    // Arrange: definir la única respuesta del colaborador necesaria.
    when(pedidoService.listarTodos()).thenReturn(List.of());

    // Act: ejercer el contrato HTTP del SUT.
    ResultActions response = mockMvc.perform(get(ORDERS_PATH));

    // Assert: resultado observable, no detalles internos de implementación.
    response
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  @DisplayName("Return zero order count from PedidoApiController when no orders exist")
  void returnZeroOrderCountFromPedidoApiControllerWhenNoOrdersExist()
      throws Exception {
    // Arrange
    when(pedidoService.listarTodos()).thenReturn(List.of());

    // Act
    ResultActions response = mockMvc.perform(get(SUMMARY_PATH));

    // Assert
    response
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.modulo").value("Sistema de Pedidos"))
        .andExpect(jsonPath("$.totalPedidos").value(0));
  }

  @Test
  @DisplayName("Return order count of two from PedidoApiController when two orders exist")
  void returnOrderCountOfTwoFromPedidoApiControllerWhenTwoOrdersExist()
      throws Exception {
    // Arrange: datos mínimos para comprobar el conteo.
    List<Pedido> orders = List.of(pedido(0), pedido(0));
    when(pedidoService.listarTodos()).thenReturn(orders);

    // Act
    ResultActions response = mockMvc.perform(get(SUMMARY_PATH));

    // Assert
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPedidos").value(2));
  }

  @Test
  @DisplayName("Return total units of five from PedidoApiController when quantities are two and three")
  void returnTotalUnitsOfFiveFromPedidoApiControllerWhenQuantitiesAreTwoAndThree()
      throws Exception {

    // Arrange
    Pedido first = pedido(2);


    Pedido second = pedido(3);


    when(pedidoService.listarTodos())
        .thenReturn(List.of(first, second));

    // Act
    ResultActions response = mockMvc.perform(
        get("/api/v1/orders/resumen")
    );

    // Assert
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPedidos").value(2))
        .andExpect(jsonPath("$.totalUnidades").value(5));
  }

  private Pedido pedido(int cantidad) { return new Pedido(1L, "Ana", "P1", "Producto", java.math.BigDecimal.ONE, cantidad, java.math.BigDecimal.ONE, java.time.LocalDateTime.now()); }
}
