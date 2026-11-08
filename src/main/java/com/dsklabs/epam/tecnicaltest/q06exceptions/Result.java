package com.dsklabs.epam.tecnicaltest.q06exceptions;

/**
 * Resultado explícito de una operación que puede fallar de forma <b>esperada</b>.
 *
 * <p>Es la alternativa a lanzar excepciones para el flujo normal. Con un tipo sellado:
 * <ul>
 *   <li>el compilador obliga a cubrir los dos casos ({@code switch} exhaustivo sin {@code default});</li>
 *   <li>el "fallo" es un valor de retorno, no una excepción: se documenta en la firma;</li>
 *   <li>no hay el coste de construir el stack trace cuando el fallo es el camino esperado
 *       (un 404 en un catálogo se pide miles de veces por segundo).</li>
 * </ul>
 *
 * <p>No reemplaza a las excepciones: se usa para el flujo esperado (no encontrado, sin stock) y
 * se reservan las excepciones para lo verdaderamente excepcional (un bug, un fallo de infra).
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();

    T valueOrThrow();

    record Success<T>(T value) implements Result<T> {

        public static <T> Success<T> of(T value) {
            return new Success<>(value);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T valueOrThrow() {
            return value;
        }
    }

    record Failure<T>(String errorCode, String message) implements Result<T> {

        public static <T> Failure<T> of(String errorCode, String message) {
            return new Failure<>(errorCode, message);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public T valueOrThrow() {
            throw new IllegalStateException("Resultado fallido [" + errorCode + "]: " + message);
        }
    }
}
