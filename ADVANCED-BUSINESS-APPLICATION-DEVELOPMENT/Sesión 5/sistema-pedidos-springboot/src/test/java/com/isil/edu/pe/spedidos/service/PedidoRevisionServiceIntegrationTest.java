package com.isil.edu.pe.spedidos.service;

import static org.junit.jupiter.api.Assertions.*;
import com.isil.edu.pe.spedidos.exception.PedidoRevisionException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.util.concurrent.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:supervisor_service;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql("/supervisor-schema.sql")
class PedidoRevisionServiceIntegrationTest {
  @Autowired PedidoRevisionService service;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach void preparar() {
    jdbc.update("""
        INSERT INTO pedido (id,cliente,producto_codigo,producto_nombre,precio_unitario,cantidad,total,fecha,revisado)
        VALUES (1,'Ana','P1','Producto',10,2,20,CURRENT_TIMESTAMP,FALSE)
        """);
  }
  boolean revisado() {
    return jdbc.queryForObject("SELECT revisado FROM pedido WHERE id=1", Boolean.class);
  }
  int revisiones() { return jdbc.queryForObject("SELECT COUNT(*) FROM pedido_revision", Integer.class); }

  @Test void confirmaAmbasEscrituras() {
    assertFalse(revisado());
    service.revisar(1, "  Conforme  ");
    assertTrue(revisado());
    assertEquals(1, revisiones());
    assertEquals("Conforme", jdbc.queryForObject("SELECT observacion FROM pedido_revision", String.class));
    assertNotNull(jdbc.queryForObject("SELECT fecha_revision FROM pedido_revision", java.sql.Timestamp.class));
    assertTrue(service.listarPendientes().isEmpty());
    assertEquals(1, service.listarRevisiones().size());
  }
  @Test void rechazaPedidoInexistente() {
    var e = assertThrows(PedidoRevisionException.class, () -> service.revisar(99, "Conforme"));
    assertEquals(PedidoRevisionException.Motivo.NO_ENCONTRADO, e.getMotivo());
    assertEquals(0, revisiones());
  }
  @Test void rechazaRevisionDuplicadaSinSobrescribir() {
    service.revisar(1, "Primera");
    assertThrows(PedidoRevisionException.class, () -> service.revisar(1, "Segunda"));
    assertEquals(1, revisiones());
    assertEquals("Primera", jdbc.queryForObject("SELECT observacion FROM pedido_revision", String.class));
  }
  @Test void rollbackRealCuandoFallaInsertDespuesDelUpdate() {
    // Inconsistencia deliberada SOLO en H2: el INSERT chocará con UNIQUE.
    // El UPDATE previo es real y debe revertirse al salir del proxy transaccional.
    jdbc.update("INSERT INTO pedido_revision(pedido_id,observacion,fecha_revision) VALUES(1,'Anterior',CURRENT_TIMESTAMP)");
    assertThrows(PedidoRevisionException.class, () -> service.revisar(1, "Nueva"));
    assertFalse(revisado());
    assertEquals(1, revisiones());
    assertEquals("Anterior", jdbc.queryForObject("SELECT observacion FROM pedido_revision", String.class));
  }
  @Test void rechazaObservacionesInvalidas() {
    for (String observacion : new String[] {null, "", "  ", "x".repeat(501)}) {
      assertThrows(PedidoRevisionException.class, () -> service.revisar(1, observacion));
    }
    assertFalse(revisado());
    assertEquals(0, revisiones());
  }
  @Test void aceptaLimiteDe500Caracteres() {
    service.revisar(1, "x".repeat(500));
    assertTrue(revisado());
  }
  @Test void rechazaIdInvalido() {
    assertThrows(PedidoRevisionException.class, () -> service.revisar(0, "Conforme"));
    assertEquals(0, revisiones());
  }
  @Test void dosSolicitudesConcurrentesSoloConfirmanUnaRevision() throws Exception {
    var inicio = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Callable<Boolean> revisar = () -> {
        inicio.await();
        try { service.revisar(1, "Concurrente"); return true; }
        catch (PedidoRevisionException e) {
          assertEquals(PedidoRevisionException.Motivo.YA_REVISADO, e.getMotivo());
          return false;
        }
      };
      var a = executor.submit(revisar);
      var b = executor.submit(revisar);
      inicio.countDown();
      assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    }
    assertTrue(revisado());
    assertEquals(1, revisiones());
  }
}
