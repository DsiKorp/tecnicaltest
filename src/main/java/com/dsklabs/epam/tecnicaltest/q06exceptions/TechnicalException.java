package com.dsklabs.epam.tecnicaltest.q06exceptions;

/**
 * Fallo técnico: la infraestructura no pudo completar la operación.
 *
 * <p>Ejemplos: la base de datos no responde, un servicio externo devuelve 500, el disco está lleno.
 *
 * <p>Estas excepciones <b>no deberían llegar al consumidor</b>: un service layer las convierte
 * en un {@link BusinessException} genérico (fault barrier). Si se escapan, el handler global las
 * traduce a un 502/500 <b>sin filtrar el detalle técnico</b>.
 */
public abstract non-sealed class TechnicalException extends AppException {

    protected TechnicalException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected TechnicalException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
