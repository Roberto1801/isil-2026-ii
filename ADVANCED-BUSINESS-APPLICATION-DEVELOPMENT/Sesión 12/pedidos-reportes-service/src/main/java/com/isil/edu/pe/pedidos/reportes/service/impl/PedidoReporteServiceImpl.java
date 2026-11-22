package com.isil.edu.pe.pedidos.reportes.service.impl;

import com.isil.edu.pe.pedidos.reportes.exception.PedidoNoEncontradoException;
import com.isil.edu.pe.pedidos.reportes.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.pedidos.reportes.repository.PedidoRevisionReporteRepository;
import com.isil.edu.pe.pedidos.reportes.repository.projection.ResumenPedidosProjection;
import com.isil.edu.pe.pedidos.reportes.service.PedidoReporteService;
import com.isil.edu.pe.pedidos.reportes.service.domain.Pedido;
import com.isil.edu.pe.pedidos.reportes.service.domain.ProductoVendido;
import com.isil.edu.pe.pedidos.reportes.service.domain.ResumenPedidos;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * ImplementaciÃ³n del servicio de reporte de pedidos.
 */
@Service
public class PedidoReporteServiceImpl implements PedidoReporteService {

  private final PedidoReporteJdbcRepository jdbcRepository;
  private final PedidoRevisionReporteRepository revisionRepository;

  public PedidoReporteServiceImpl(PedidoReporteJdbcRepository jdbcRepository,
      PedidoRevisionReporteRepository revisionRepository) {
    this.jdbcRepository = jdbcRepository;
    this.revisionRepository = revisionRepository;
  }

  @Override
  public Pedido obtenerPorId(long id) {
    return jdbcRepository.buscarPorId(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
  }

  @Override
  public List<Pedido> listarTodos() {
    return jdbcRepository.listarTodos();
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

  @Override
  public List<Map<String, Object>> listarRevisiones(String estado) {
    return revisionRepository.listarPorEstado(estado);
  }

  @Override
  public Map<String, Object> obtenerEstadisticasRevisiones() {
    return revisionRepository.obtenerEstadisticas();
  }
}
