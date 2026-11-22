<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Supervisión de pedidos - ISIL</title>
  <c:url var="cssUrl" value="/assets/css/app.css"/>
  <link rel="stylesheet" href="${cssUrl}">
</head>
<body>
<div class="contenedor supervisor">
  <header class="cabecera">
    <div><h1>Supervisión de pedidos</h1><p class="subtitulo">Revise los pedidos pendientes y registre su observación.</p></div>
    <c:url var="logoutUrl" value="/logout"/>
    <form method="post" action="${logoutUrl}">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
      <button type="submit">Cerrar sesión</button>
    </form>
  </header>
  <c:if test="${not empty error}"><div class="alerta error" role="alert"><c:out value="${error}"/></div></c:if>
  <c:if test="${not empty exito}"><div class="alerta exito" role="status"><c:out value="${exito}"/></div></c:if>
  <section class="tarjeta" id="estadisticas-revisiones">
    <h2>Estadísticas de revisiones (servicio REST)</h2>

    <c:choose>
      <c:when test="${reportesDisponibles}">
        <p>Total de pedidos: <strong><c:out value="${estadisticasRevisiones.totalPedidos}"/></strong></p>
        <p>Pedidos revisados: <strong><c:out value="${estadisticasRevisiones.totalRevisados}"/></strong></p>
        <p>Pedidos pendientes: <strong><c:out value="${estadisticasRevisiones.totalPendientes}"/></strong></p>
        <p>Porcentaje revisado: <strong><c:out value="${estadisticasRevisiones.porcentajeRevisado}"/>%</strong></p>
        <p>Registros pendientes consultados por REST: <c:out value="${pendientesReporte.size()}"/></p>
        <p>Registros revisados consultados por REST: <c:out value="${revisadosReporte.size()}"/></p>
      </c:when>
      <c:otherwise>
        <p role="status">El servicio de reportes no está disponible temporalmente. Puede continuar revisando pedidos.</p>
      </c:otherwise>
    </c:choose>
  </section>
  <section class="tarjeta">
    <h2>Pedidos pendientes</h2>
    <c:if test="${empty pendientes}"><p>No hay pedidos pendientes.</p></c:if>
    <c:forEach var="pedido" items="${pendientes}">
      <article>
        <h3>Pedido #<c:out value="${pedido.id}"/> · <c:out value="${pedido.cliente}"/></h3>
        <p><c:out value="${pedido.productoNombre}"/> · Cantidad: <c:out value="${pedido.cantidad}"/> · Total: S/ <c:out value="${pedido.total}"/></p>
        <c:url var="revisionUrl" value="/supervisor/pedidos/${pedido.id}/revision"/>
        <form method="post" action="${revisionUrl}" class="form-grid">
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
          <label for="observacion-${pedido.id}">Observación (máximo 500 caracteres)</label>
          <textarea id="observacion-${pedido.id}" name="observacion" required maxlength="500" rows="3"><c:if test="${pedidoFallido eq pedido.id}"><c:out value="${observacionIngresada}"/></c:if></textarea>
          <button type="submit">Confirmar revisión</button>
        </form>
      </article>
    </c:forEach>
  </section>
  <section class="tarjeta">
    <h2>Revisiones registradas</h2>
    <c:if test="${empty revisiones}"><p>No hay revisiones registradas.</p></c:if>
    <c:forEach var="revision" items="${revisiones}">
      <article>
        <h3>Pedido #<c:out value="${revision.id}"/> · <c:out value="${revision.cliente}"/></h3>
        <p><c:out value="${revision.producto_nombre}"/></p>
        <p class="observacion"><c:out value="${revision.observacion}"/></p>
        <p>Fecha: <c:out value="${revision.fecha_revision}"/></p>
      </article>
    </c:forEach>
  </section>
</div>
</body>
</html>
