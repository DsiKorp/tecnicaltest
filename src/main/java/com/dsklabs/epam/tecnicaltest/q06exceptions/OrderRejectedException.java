package com.dsklabs.epam.tecnicaltest.q06exceptions;

/**
 * Fallo de negocio genérico que nace en la <b>fault barrier</b>: el servicio atrapa una
 * {@link TechnicalException} y la traduce a este error de negocio.
 *
 * <p>El consumidor recibe "no pudimos procesar tu pedido" (un 409/422), no "la conexión a la
 * BD dio timeout contra el host jdbc://interno:5432".
 */
public final class OrderRejectedException extends BusinessException {

    public OrderRejectedException(String message, Throwable cause) {
        super("ORDER_REJECTED", message, cause);
    }
}
