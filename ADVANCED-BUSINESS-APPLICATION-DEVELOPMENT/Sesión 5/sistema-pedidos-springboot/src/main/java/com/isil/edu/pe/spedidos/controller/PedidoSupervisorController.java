package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteClient;
import com.isil.edu.pe.spedidos.client.reportes.PedidoReporteNoDisponibleException;
import com.isil.edu.pe.spedidos.exception.PedidoRevisionException;
import com.isil.edu.pe.spedidos.service.PedidoRevisionService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/supervisor")
public class PedidoSupervisorController {
  private static final Logger log = LoggerFactory.getLogger(PedidoSupervisorController.class);
  private final PedidoRevisionService service;
  private final PedidoReporteClient reporteClient;
  public PedidoSupervisorController(
      PedidoRevisionService service,
      PedidoReporteClient reporteClient) {
    this.service = service;
    this.reporteClient = reporteClient;
  }

  @GetMapping({"", "/pedidos"})
  public String listar(Model model) {
    model.addAttribute("pendientes", service.listarPendientes());
    model.addAttribute("revisiones", service.listarRevisiones());

    try {
      model.addAttribute("pendientesReporte", reporteClient.listarRevisiones("PENDIENTE"));
      model.addAttribute("revisadosReporte", reporteClient.listarRevisiones("REVISADO"));
      model.addAttribute("estadisticasRevisiones", reporteClient.obtenerEstadisticasRevisiones());
      model.addAttribute("reportesDisponibles", true);
    } catch (PedidoReporteNoDisponibleException e) {
      log.warn("Servicio de reportes no disponible: {}", e.getClass().getSimpleName());
      model.addAttribute("reportesDisponibles", false);
    }
    return "supervisor-pedidos";
  }

  @PostMapping("/pedidos/{id}/revision")
  public String revisar(@PathVariable long id,
      @RequestParam(defaultValue = "") String observacion, Model model,
      RedirectAttributes redirect, HttpServletResponse response) {
    try {
      service.revisar(id, observacion);
      redirect.addFlashAttribute("exito", "Pedido #" + id + " revisado correctamente.");
      return "redirect:/supervisor";
    } catch (PedidoRevisionException e) {
      response.setStatus(switch (e.getMotivo()) {
        case INVALIDO -> 400;
        case NO_ENCONTRADO -> 404;
        case YA_REVISADO -> 409;
      });
      model.addAttribute("error", e.getMessage());
      model.addAttribute("pedidoFallido", id);
      model.addAttribute("observacionIngresada", observacion);
      return listar(model);
    }
  }

  @ExceptionHandler(DataAccessException.class)
  public String errorPersistencia(DataAccessException e, Model model, HttpServletResponse response) {
    // No exponer SQL, datos del formulario ni detalles de conexión al navegador.
    log.error("No se pudo completar la operación de supervisión ({})", e.getClass().getSimpleName());
    response.setStatus(503);
    model.addAttribute("error", "No se pudo completar la operación. Intente nuevamente.");
    return "error";
  }
}
