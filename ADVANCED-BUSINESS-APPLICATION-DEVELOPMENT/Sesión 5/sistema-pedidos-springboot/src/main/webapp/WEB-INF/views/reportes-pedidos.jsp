<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!doctype html>
<html lang="es">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Reportes de pedidos</title>
  <c:url var="cssUrl" value="/assets/css/app.css"/>
  <link rel="stylesheet" href="${cssUrl}">
</head>
<body><main class="contenedor">
  <h1>Reportes de pedidos</h1>
  <c:url var="pedidosUrl" value="/pedidos"/>
  <a href="${pedidosUrl}">Volver a pedidos</a>
  <c:url var="logoutUrl" value="/logout"/>
  <form method="post" action="${logoutUrl}">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
    <button type="submit">Cerrar sesión</button>
  </form>
  <c:if test="${not empty error}"><p role="alert"><c:out value="${error}"/></p></c:if>
  <c:if test="${not empty resumen}">
    <section class="tarjeta"><h2>Resumen general</h2>
      <p>Pedidos: <c:out value="${resumen.totalPedidos()}"/> |
        Unidades: <c:out value="${resumen.totalUnidades()}"/> |
        Importe acumulado: <c:out value="${resumen.importeAcumulado()}"/> |
        Ticket promedio: <c:out value="${resumen.ticketPromedio()}"/></p>
    </section>
  </c:if>
  <c:url var="reportesUrl" value="/reportes/pedidos"/>
  <section class="tarjeta"><h2>Búsqueda avanzada</h2>
    <form method="get" action="${reportesUrl}" class="form-grid">
      <label>Cliente <input name="cliente" required value="${fn:escapeXml(cliente)}"></label>
      <label>Total mínimo <input name="totalMinimo" type="number" min="0" step="0.01"
        required value="${empty totalMinimo ? '0' : fn:escapeXml(totalMinimo)}"></label>
      <button type="submit">Buscar</button>
    </form>
    <c:if test="${busquedaRealizada}">
      <table><thead><tr><th>ID</th><th>Cliente</th><th>Producto</th><th>Cantidad</th><th>Total</th><th>Fecha</th></tr></thead>
      <tbody><c:forEach var="pedido" items="${pedidos}">
        <tr><td><c:out value="${pedido.id}"/></td><td><c:out value="${pedido.cliente}"/></td>
          <td><c:out value="${pedido.productoNombre}"/></td><td><c:out value="${pedido.cantidad}"/></td>
          <td><c:out value="${pedido.total}"/></td><td><c:out value="${pedido.fecha}"/></td></tr>
      </c:forEach></tbody></table>
      <c:if test="${empty pedidos}"><p>No hay pedidos que coincidan con los filtros.</p></c:if>
    </c:if>
  </section>
  <section class="tarjeta"><h2>Clientes frecuentes</h2>
    <form method="get" action="${reportesUrl}">
      <label>Mínimo de pedidos <input name="minPedidos" type="number" min="1" required
        value="${empty minPedidos ? '2' : fn:escapeXml(minPedidos)}"></label>
      <button type="submit">Consultar</button>
    </form>
    <table><thead><tr><th>Cliente</th><th>Pedidos</th><th>Unidades</th><th>Importe acumulado</th><th>Ticket promedio</th></tr></thead>
    <tbody><c:forEach var="item" items="${clientes}">
      <tr><td><c:out value="${item.cliente()}"/></td><td><c:out value="${item.totalPedidos()}"/></td>
        <td><c:out value="${item.totalUnidades()}"/></td><td><c:out value="${item.importeAcumulado()}"/></td>
        <td><c:out value="${item.ticketPromedio()}"/></td></tr>
    </c:forEach></tbody></table>
    <c:if test="${empty clientes}"><p>No hay clientes para el umbral indicado.</p></c:if>
  </section>
</main></body></html>
