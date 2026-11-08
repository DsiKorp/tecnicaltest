package com.dsklabs.epam.tecnicaltest.q09aop;

import java.util.List;
import java.util.Locale;

import com.dsklabs.epam.tecnicaltest.support.web.ForbiddenOperationException;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspecto de autorización: exige un rol válido antes de ejecutar el método anotado con
 * {@link Authorized}.
 *
 * <p>El rol se lee de la cabecera {@code X-Role} de la petición en curso. Si <b>no hay
 * petición en curso</b> (por ejemplo, una {@code main} de consola o una llamada desde un test
 * sin contexto web) se asume el rol {@code ADMIN}: así la demo no revienta fuera del
 * contenedor. Si hay petición pero la cabecera falta, el rol es {@code SIN_ROL} y la operación
 * se rechaza con 403.
 *
 * <p>Un advice {@code @Before} que lanza una excepción aborta la cadena: el método de negocio
 * no llega a ejecutarse nunca.
 */
@Aspect
@Component
@Order(10)
public class AuthorizationAspect {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationAspect.class);

    /** Cabecera HTTP que transporta el rol del llamante. */
    public static final String CABECERA_ROL = "X-Role";

    /** Roles aceptados. */
    public static final List<String> ROLES_PERMITIDOS = List.of("ADMIN", "USER");

    /** Rol asumido cuando no hay ninguna petición web en curso. */
    public static final String ROL_POR_DEFECTO_SIN_PETICION = "ADMIN";

    /** Rol asignado cuando hay petición pero la cabecera no viene. */
    public static final String ROL_SIN_CABECERA = "SIN_ROL";

    /**
     * Advice "before": si el rol no es válido, aborta la llamada con 403.
     *
     * @param authorized anotación que dispara el advice (nombre de la operación protegida)
     */
    @Before("@annotation(authorized)")
    public void autorizar(Authorized authorized) {
        String rol = rolActual();

        if (!ROLES_PERMITIDOS.contains(rol)) {
            throw new ForbiddenOperationException(
                    "El rol '" + rol + "' no puede ejecutar la operación '" + authorized.value()
                            + "'. Roles permitidos: " + String.join(", ", ROLES_PERMITIDOS) + ".");
        }

        log.debug("[AUTORIZACION] operacion={} rol={}", authorized.value(), rol);
    }

    /**
     * Resuelve el rol de la petición en curso.
     *
     * @return el rol en mayúsculas, o {@link #ROL_SIN_CABECERA} si la cabecera no viene
     */
    private String rolActual() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            String rol = servletAttributes.getRequest().getHeader(CABECERA_ROL);
            if (rol == null || rol.isBlank()) {
                return ROL_SIN_CABECERA;
            }
            return rol.trim().toUpperCase(Locale.ROOT);
        }

        // Fuera de una petición web (main de consola, test unitario): asumimos ADMIN para no
        // hacer fallar la demo fuera del contenedor.
        return ROL_POR_DEFECTO_SIN_PETICION;
    }
}
