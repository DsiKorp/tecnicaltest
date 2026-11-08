package com.dsklabs.epam.tecnicaltest.q09aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio para que {@link AuditAspect} lo audite.
 *
 * <p>Es una anotación <b>de método</b>, no de clase: Spring AOP hace coincidir el advice
 * únicamente con los métodos anotados, y el proxy solo intercepta esos. Si la anotación
 * estuviera en la clase, el advice se aplicaría a todos los métodos públicos de esa clase.
 *
 * <p>El {@link #value()} es una etiqueta libre que el aspect escribe en la traza: sirve para
 * distinguir operaciones de negocio que comparten el mismo método.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /** Etiqueta descriptiva de la operación, visible en el log de auditoría. */
    String value() default "";
}
