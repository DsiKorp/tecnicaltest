package com.dsklabs.epam.tecnicaltest.q10httpget;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 10 ("¿qué limitaciones tiene el GET?").
 *
 * <ul>
 *   <li>{@code GET /q10/measure?length=3000} — mide una URL contra el presupuesto práctico (~2K)
 *       y contra el límite real del contenedor.</li>
 *   <li>{@code GET /q10/body-probe} — acepta un cuerpo opcional en un GET y explica por qué es
 *       mala práctica aunque el contenedor lo acepte.</li>
 *   <li>{@code GET /q10/unsafe-increment} — <b>ANTI-PATRÓN</b>: un GET que muta el estado.</li>
 *   <li>{@code POST /q10/safe-increment} — la forma correcta de mutar estado.</li>
 * </ul>
 */
@RestController
@RequestMapping("/q10")
public class GetLimitsController {

    private final GetLimitsService service;

    public GetLimitsController(GetLimitsService service) {
        this.service = service;
    }

    /**
     * Compara la longitud pedida con el presupuesto de diseño y con el límite del contenedor.
     */
    @GetMapping("/measure")
    public ApiResponse<Map<String, Object>> measure(@RequestParam(defaultValue = "3000") int length) {
        return ApiResponse.of("q10-http-get",
                "Longitud de URL: presupuesto práctico (~2K) frente al límite real del contenedor",
                service.measure(length));
    }

    /**
     * Comprueba si el GET ha llegado con cuerpo.
     *
     * <p>El cuerpo es opcional a propósito: se puede llamar sin cuerpo para ver el caso normal.
     */
    @GetMapping("/body-probe")
    public ApiResponse<Map<String, Object>> bodyProbe(@RequestBody(required = false) String body) {
        boolean received = body != null && !body.isEmpty();

        return ApiResponse.of("q10-http-get",
                received
                        ? "El GET ha llegado con cuerpo: se acepta, pero no es buena práctica"
                        : "El GET ha llegado sin cuerpo: esta es la forma correcta",
                service.bodyProbe(body));
    }

    /**
     * <b>ANTI-PATRÓN:</b> un GET que cambia el estado del servidor.
     *
     * <p>Está aquí a propósito, y el endpoint está en un GET justamente para que se vea en la
     * demo por qué está mal.
     */
    @GetMapping("/unsafe-increment")
    public ApiResponse<Map<String, Object>> unsafeIncrement() {
        return ApiResponse.of("q10-http-get",
                "ANTI-PATRÓN: un GET que incrementa un contador (el verbo está equivocado)",
                service.unsafeIncrement());
    }

    /**
     * La forma correcta: POST para mutar estado.
     *
     * <p>Con cabecera {@code Idempotency-Key} el incremento es idempotente: un reintento con la
     * misma clave no vuelve a incrementar.
     */
    @PostMapping("/safe-increment")
    public ApiResponse<Map<String, Object>> safeIncrement(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.of("q10-http-get",
                "La forma correcta: POST para mutar estado (idempotente con Idempotency-Key)",
                service.safeIncrement(idempotencyKey));
    }
}
