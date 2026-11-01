package com.isil.edu.pe.spedidos.service;

import java.math.BigDecimal;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;


import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.service.domain.ProductoVendido;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;
import java.util.List;

public interface PedidoReporteService {

  List<Pedido> buscarPedidosPorClienteYTotalMinimo(String cliente, BigDecimal totalMinimo);

  List<ClienteFrecuente> listarClientesFrecuentes(Integer minPedidos);

  ResumenPedidos obtenerResumen();

  List<Pedido> buscarPorCliente(String cliente);

  List<ProductoVendido> listarProductosMasVendidos();

}
