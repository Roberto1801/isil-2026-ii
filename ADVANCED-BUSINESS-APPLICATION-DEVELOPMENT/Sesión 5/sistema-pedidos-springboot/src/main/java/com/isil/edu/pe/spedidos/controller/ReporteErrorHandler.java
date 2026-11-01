package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.exception.ReporteValidacionException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice(assignableTypes = PedidoReporteController.class)
class ReporteRestErrorHandler {
  @ExceptionHandler(ReporteValidacionException.class)
  @ResponseBody
  ResponseEntity<Map<String, String>> invalidos(ReporteValidacionException e) {
    return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
  }

  @ExceptionHandler({MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class})
  @ResponseBody
  ResponseEntity<Map<String, String>> formato(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("error",
        "Verifique los parámetros requeridos: cliente, totalMinimo numérico y minPedidos entero."));
  }
}

@ControllerAdvice(assignableTypes = PedidoReporteWebController.class)
class ReporteWebErrorHandler {
  @ExceptionHandler({MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class})
  String formato(Exception e, Model model, HttpServletResponse response) {
    response.setStatus(400);
    model.addAttribute("error", "Formato inválido: totalMinimo debe ser numérico y minPedidos entero.");
    return "reportes-pedidos";
  }
}
