package com.dsklabs.epam.tecnicaltest.q06exceptions;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Repository;

/**
 * Implementación en memoria del repositorio, solo para la demo.
 *
 * <p>El interruptor {@link #setFailing(boolean)} es lo que permite demostrar en vivo la
 * fault barrier: se activa, se llama al servicio, y se ve cómo un fallo técnico de la BD
 * llega al consumidor como un 409 de negocio en vez de como un 500 con el SQLSTATE.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of("SKU-1", 10, "SKU-2", 1));
    private final Map<String, String> orders = new ConcurrentHashMap<>();
    private final AtomicBoolean failing = new AtomicBoolean(false);

    @Override
    public void save(String orderId, String sku, int quantity) throws PersistenceException {
        if (failing.get()) {
            throw new PersistenceException(
                    "No se pudo guardar el pedido '" + orderId + "': timeout de conexión a la BD (SQLSTATE 08006)");
        }
        orders.put(orderId, sku + "x" + quantity);
    }

    @Override
    public Optional<Map.Entry<String, Integer>> findStock(String sku) throws PersistenceException {
        if (failing.get()) {
            throw new PersistenceException(
                    "No se pudo leer el stock de '" + sku + "': timeout de conexión a la BD (SQLSTATE 08006)");
        }
        return Optional.ofNullable(stock.get(sku)).map(v -> Map.entry(sku, v));
    }

    @Override
    public void setFailing(boolean failing) {
        this.failing.set(failing);
    }

    @Override
    public boolean isFailing() {
        return failing.get();
    }

    public Map<String, String> savedOrders() {
        return Map.copyOf(orders);
    }
}
