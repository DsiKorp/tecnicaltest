package com.dsklabs.epam.tecnicaltest.q06exceptions;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 6 (jerarquía de excepciones).
 *
 * <ul>
 *   <li>{@code GET /q06/hierarchy} — el árbol de la jerarquía y el status HTTP de cada rama.</li>
 *   <li>{@code GET /q06/throw?case=...} — provoca cada tipo de fallo para ver la respuesta.</li>
 *   <li>{@code GET /q06/fault-barrier} — activa el fallo de la "base de datos" y demuestra que
 *       al cliente le llega un 409 de negocio, no un 500 con el SQLSTATE.</li>
 * </ul>
 */
@RestController
@RequestMapping("/q06")
public class ExceptionHierarchyController {

    private final OrderService orderService;
    private final InMemoryOrderRepository repository;

    public ExceptionHierarchyController(OrderService orderService, InMemoryOrderRepository repository) {
        this.orderService = orderService;
        this.repository = repository;
    }

    @GetMapping("/hierarchy")
    public ApiResponse<Map<String, Object>> hierarchy() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("raiz", "AppException (sealed)");
        data.put("familias", List.of(
                Map.of("nombre", "BusinessException",
                        "significado", "Fallo esperado y explicable en lenguaje de negocio",
                        "ejemplos", List.of("OrderNotFoundException", "InsufficientStockException",
                                "OrderRejectedException"),
                        "statusHttp", "404 / 409 / 400"),
                Map.of("nombre", "TechnicalException",
                        "significado", "Fallo de infraestructura: no es culpa del cliente",
                        "ejemplos", List.of("PersistenceException", "ExternalServiceException"),
                        "statusHttp", "500 / 502")));
        data.put("ventajaDeSellada",
                "El compilador conoce el conjunto completo de subtipos: el switch del handler "
                        + "global es exhaustivo SIN default. Si se añade una excepción concreta "
                        + "nueva, el build falla hasta que se decida su status HTTP.");
        data.put("faultBarrier",
                "OrderService convierte las TechnicalException en OrderRejectedException "
                        + "(negocio). El detalle técnico (SQLSTATE, host, timeout) no cruza la "
                        + "frontera HTTP.");
        data.put("capturaEnTresNiveles", List.of(
                "1. Concreto: el service captura OrderNotFoundException.",
                "2. Familia: el service captura BusinessException.",
                "3. Base: el handler global captura AppException."));

        return ApiResponse.of("q06-hierarchy", "Jerarquía de excepciones de la aplicación", data);
    }

    /**
     * Provoca un fallo concreto para ver cómo se traduce a HTTP.
     *
     * @param caso uno de: not-found, stock, persistence, external, success
     */
    @GetMapping("/throw")
    public ApiResponse<Map<String, Object>> trigger(@RequestParam(defaultValue = "not-found") String caso)
            throws AppException {

        return switch (caso.toLowerCase()) {
            case "not-found" -> throw new OrderNotFoundException("ORD-999");
            case "stock" -> throw new InsufficientStockException("SKU-2", 5, 1);
            case "persistence" -> throw new PersistenceException(
                    "timeout de conexión a la BD (SQLSTATE 08006) contra jdbc://interno:5432");
            case "external" -> throw new ExternalServiceException("pasarela-de-pagos",
                    "HTTP 503 Service Unavailable");
            case "success" -> ApiResponse.of("q06-throw", "Sin errores: el camino feliz", Map.of(
                    "resultado", orderService.placeOrder("ORD-1", "SKU-1", 2)));
            default -> throw new IllegalArgumentException(
                    "Caso desconocido: '" + caso + "'. Usa: not-found | stock | persistence | external | success");
        };
    }

    /**
     * Demuestra la fault barrier en vivo.
     *
     * <p>Con {@code fail=true} la "base de datos" falla. El service atrapa la
     * {@link PersistenceException} y la traduce; la respuesta es un 409 de negocio con un
     * mensaje limpio, no un 500 filtrando el SQLSTATE.
     */
    @GetMapping("/fault-barrier")
    public ApiResponse<Map<String, Object>> faultBarrier(@RequestParam(defaultValue = "false") boolean fail)
            throws BusinessException {

        repository.setFailing(fail);
        try {
            String resultado = orderService.placeOrder("ORD-777", "SKU-1", 1);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("almacenCaido", true);
            data.put("resultado", resultado);
            data.put("nota", "No debería llegar aquí: el almacén está marcado como caído.");
            return ApiResponse.of("q06-fault-barrier", "El almacén no falló", data);
        } catch (OrderRejectedException e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("almacenCaido", true);
            data.put("excepcionInterna", e.getCause() == null ? null : e.getCause().getClass().getSimpleName());
            data.put("detalleTecnico filtradoAlCliente", false);
            data.put("errorCode", e.errorCode());
            data.put("mensajeAlCliente", e.getMessage());
            data.put("nota",
                    "La PersistenceException (técnica) fue atrapada por el service y traducida a "
                            + "una OrderRejectedException (negocio). El cliente recibe un 409 con un "
                            + "código estable, y el SQLSTATE se queda dentro de la aplicación.");
            return ApiResponse.of("q06-fault-barrier",
                    "El fallo técnico se tradujo a un fallo de negocio (fault barrier)", data);
        } finally {
            repository.setFailing(false);
        }
    }

    /**
     * La variante con {@link Result}: el fallo esperado se devuelve como valor, no como excepción.
     */
    @GetMapping("/result")
    public ApiResponse<Map<String, Object>> result(@RequestParam(defaultValue = "SKU-2") String sku,
                                                    @RequestParam(defaultValue = "5") int cantidad) {
        Result<String> resultado = orderService.tryPlaceOrder("ORD-888", sku, cantidad);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exito", resultado.isSuccess());
        data.put("resultado", resultado);
        data.put("nota",
                "Con Result<T> (sellado) el fallo esperado es un VALOR de retorno: el compilador "
                        + "obliga a cubrir los dos casos y no se construye un stack trace en el "
                        + "camino normal (un 404 en un catálogo se pide miles de veces por segundo).");

        return ApiResponse.of("q06-result", "El mismo caso, con Result en vez de excepción", data);
    }
}
