package com.isil.edu.pe.spedidos.service.impl;

import java.math.BigDecimal;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import com.isil.edu.pe.spedidos.exception.ReporteValidacionException;


import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.repository.projection.ResumenPedidosProjection;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import com.isil.edu.pe.spedidos.service.domain.ProductoVendido;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PedidoReporteServiceImpl implements PedidoReporteService {

  @Override
  public List<Pedido> buscarPedidosPorClienteYTotalMinimo(String cliente, BigDecimal totalMinimo) {
    if (cliente == null || cliente.isBlank()) {
      throw new ReporteValidacionException("El cliente es obligatorio.");
    }
    if (totalMinimo == null || totalMinimo.signum() < 0) {
      throw new ReporteValidacionException("totalMinimo debe ser mayor o igual a cero.");
    }
    return jdbcRepository.buscarPedidosPorClienteYTotalMinimo(cliente.trim(), totalMinimo);
  }

  @Override
  public List<ClienteFrecuente> listarClientesFrecuentes(Integer minPedidos) {
    if (minPedidos == null || minPedidos < 1) {
      throw new ReporteValidacionException("minPedidos debe ser mayor o igual a uno.");
    }
    return jdbcRepository.listarClientesFrecuentes(minPedidos).stream()
        .map(item -> new ClienteFrecuente(
            item.cliente(), item.totalPedidos(), item.totalUnidades(),
            item.importeAcumulado(), item.ticketPromedio())).toList();
  }

  private final PedidoReporteJdbcRepository jdbcRepository;

  public PedidoReporteServiceImpl(PedidoReporteJdbcRepository jdbcRepository) {
      this.jdbcRepository = jdbcRepository;
  }

  @Override
  public ResumenPedidos obtenerResumen() {
    ResumenPedidosProjection resumenPedidos = jdbcRepository.obtenerResumen();
    return new ResumenPedidos(resumenPedidos.totalPedidos(), resumenPedidos.totalUnidades(),
        resumenPedidos.importeAcumulado(), resumenPedidos.ticketPromedio());
  }

  @Override
  public List<Pedido> buscarPorCliente(String cliente) {
    if (cliente == null || cliente.isBlank()) {
      return List.of();
    }

    return jdbcRepository.buscarPorCliente(cliente);
  }

  @Override
  public List<ProductoVendido> listarProductosMasVendidos() {
    return jdbcRepository.listarProductosMasVendidos()
        .stream()
        .map(item ->
            new ProductoVendido(
                item.productoCodigo(),
                item.productoNombre(),
                item.totalPedidos(),
                item.totalUnidades(),
                item.importeAcumulado()
            )
        )
        .toList();

  }
}
