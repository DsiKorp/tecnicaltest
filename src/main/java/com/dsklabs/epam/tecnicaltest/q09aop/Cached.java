package com.dsklabs.epam.tecnicaltest.q09aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio como cacheable: {@link CachingAspect} guarda su resultado en
 * memoria durante un TTL y lo devuelve sin volver a ejecutar el método.
 *
 * <p>La clave se compone con el nombre del método y sus argumentos, así que dos llamadas con
 * argumentos distintos no comparten entrada.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Cached {

    /** Nombre del caché, por si conviven varios cachés sobre la misma clase. */
    String value() default "";
}
