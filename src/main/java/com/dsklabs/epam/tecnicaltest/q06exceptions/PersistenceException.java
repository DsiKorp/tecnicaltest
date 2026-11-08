package com.dsklabs.epam.tecnicaltest.q06exceptions;

/**
 * Fallo técnico de persistencia (BD caída, timeout, violación de constraint no prevista).
 *
 * <p>Es el ejemplo canónico de lo que una <b>fault barrier</b> debe interceptar: el service
 * layer la convierte en un fallo de negocio y el consumidor nunca ve el SQLSTATE.
 */
public final class PersistenceException extends TechnicalException {

    public PersistenceException(String message) {
        super("PERSISTENCE_FAILURE", message);
    }

    public PersistenceException(String message, Throwable cause) {
        super("PERSISTENCE_FAILURE", message, cause);
    }
}
