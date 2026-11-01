package com.isil.edu.pe.spedidos.service.domain;

import java.math.BigDecimal;

/** Resultado agregado por nombre de cliente; no representa una identidad única. */
public record ClienteFrecuente(String cliente, long totalPedidos, long totalUnidades,
    BigDecimal importeAcumulado, BigDecimal ticketPromedio) {
}
