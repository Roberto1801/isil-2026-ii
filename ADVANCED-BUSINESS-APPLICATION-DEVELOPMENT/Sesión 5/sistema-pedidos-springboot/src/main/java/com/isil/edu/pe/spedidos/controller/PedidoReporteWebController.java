package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.exception.ReporteValidacionException;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import java.math.BigDecimal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PedidoReporteWebController {
  private final PedidoReporteService service;

  public PedidoReporteWebController(PedidoReporteService service) {
    this.service = service;
  }

  @GetMapping("/reportes/pedidos")
  public String reportes(@RequestParam(required = false) String cliente,
      @RequestParam(required = false) BigDecimal totalMinimo,
      @RequestParam(defaultValue = "2") Integer minPedidos,
      Model model, HttpServletResponse response) {
    model.addAttribute("resumen", service.obtenerResumen());
    model.addAttribute("cliente", cliente);
    model.addAttribute("totalMinimo", totalMinimo);
    model.addAttribute("minPedidos", minPedidos);
    try {
      model.addAttribute("clientes", service.listarClientesFrecuentes(minPedidos));
      if (cliente != null || totalMinimo != null) {
        model.addAttribute("pedidos", service.buscarPedidosPorClienteYTotalMinimo(cliente, totalMinimo));
        model.addAttribute("busquedaRealizada", true);
      }
    } catch (ReporteValidacionException e) {
      response.setStatus(400);
      model.addAttribute("error", e.getMessage());
    }
    return "reportes-pedidos";
  }
}
