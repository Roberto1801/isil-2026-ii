package com.isil.edu.pe.pedidos.reportes.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import com.isil.edu.pe.pedidos.reportes.controller.dto.PedidoResponse;
import com.isil.edu.pe.pedidos.reportes.controller.dto.ProductoVendidoDto;
import com.isil.edu.pe.pedidos.reportes.controller.dto.ResumenPedidosDto;
import com.isil.edu.pe.pedidos.reportes.service.PedidoReporteService;
import java.util.List;
import java.util.Map;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para reportes de pedidos.
 * Proporciona endpoints para listar pedidos por cliente, obtener resumen de pedidos
 * y listar productos mÃ¡s vendidos con enlaces HAL.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoReporteController {

  private final PedidoReporteService reporteService;

  public PedidoReporteController(PedidoReporteService reporteService) {
    this.reporteService = reporteService;
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public PedidoResponse obtenerPorId(@PathVariable long id) {
    return PedidoResponse.from(reporteService.obtenerPorId(id));
  }

  /**
   * RMM 1: coleccion identificada mediante URI (JSON sin controles hipermedia).
   * La utilizacion de GET es necesaria en Spring MVC, pero no demuestra
   * por si sola el uso integral de metodos y codigos HTTP del nivel 2.
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public List<PedidoResponse> listarPedidosPorCliente(@RequestParam String cliente) {
    return reporteService.buscarPorCliente(cliente)
        .stream()
        .map(PedidoResponse::from)
        .toList();
  }

  /**
   * RMM 2: recurso de resumen con representacion JSON, GET seguro y
   * estado HTTP explicito 200 OK, sin controles hipermedia.
   */
  @GetMapping(value = "/estadisticas/resumen", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResumenPedidosDto> obtenerResumen() {
    return ResponseEntity.ok(ResumenPedidosDto.from(reporteService.obtenerResumen()));
  }

  @GetMapping(
      value = "/estadisticas/productos",
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public List<ProductoVendidoDto> listarProductosMasVendidos() {

    return reporteService.listarProductosMasVendidos()
        .stream()
        .map(ProductoVendidoDto::from)
        .toList();
  }


  @GetMapping(value = "/revisiones", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> listarRevisiones(@RequestParam String estado) {
    String estadoNormalizado = estado.trim().toUpperCase(java.util.Locale.ROOT);

    if (!estadoNormalizado.equals("PENDIENTE")
        && !estadoNormalizado.equals("REVISADO")) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "Estado invalido. Use PENDIENTE o REVISADO."));
    }

    return ResponseEntity.ok(reporteService.listarRevisiones(estadoNormalizado));
  }

  @GetMapping(value = "/estadisticas/revisiones",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> estadisticasRevisiones() {
    return ResponseEntity.ok(reporteService.obtenerEstadisticasRevisiones());
  }
}

