package com.dsklabs.epam.tecnicaltest.q06exceptions;

import java.util.Map;
import java.util.Optional;

/**
 * Capa técnica de persistencia. Es la que falla con errores de infraestructura.
 *
 * <p>La demo la implementa en memoria, pero el contrato es el de un repositorio real: si la
 * BD no responde, lanza {@link PersistenceException} (técnica), nunca una excepción de negocio.
 */
public interface OrderRepository {

    /** @throws PersistenceException si el almacén no está disponible */
    void save(String orderId, String sku, int quantity) throws PersistenceException;

    /** @throws PersistenceException si el almacén no está disponible */
    Optional<Map.Entry<String, Integer>> findStock(String sku) throws PersistenceException;

    /**
     * Simula una caída de la infraestructura para la demo.
     * En un repositorio real esto sería, por ejemplo, que la BD no responda.
     */
    void setFailing(boolean failing);

    boolean isFailing();
}
