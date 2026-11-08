package com.dsklabs.epam.tecnicaltest.support.web;

/**
 * Sobre (envelope) común de la respuesta de todos los endpoints de demo.
 *
 * <p>Un único formato para las 10 preguntas hace que la landing page pueda renderizar cualquier
 * resultado con el mismo código, y que el test end-to-end pueda validar la misma clave
 * ("demo") en las diez demos.
 *
 * @param demo    identificador de la pregunta, p.ej. "q01-collections"
 * @param summary frase corta que resume qué acaba de pasar
 * @param data    payload específico de la demo
 */
public record ApiResponse<T>(String demo, String summary, T data) {

    public static <T> ApiResponse<T> of(String demo, String summary, T data) {
        return new ApiResponse<>(demo, summary, data);
    }
}
