package com.dsklabs.epam.tecnicaltest.q04testing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Capa de CARGA de la pirámide de testing (el bloque lateral del artículo).
 *
 * <p><b>No se ejecuta en el run por defecto.</b> Está excluido en el POM porque es lento y
 * dependiente de la máquina: un CI lento lo convertiría en un test rojo. Para ejecutarlo:
 *
 * <pre>{@code
 * ./mvnw test -Dtest=LoadTest -DfailIfNoTests=false -Dsurefire.excludes=
 * }</pre>
 *
 * <p>Lo que demuestra: un pico de peticiones concurrentes contra la app, midiendo latencia y
 * throughput. Es el bloque que el artículo llama "component, load or performance tests".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadTest {

    private static final int REQUESTS = 500;
    private static final int CONCURRENCY = 50;

    @LocalServerPort
    private int port;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Test
    @DisplayName("Aguanta un pico de peticiones concurrentes y reporta latencias")
    void loadTest() throws Exception {
        URI uri = URI.create("http://localhost:" + port + "/q05/order?strategy=tree&size=10");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        AtomicInteger errors = new AtomicInteger();
        List<Long> latencies = java.util.Collections.synchronizedList(new ArrayList<>());

        long start = System.nanoTime();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < REQUESTS; i++) {
                executor.submit(() -> {
                    long requestStart = System.nanoTime();
                    try {
                        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
                        HttpResponse<String> response = client.send(request,
                                HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() >= 400) {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latencies.add(System.nanoTime() - requestStart);
                    }
                });
            }
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
        long elapsedNanos = System.nanoTime() - start;

        List<Long> sorted = new ArrayList<>(latencies);
        sorted.sort(Long::compareTo);

        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double rps = REQUESTS / elapsedSeconds;

        System.out.printf("""
                === Resultado del test de carga ===
                Peticiones:      %d
                Concurrencia:    %d
                Errores:         %d
                Tiempo total:    %.3f s
                Peticiones/s:    %.1f
                p50:             %.2f ms
                p95:             %.2f ms
                p99:             %.2f ms
                """,
                REQUESTS, CONCURRENCY, errors.get(), elapsedSeconds, rps,
                percentile(sorted, 50) / 1_000_000.0,
                percentile(sorted, 95) / 1_000_000.0,
                percentile(sorted, 99) / 1_000_000.0);

        assertThat(sorted).hasSize(REQUESTS);
        assertThat(errors.get()).as("ninguna petición debe fallar bajo carga").isZero();
        assertThat(rps).as("debe atender al menos 20 peticiones por segundo").isGreaterThan(20.0);
    }

    private long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.min(Math.max(rank - 1, 0), sorted.size() - 1));
    }
}
