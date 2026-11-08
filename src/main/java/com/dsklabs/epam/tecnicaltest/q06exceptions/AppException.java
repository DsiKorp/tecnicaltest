package com.dsklabs.epam.tecnicaltest.q06exceptions;

/**
 * Raíz de la jerarquía de excepciones de la aplicación.
 *
 * <p>Es {@code sealed}: el compilador conoce el conjunto completo de subtipos, lo que permite
 * un {@code switch} exhaustivo <b>sin {@code default}</b> sobre el tipo. Si mañana se añade una
 * sexta excepción concreta, el compilador rompe la compilación en vez de dejar que la app
 * devuelva un 500 por una rama no contemplada.
 *
 * <p>La jerarquía separa dos familias con responsabilidades distintas:
 * <ul>
 *   <li>{@link BusinessException} — el fallo es <b>esperado</b> y explicable en lenguaje de
 *       negocio ("no hay stock", "el pedido no existe"). El cliente puede actuar sobre él.</li>
 *   <li>{@link TechnicalException} — el fallo es <b>infraestructura</b> (la base de datos se cayó,
 *       un servicio externo no responde). El cliente no puede hacer nada útil con él.</li>
 * </ul>
 *
 * <p>Esa distinción es lo que permite construir una <b>fault barrier</b>: la capa de servicio
 * traduce los fallos técnicos en fallos de negocio, de modo que el detalle técnico
 * (un SQLSTATE, un timeout) nunca cruza la frontera hacia el consumidor.
 */
public sealed abstract class AppException extends Exception
        permits BusinessException, TechnicalException {

    private final String errorCode;

    protected AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AppException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Código de error estable y legible por máquina. No cambia aunque cambie el mensaje:
     * los clientes deben ramificar sobre el código, no sobre el texto.
     */
    public String errorCode() {
        return errorCode;
    }
}
