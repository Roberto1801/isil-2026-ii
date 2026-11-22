package com.isil.edu.pe.spedidos.client.reportes;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PedidoReporteRestClientTest {
  @Test void convierteErrorDeComunicacionSinContactar8083() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("http://reportes.test/api/v1/pedidos/estadisticas/resumen"))
        .andRespond(withException(new IOException("Conexión rechazada")));
    var client = new PedidoReporteRestClient(builder.build(), "http://reportes.test/api/v1/pedidos");
    var error = assertThrows(PedidoReporteNoDisponibleException.class, client::obtenerResumen);
    assertNotNull(error.getCause());
    server.verify();
  }
}
