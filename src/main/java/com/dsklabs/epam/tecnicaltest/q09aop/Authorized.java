package com.dsklabs.epam.tecnicaltest.q09aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio como restringido: {@link AuthorizationAspect} exige un rol
 * válido antes de dejar que el método se ejecute.
 *
 * <p><b>Advertencia importante (y es parte de la demo):</b> esta comprobación solo se aplica
 * cuando la llamada <b>entra por el proxy</b>. Si el propio bean invoca el método con
 * {@code this.metodo()}, el aspect no se ejecuta y la comprobación se elude. Es el motivo por el
 * que la autorización no debe vivir en un aspect si el código de negocio se llama a sí mismo.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Authorized {

    /** Nombre de la operación protegida, visible en el error 403. */
    String value() default "";
}
