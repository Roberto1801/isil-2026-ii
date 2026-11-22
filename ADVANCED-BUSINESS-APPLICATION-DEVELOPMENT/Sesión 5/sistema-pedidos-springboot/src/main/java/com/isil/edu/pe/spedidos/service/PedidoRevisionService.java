package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.PedidoRevisionException;
import static com.isil.edu.pe.spedidos.exception.PedidoRevisionException.Motivo.*;
import com.isil.edu.pe.spedidos.repository.PedidoRevisionJdbcRepository;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoRevisionService {
  private final PedidoRevisionJdbcRepository repository;
  public PedidoRevisionService(PedidoRevisionJdbcRepository repository) { this.repository = repository; }

  @Transactional(rollbackFor = Exception.class)
  public void revisar(long pedidoId, String observacion) {
    if (pedidoId <= 0) throw new PedidoRevisionException(INVALIDO, "El pedido debe tener un ID positivo.");
    if (observacion == null || observacion.isBlank() || observacion.length() > 500) {
      throw new PedidoRevisionException(INVALIDO, "La observación es obligatoria y admite hasta 500 caracteres.");
    }
    boolean revisado = repository.bloquearEstado(pedidoId)
        .orElseThrow(() -> new PedidoRevisionException(NO_ENCONTRADO, "El pedido no existe."));
    if (revisado) throw duplicado();
    if (repository.marcarRevisado(pedidoId) != 1) throw duplicado();
    try {
      if (repository.registrarRevision(pedidoId, observacion.strip()) != 1) {
        throw new IllegalStateException("No se pudo guardar la revisión.");
      }
    } catch (DuplicateKeyException e) {
      throw duplicado();
    }
  }

  private PedidoRevisionException duplicado() {
    return new PedidoRevisionException(YA_REVISADO, "El pedido ya tiene una revisión registrada.");
  }

  @Transactional(readOnly = true)
  public List<Pedido> listarPendientes() { return repository.listarPendientes(); }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listarRevisiones() { return repository.listarRevisiones(); }
}
