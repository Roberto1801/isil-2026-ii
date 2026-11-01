package com.isil.edu.pe.spedidos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.isil.edu.pe.spedidos.client.InventarioClient;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:pa3_render;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class Pa3JspHttpIntegrationTest {
  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean InventarioClient inventario;
  @Value("${demo.security.operador-password}") String password;

  @Test
  void jspRenderizaReportesYRegistroDevuelve503ConInventarioNoDisponible() throws Exception {
    // Arrange: base H2 aislada, HTTP real y fallo simulado del adaptador externo.
    String cliente = "PA3 <Ana>";
    jdbc.update("""
        INSERT INTO pedido (cliente, producto_codigo, producto_nombre, precio_unitario,
          cantidad, total, fecha) VALUES (?, 'P001', 'Producto', 50, 2, 100, CURRENT_TIMESTAMP)
        """, cliente);
    jdbc.update("""
        INSERT INTO pedido (cliente, producto_codigo, producto_nombre, precio_unitario,
          cantidad, total, fecha) VALUES (?, 'P001', 'Producto', 50, 4, 200, CURRENT_TIMESTAMP)
        """, cliente);
    when(inventario.reservarStock(anyString(), anyInt()))
        .thenThrow(new InventarioNoDisponibleException("Inventario no disponible (simulado)."));
    var http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    String root = "http://localhost:" + port + "/sistema-pedidos";
    var login = http.send(HttpRequest.newBuilder(URI.create(root + "/login")).GET().build(),
        HttpResponse.BodyHandlers.ofString());
    String token = token(login.body());
    var auth = http.send(HttpRequest.newBuilder(URI.create(root + "/login"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("username=operador&password=" + encode(password)
            + "&_csrf=" + encode(token))).build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(302, auth.statusCode());

    // Act: Jasper procesa la JSP; este test no se limita al forward de MockMvc.
    var reporte = http.send(HttpRequest.newBuilder(URI.create(root
        + "/reportes/pedidos?cliente=PA3&totalMinimo=200&minPedidos=2")).GET().build(),
        HttpResponse.BodyHandlers.ofString());
    // Assert
    assertEquals(200, reporte.statusCode(), reporte.body());
    assertTrue(reporte.body().contains("PA3 &lt;Ana&gt;"), reporte.body());
    assertFalse(reporte.body().contains("PA3 <Ana>"));
    assertTrue(reporte.body().contains("200.00"), reporte.body());
    assertTrue(reporte.body().contains("300.00"), reporte.body());
    verifyNoInteractions(inventario);

    // Act: registro autenticado con token válido y dependencia caída.
    var registro = http.send(HttpRequest.newBuilder(URI.create(root + "/pedidos"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("cliente=Ana&productoCodigo=P001&cantidad=1&_csrf="
            + encode(token(reporte.body())))).build(), HttpResponse.BodyHandlers.ofString());
    // Assert: error controlado, ningún pedido adicional; los reportes siguen disponibles.
    assertEquals(503, registro.statusCode(), registro.body());
    assertTrue(registro.body().contains("Inventario no disponible"));
    assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class));
    var despues = http.send(HttpRequest.newBuilder(URI.create(root + "/reportes/pedidos")).GET().build(),
        HttpResponse.BodyHandlers.ofString());
    assertEquals(200, despues.statusCode());
    verify(inventario).reservarStock("P001", 1);
    verifyNoMoreInteractions(inventario);
  }

  private String token(String html) {
    var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
    assertTrue(matcher.find(), "La página debe incluir el token CSRF");
    return matcher.group(1);
  }

  private String encode(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }
}
