package com.isil.edu.pe.pedidos.reportes.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.isil.edu.pe.pedidos.reportes.service.PedidoReporteService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PedidoRevisionControllerTest {

  private PedidoReporteService service;
  private MockMvc mvc;

  @BeforeEach
  void preparar() {
    service = mock(PedidoReporteService.class);
    mvc = MockMvcBuilders.standaloneSetup(new PedidoReporteController(service)).build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"PENDIENTE", "REVISADO"})
  void listaRevisionesPorEstado(String estado) throws Exception {
    boolean revisado = estado.equals("REVISADO");
    when(service.listarRevisiones(estado)).thenReturn(List.of(Map.of(
        "pedidoId", 7L, "cliente", "Cliente de prueba",
        "revisado", revisado, "observacion", revisado ? "Conforme" : "")));

    mvc.perform(get("/api/v1/pedidos/revisiones").param("estado", estado))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].pedidoId").value(7))
        .andExpect(jsonPath("$[0].cliente").value("Cliente de prueba"))
        .andExpect(jsonPath("$[0].revisado").value(revisado))
        .andExpect(jsonPath("$[0].observacion").value(revisado ? "Conforme" : ""));

    verify(service).listarRevisiones(estado);
    verifyNoMoreInteractions(service);
  }

  @Test
  void normalizaEstadoAntesDeDelegar() throws Exception {
    when(service.listarRevisiones("REVISADO")).thenReturn(List.of());

    mvc.perform(get("/api/v1/pedidos/revisiones").param("estado", " revisado "))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    verify(service).listarRevisiones("REVISADO");
    verifyNoMoreInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "TODOS", "REVISADOS"})
  void rechazaEstadoInvalidoSinConsultarServicio(String estado) throws Exception {
    mvc.perform(get("/api/v1/pedidos/revisiones").param("estado", estado))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Estado invalido. Use PENDIENTE o REVISADO."));

    verifyNoInteractions(service);
  }

  @Test
  void exigeParametroEstado() throws Exception {
    mvc.perform(get("/api/v1/pedidos/revisiones"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void devuelveEstadisticasDeRevision() throws Exception {
    when(service.obtenerEstadisticasRevisiones()).thenReturn(Map.of(
        "totalPedidos", 4L, "totalRevisados", 1L,
        "totalPendientes", 3L, "porcentajeRevisado", new BigDecimal("25.00")));

    mvc.perform(get("/api/v1/pedidos/estadisticas/revisiones"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPedidos").value(4))
        .andExpect(jsonPath("$.totalRevisados").value(1))
        .andExpect(jsonPath("$.totalPendientes").value(3))
        .andExpect(jsonPath("$.porcentajeRevisado").value(25.0));

    verify(service).obtenerEstadisticasRevisiones();
    verifyNoMoreInteractions(service);
  }

  @Test
  void devuelveEstadisticasSinPedidos() throws Exception {
    when(service.obtenerEstadisticasRevisiones()).thenReturn(Map.of(
        "totalPedidos", 0L, "totalRevisados", 0L,
        "totalPendientes", 0L, "porcentajeRevisado", BigDecimal.ZERO));

    mvc.perform(get("/api/v1/pedidos/estadisticas/revisiones"))
        .andExpect(status().isOk())
        .andExpect(content().json("""
            {"totalPedidos":0,"totalRevisados":0,"totalPendientes":0,"porcentajeRevisado":0}
            """));

    verify(service).obtenerEstadisticasRevisiones();
    verifyNoMoreInteractions(service);
  }
}
