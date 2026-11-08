package com.dsklabs.epam.tecnicaltest.q06exceptions;

/** Un servicio externo (pagos, inventario, email) falló o devolvió un error. → HTTP 502. */
public final class ExternalServiceException extends TechnicalException {

    private final String service;

    public ExternalServiceException(String service, String message) {
        super("EXTERNAL_SERVICE_FAILURE", "El servicio '" + service + "' falló: " + message);
        this.service = service;
    }

    public ExternalServiceException(String service, String message, Throwable cause) {
        super("EXTERNAL_SERVICE_FAILURE", "El servicio '" + service + "' falló: " + message, cause);
        this.service = service;
    }

    public String service() {
        return service;
    }
}
