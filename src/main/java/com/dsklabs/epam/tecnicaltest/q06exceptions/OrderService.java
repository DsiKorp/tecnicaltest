package com.dsklabs.epam.tecnicaltest.q06exceptions;

import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Capa de negocio. Aquí está la <b>fault barrier</b>.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>Atrapar las {@link TechnicalException} que vienen del repositorio y convertirlas en
 *       {@link OrderRejectedException} (negocio). El detalle técnico (SQLSTATE, host, timeout)
 *       queda dentro de la aplicación y nunca cruza la frontera HTTP.</li>
 *   <li>Dejar pasar las {@link BusinessException} sin tocar: ya están en el lenguaje correcto.</li>
 *   <li>Validar reglas de negocio (stock suficiente) y lanzar la excepción concreta.</li>
 * </ol>
 *
 * <p><b>Por qué importa:</b> el consumidor queda desacoplado de la topología interna. Mañana la
 * BD se cambia por un queue y el contrato público del servicio no cambia.
 */
@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    /**
     * Coloca un pedido. Puede fallar.
     *
     * @throws InsufficientStockException si no hay stock (negocio, 409)
     * @throws OrderRejectedException    si la infraestructura falló (negocio tras la barrier, 409)
     */
    public String placeOrder(String orderId, String sku, int quantity) throws BusinessException {
        if (quantity <= 0) {
            throw new InsufficientStockException(sku, quantity, 0);
        }

        // --- LA FAULT BARRIER ---
        // Todo lo que viene del repositorio (técnico) se atrapa aquí y se traduce.
        // Todo lo que sale hacia arriba (hacia el controller) es de negocio.
        int available;
        try {
            available = repository.findStock(sku)
                    .map(Map.Entry::getValue)
                    .orElse(0);
        } catch (TechnicalException e) {
            // El consumidor NO ve el SQLSTATE: ve un error de negocio estable.
            throw new OrderRejectedException(
                    "No pudimos confirmar el pedido '" + orderId + "' porque el almacén no está disponible. Inténtalo más tarde.",
                    e);
        }

        if (available < quantity) {
            // Fallo de negocio: el cliente SÍ puede corregirlo (pedir menos unidades).
            throw new InsufficientStockException(sku, quantity, available);
        }

        try {
            repository.save(orderId, sku, quantity);
        } catch (TechnicalException e) {
            throw new OrderRejectedException(
                    "No pudimos confirmar el pedido '" + orderId + "' porque el almacén no está disponible. Inténtalo más tarde.",
                    e);
        }

        return "Pedido '" + orderId + "' confirmado: " + sku + " x" + quantity;
    }

    /**
     * Variante que devuelve un {@link Result} en vez de lanzar excepción.
     *
     * <p>Es la alternativa al uso de excepciones para el flujo normal: cuando un fallo es
     * <b>esperado</b> (y no excepcional), un tipo sellado con estado explícito suele ser más
     * legible y obliga al compilador a cubrir todos los casos.
     */
    public Result<String> tryPlaceOrder(String orderId, String sku, int quantity) {
        try {
            return Result.Success.of(placeOrder(orderId, sku, quantity));
        } catch (BusinessException e) {
            return Result.Failure.of(e.errorCode(), e.getMessage());
        }
    }
}
