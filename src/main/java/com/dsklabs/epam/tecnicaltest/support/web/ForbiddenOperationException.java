package com.dsklabs.epam.tecnicaltest.support.web;

/** Operación prohibida por falta de permisos. La lanza el aspect de autorización de la P9. */
public class ForbiddenOperationException extends RuntimeException {

    private final String reason;

    public ForbiddenOperationException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
