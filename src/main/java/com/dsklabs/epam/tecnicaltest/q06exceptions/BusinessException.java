package com.dsklabs.epam.tecnicaltest.q06exceptions;

/**
 * Fallo de negocio: la petición es válida pero la operación no se puede completar por
 * una razón que el cliente entiende y puede corregir.
 *
 * <p>Ejemplos: el pedido no existe, no hay stock suficiente, el cliente no tiene permiso
 * para esa operación.
 *
 * <p>Se marca {@code non-sealed} para poder añadir excepciones de negocio concretas sin
 * cambiar esta clase ni forzar un {@code default} en los switches exhaustivos.
 */
public abstract non-sealed class BusinessException extends AppException {

    protected BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
