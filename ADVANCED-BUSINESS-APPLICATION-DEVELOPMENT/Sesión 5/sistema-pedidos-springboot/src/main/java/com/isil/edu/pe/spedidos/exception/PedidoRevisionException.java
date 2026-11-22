package com.isil.edu.pe.spedidos.exception;

public class PedidoRevisionException extends RuntimeException {
  public enum Motivo { INVALIDO, NO_ENCONTRADO, YA_REVISADO }
  private final Motivo motivo;
  public PedidoRevisionException(Motivo motivo, String mensaje) {
    super(mensaje);
    this.motivo = motivo;
  }
  public Motivo getMotivo() { return motivo; }
}
