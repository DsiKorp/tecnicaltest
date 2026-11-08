package com.dsklabs.epam.tecnicaltest.support.web;

import com.dsklabs.epam.tecnicaltest.q06exceptions.AppException;
import com.dsklabs.epam.tecnicaltest.q06exceptions.BusinessException;
import com.dsklabs.epam.tecnicaltest.q06exceptions.ExternalServiceException;
import com.dsklabs.epam.tecnicaltest.q06exceptions.InsufficientStockException;
import com.dsklabs.epam.tecnicaltest.q06exceptions.OrderNotFoundException;
import com.dsklabs.epam.tecnicaltest.q06exceptions.OrderRejectedException;
import com.dsklabs.epam.tecnicaltest.q06exceptions.PersistenceException;
import com.dsklabs.epam.tecnicaltest.q06exceptions.TechnicalException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handler global. Es el <b>tercer nivel</b> de captura de la jerarquía de excepciones:
 * <ol>
 *   <li>nivel concreto — el repositorio/service captura {@link OrderNotFoundException};</li>
 *   <li>nivel familia — el service captura {@link BusinessException};</li>
 *   <li>nivel base — este handler captura {@link AppException}.</li>
 * </ol>
 *
 * <p>El {@code switch} sobre el tipo sellado es <b>exhaustivo sin {@code default}</b>: si se añade
 * una excepción concreta nueva, el compilador obliga a decidir su status HTTP.
 *
 * <p>Se usa {@link ProblemDetail} (RFC 9457), que es lo que Spring devuelve por defecto en
 * Boot 3+/4: la respuesta de error es un documento JSON estándar, no un blob libre.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ProblemDetail handleAppException(AppException ex) {
        HttpStatus status = statusFor(ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("errorCode", ex.errorCode());
        problem.setProperty("exceptionType", ex.getClass().getSimpleName());
        // Solo se expone la causa cuando NO estamos detrás de una fault barrier:
        // un fallo de negocio no debe filtrar el detalle técnico de infraestructura.
        if (ex instanceof TechnicalException) {
            problem.setProperty("technicalDetail", String.valueOf(ex.getCause()));
        }
        return problem;
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.reason());
        problem.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
        problem.setProperty("errorCode", "FORBIDDEN");
        return problem;
    }

    /**
     * Mapeo de la jerarquía sellada a códigos HTTP.
     *
     * <p>Sin {@code default}: el compilador garantiza que toda excepción concreta tiene
     * un status asignado.
     */
    private HttpStatus statusFor(AppException ex) {
        return switch (ex) {
            case OrderNotFoundException e -> HttpStatus.NOT_FOUND;
            case InsufficientStockException e -> HttpStatus.CONFLICT;
            case OrderRejectedException e -> HttpStatus.CONFLICT;
            case ExternalServiceException e -> HttpStatus.BAD_GATEWAY;
            case PersistenceException e -> HttpStatus.INTERNAL_SERVER_ERROR;
            // Las familias "abiertas" cubren sus propias subclases por dominancia.
            case BusinessException e -> HttpStatus.BAD_REQUEST;
            case TechnicalException e -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
