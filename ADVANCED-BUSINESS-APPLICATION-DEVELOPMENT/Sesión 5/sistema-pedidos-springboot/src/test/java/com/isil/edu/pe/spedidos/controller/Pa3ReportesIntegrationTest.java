package com.isil.edu.pe.spedidos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import com.isil.edu.pe.spedidos.exception.ReporteValidacionException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Pa3ReportesIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired PedidoReporteJdbcRepository repository;
  @Autowired PedidoReporteService service;
  MockMvc mvc;

  @BeforeEach
  void preparar() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM pedido");
    insertar("Ana Torres", 2, "100.00");
    insertar("Carlos Perez", 1, "50.00");
    insertar("Ana Torres", 4, "200.00");
  }

  void insertar(String cliente, int cantidad, String total) {
    jdbc.update("""
        INSERT INTO pedido (cliente, producto_codigo, producto_nombre, precio_unitario,
          cantidad, total, fecha) VALUES (?, 'P001', 'Producto', 50.00, ?, ?, CURRENT_TIMESTAMP)
        """, cliente, cantidad, new BigDecimal(total));
  }

  @Test
  void busquedaJdbcCombinaClienteYMontoIncluyeLimiteYOrdenaDescendente() {
    // Arrange
    insertar("Ana Torres", 6, "300.00");
    // Act
    var pedidos = repository.buscarPedidosPorClienteYTotalMinimo(" aNA ", new BigDecimal("200"));
    // Assert
    assertEquals(2, pedidos.size());
    assertEquals(0, new BigDecimal("300").compareTo(pedidos.get(0).getTotal()));
    assertTrue(pedidos.get(0).getId() > pedidos.get(1).getId());
    assertEquals("Ana Torres", pedidos.get(1).getCliente());
  }

  @Test
  void clientesJdbcAplicaHavingYCalculaCuatroIndicadores() {
    // Arrange: Ana tiene dos pedidos, Carlos uno.
    // Act
    var clientes = repository.listarClientesFrecuentes(2);
    // Assert
    assertEquals(1, clientes.size());
    var ana = clientes.getFirst();
    assertEquals("Ana Torres", ana.cliente());
    assertEquals(2, ana.totalPedidos());
    assertEquals(6, ana.totalUnidades());
    assertEquals(0, new BigDecimal("300").compareTo(ana.importeAcumulado()));
    assertEquals(0, new BigDecimal("150").compareTo(ana.ticketPromedio()));
    assertTrue(repository.listarClientesFrecuentes(3).isEmpty());
  }

  @Test
  void servicioRechazaValoresInvalidos() {
    // Arrange / Act / Assert: validación centralizada, también para consumidores no HTTP.
    assertThrows(ReporteValidacionException.class, () -> service.listarClientesFrecuentes(0));
    assertThrows(ReporteValidacionException.class, () -> service.listarClientesFrecuentes(null));
    assertThrows(ReporteValidacionException.class,
        () -> service.buscarPedidosPorClienteYTotalMinimo(" ", BigDecimal.ZERO));
    assertThrows(ReporteValidacionException.class,
        () -> service.buscarPedidosPorClienteYTotalMinimo("Ana", new BigDecimal("-1")));
    assertThrows(ReporteValidacionException.class,
        () -> service.buscarPedidosPorClienteYTotalMinimo("Ana", null));
  }

  @Test
  void consultaAccedeJspConResumenYResultados() throws Exception {
    // Arrange / Act
    var respuesta = mvc.perform(get("/reportes/pedidos").param("cliente", "Ana")
        .param("totalMinimo", "200").with(user("consulta").roles("CONSULTA")));
    // Assert
    respuesta.andExpect(status().isOk()).andExpect(view().name("reportes-pedidos"))
        .andExpect(forwardedUrl("/WEB-INF/views/reportes-pedidos.jsp"))
        .andExpect(model().attributeExists("resumen", "pedidos", "clientes"));
  }

  @Test
  void anonimoEsRedirigidoAlLoginDeReportes() throws Exception {
    // Arrange / Act / Assert
    mvc.perform(get("/reportes/pedidos")).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
    mvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "2"))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void consultaObtieneJsonDeAmbasConsultas() throws Exception {
    // Arrange / Act / Assert
    mvc.perform(get("/api/v1/pedidos/busqueda-avanzada").param("cliente", "Ana")
        .param("totalMinimo", "200").with(user("consulta").roles("CONSULTA")))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].total").value(200));
    mvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "2")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isOk())
        .andExpect(jsonPath("$[0].totalPedidos").value(2))
        .andExpect(jsonPath("$[0].totalUnidades").value(6))
        .andExpect(jsonPath("$[0].importeAcumulado").value(300))
        .andExpect(jsonPath("$[0].ticketPromedio").value(150));
  }

  @Test
  void parametrosInvalidosRestDevuelven400ConMensaje() throws Exception {
    // Arrange
    String ruta = "/api/v1/pedidos/busqueda-avanzada";
    // Act / Assert
    mvc.perform(get(ruta).param("cliente", "").param("totalMinimo", "0")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("El cliente es obligatorio."));
    mvc.perform(get(ruta).param("cliente", "Ana").param("totalMinimo", "-1")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "0")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }

  @Test
  void formatosInvalidosYParametrosAusentesSonControlados() throws Exception {
    // Arrange / Act / Assert
    mvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "abc")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
    mvc.perform(get("/api/v1/pedidos/busqueda-avanzada")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
    mvc.perform(get("/reportes/pedidos").param("totalMinimo", "abc")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest())
        .andExpect(view().name("reportes-pedidos")).andExpect(model().attributeExists("error"));
  }

  @Test
  void jspRechazaUmbralInvalidoYClienteVacio() throws Exception {
    // Arrange / Act / Assert
    mvc.perform(get("/reportes/pedidos").param("minPedidos", "0")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest())
        .andExpect(model().attributeExists("error"));
    mvc.perform(get("/reportes/pedidos").param("cliente", " ").param("totalMinimo", "0")
        .with(user("consulta").roles("CONSULTA"))).andExpect(status().isBadRequest())
        .andExpect(model().attributeExists("error"));
  }

  @Test
  void rolAjenoNoAccedeReportesAunqueEsteAutenticado() throws Exception {
    // Arrange / Act / Assert
    mvc.perform(get("/reportes/pedidos").with(user("otro").roles("OTRO")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/pedidos/clientes-frecuentes").param("minPedidos", "2")
        .with(user("otro").roles("OTRO"))).andExpect(status().isForbidden());
  }

  @Test
  void operadorAccedeReportesYCsrfInvalidoSigueBloqueandoRegistro() throws Exception {
    // Arrange / Act / Assert
    mvc.perform(get("/reportes/pedidos").with(user("operador").roles("OPERADOR")))
        .andExpect(status().isOk());
    mvc.perform(post("/pedidos").with(user("operador").roles("OPERADOR"))
        .with(csrf().useInvalidToken()).param("cliente", "Ana")
        .param("productoCodigo", "P001").param("cantidad", "1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void parametrosSqlNoSeInterpretanComoInyeccion() {
    // Arrange / Act
    var pedidos = repository.buscarPedidosPorClienteYTotalMinimo("' OR 1=1 --", BigDecimal.ZERO);
    // Assert
    assertTrue(pedidos.isEmpty());
  }
}
