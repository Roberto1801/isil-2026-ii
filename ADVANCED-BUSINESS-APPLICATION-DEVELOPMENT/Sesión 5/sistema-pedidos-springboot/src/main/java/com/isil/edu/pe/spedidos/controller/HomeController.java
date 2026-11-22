package com.isil.edu.pe.spedidos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  @GetMapping("/")
  public String inicio(jakarta.servlet.http.HttpServletRequest request) {
    if (request.isUserInRole("SUPERVISOR")) {
      return "redirect:/supervisor";
    }
    return "redirect:/pedidos";
  }

}
