package com.dsklabs.epam.tecnicaltest.q08redis;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Publica en {@code /actuator/health} qué almacén de blobs está activo.
 *
 * <p>El health indicator oficial de Redis está desactivado ({@code management.health.redis.enabled=false}):
 * en AWS no hay Redis y reportaría DOWN aunque la aplicación esté perfectamente sana. Este
 * indicador informa de lo que de verdad importa: <b>que hay un almacén funcionando</b>.
 */
@Component
public class BlobStoreHealthIndicator implements HealthIndicator {

    private final ChunkedBlobStore store;
    private final ChunkedBlobStoreConfiguration configuration;

    public BlobStoreHealthIndicator(ChunkedBlobStore store, ChunkedBlobStoreConfiguration configuration) {
        this.store = store;
        this.configuration = configuration;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up()
                .withDetail("store", store.kind())
                .withDetail("mode", configuration.mode())
                .withDetail("reason", configuration.reason())
                .withDetail("chunkSizeBytes", configuration.chunkSize());
        return builder.build();
    }
}
