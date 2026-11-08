package com.dsklabs.epam.tecnicaltest.q06exceptions;

/** El pedido solicitado no existe. Fallo de negocio → HTTP 404. */
public final class OrderNotFoundException extends BusinessException {

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("ORDER_NOT_FOUND", "No existe el pedido '" + orderId + "'");
        this.orderId = orderId;
    }

    public String orderId() {
        return orderId;
    }
}
