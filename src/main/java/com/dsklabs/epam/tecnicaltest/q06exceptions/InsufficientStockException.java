package com.dsklabs.epam.tecnicaltest.q06exceptions;

/** No hay stock suficiente para servir el pedido. Fallo de negocio → HTTP 409. */
public final class InsufficientStockException extends BusinessException {

    private final String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(String sku, int requested, int available) {
        super("INSUFFICIENT_STOCK",
                "Stock insuficiente para '" + sku + "': pedido " + requested + ", disponible " + available);
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }

    public String sku() {
        return sku;
    }

    public int requested() {
        return requested;
    }

    public int available() {
        return available;
    }
}
