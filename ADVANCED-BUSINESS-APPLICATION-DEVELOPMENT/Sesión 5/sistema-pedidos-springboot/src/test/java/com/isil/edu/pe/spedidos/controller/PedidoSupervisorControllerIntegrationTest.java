package com.isil.edu.pe.spedidos.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:supervisor_mvc;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Sql("/supervisor-schema.sql")
class PedidoSupervisorControllerIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  MockMvc mvc;
  @BeforeEach void preparar() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("""
        INSERT INTO pedido (id,cliente,producto_codigo,producto_nombre,precio_unitario,cantidad,total,fecha,revisado)
        VALUES (1,'Ana','P1','Producto',10,2,20,CURRENT_TIMESTAMP,FALSE)
        """);
  }
  @Test void supervisorVePendientes() throws Exception {
    mvc.perform(get("/supervisor").with(user("supervisor").roles("SUPERVISOR")))
        .andExpect(status().isOk()).andExpect(view().name("supervisor-pedidos"))
        .andExpect(model().attributeExists("pendientes", "revisiones"));
  }
  @Test void confirmaYRedirige() throws Exception {
    mvc.perform(post("/supervisor/pedidos/1/revision").with(user("supervisor").roles("SUPERVISOR"))
        .with(csrf()).param("observacion","Correcto"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/supervisor"))
        .andExpect(flash().attributeExists("exito"));
    assertTrue(jdbc.queryForObject("SELECT revisado FROM pedido WHERE id=1", Boolean.class));
  }
  @Test void sinCsrfNoEscribe() throws Exception {
    mvc.perform(post("/supervisor/pedidos/1/revision").with(user("supervisor").roles("SUPERVISOR"))
        .param("observacion","Correcto")).andExpect(status().isForbidden());
    assertFalse(jdbc.queryForObject("SELECT revisado FROM pedido WHERE id=1", Boolean.class));
  }
  @Test void otrosRolesNoPuedenRevisar() throws Exception {
    for (String rol : new String[]{"OPERADOR","CONSULTA"}) {
      mvc.perform(get("/supervisor").with(user("otro").roles(rol))).andExpect(status().isForbidden());
      mvc.perform(post("/supervisor/pedidos/1/revision").with(user("otro").roles(rol))
          .with(csrf()).param("observacion","Correcto")).andExpect(status().isForbidden());
    }
  }
  @Test void anonimoNecesitaLogin() throws Exception {
    mvc.perform(get("/supervisor")).andExpect(status().is3xxRedirection());
  }
  @Test void entradaInvalidaConservaFormulario() throws Exception {
    mvc.perform(post("/supervisor/pedidos/1/revision").with(user("supervisor").roles("SUPERVISOR"))
        .with(csrf()).param("observacion"," "))
        .andExpect(status().isBadRequest()).andExpect(view().name("supervisor-pedidos"))
        .andExpect(model().attributeExists("error", "pedidoFallido"));
  }
  @Test void pedidoInexistenteDevuelve404() throws Exception {
    mvc.perform(post("/supervisor/pedidos/99/revision").with(user("supervisor").roles("SUPERVISOR"))
        .with(csrf()).param("observacion","Correcto")).andExpect(status().isNotFound());
  }
  @Test void duplicadoDevuelve409() throws Exception {
    var usuario = user("supervisor").roles("SUPERVISOR");
    mvc.perform(post("/supervisor/pedidos/1/revision").with(usuario).with(csrf()).param("observacion","Uno"));
    mvc.perform(post("/supervisor/pedidos/1/revision").with(usuario).with(csrf()).param("observacion","Dos"))
        .andExpect(status().isConflict());
  }
  @Test void inicioDirigeSupervisorASuModulo() throws Exception {
    mvc.perform(get("/").with(user("supervisor").roles("SUPERVISOR")))
        .andExpect(redirectedUrl("/supervisor"));
  }
}
