package com.dsklabs.epam.tecnicaltest.q04testing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * El bloque lateral de la pirámide de testing: <b>pruebas de carga</b>.
 *
 * <p>Dispara peticiones HTTP reales contra la propia aplicación en marcha, con N peticiones en
 * vuelo, y mide la latencia de cada una. Es la versión en vivo y en caliente de la capa LOAD que
 * en el artículo va al lado de la pirámide, junto a las pruebas de rendimiento.
 *
 * <p><b>Por qué hilos virtuales:</b> el cliente {@code HttpClient} es bloqueante, así que
 * Cada petición ocupa un hilo. Con un pool clásico de N hilos, N es el techo de concurrencia y
 * además hay que dimensionarlo a mano. Con
 * {@link Executors#newVirtualThreadPerTaskExecutor()} se lanza una tarea por petición y el
 * coste de cada hilo es el de la pila, así que miles de peticiones concurrentes cuestan muy poco.
 * (Java 21+; en Java 25 es de serie y Spring Boot 4 puede usar hilos virtuales también en el
 * servidor.)
 *
 * <p><b>Por qué se miden percentiles y no la media:</b> la media esconde el problema. Si 99 de
 * 100 peticiones tardan 5 ms y una tarda 2 s, la media sale 25 ms y parece aceptable; el p99
 * dice que hay usuarios que sofre. Por eso se reportan p50, p95 y p99.
 */
@Service
public class LoadTestRunner {

    /** Tope de peticiones: la demo no puede fundir la máquina. */
    public static final int MAX_REQUESTS = 2_000;

    /** Tope de concurrencia: mismo motivo. */
    public static final int MAX_CONCURRENCY = 200;

    /** Endpoint por defecto: una demo barata y representativa. */
    public static final String DEFAULT_PATH = "/q05/order?size=10";

    private final int serverPort;

    /**
     * Cliente HTTP compartido entre ejecuciones: reutiliza el pool de conexiones.
     *
     * <p>{@code HttpClient} es inmutable y seguro para uso concurrente, así que se construye una
     * sola vez y todas las pruebas de carga lo comparten.
     */
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public LoadTestRunner(@Value("${server.port:8080}") int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Lanza la prueba de carga contra un endpoint de la aplicación en marcha.
     *
     * @param path        ruta a golpear (con query string), {@code null} o vacía = {@link #DEFAULT_PATH}
     * @param requests    número de peticiones a lanzar (se recorta a {@link #MAX_REQUESTS})
     * @param concurrency número de peticiones en vuelo (se recorta a {@link #MAX_CONCURRENCY})
     * @return resultados: tiempos, throughput y percentiles de latencia
     */
    public Map<String, Object> run(String path, int requests, int concurrency) {
        String requestedPath = (path == null || path.isBlank()) ? DEFAULT_PATH : path.trim();
        int safeRequests = Math.max(1, Math.min(requests, MAX_REQUESTS));
        int safeConcurrency = Math.max(1, Math.min(concurrency, MAX_CONCURRENCY));

        String baseUrl = baseUrl();
        Target target = resolveTarget(baseUrl, requestedPath);
        URI uri = target.uri();

        long[] latencies = new long[safeRequests];
        AtomicInteger dispatched = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger serverErrors = new AtomicInteger();

        long start = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int worker = 0; worker < safeConcurrency; worker++) {
                executor.submit(() -> {
                    while (true) {
                        int index = dispatched.getAndIncrement();
                        if (index >= safeRequests) {
                            return;
                        }
                        long requestStart = System.nanoTime();
                        try {
                            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
                            HttpResponse<Void> response = client.send(
                                    request, HttpResponse.BodyHandlers.discarding());
                            if (response.statusCode() >= 400) {
                                serverErrors.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        } finally {
                            latencies[index] = System.nanoTime() - requestStart;
                        }
                    }
                });
            }
        }

        long elapsedNanos = System.nanoTime() - start;
        double elapsedMs = elapsedNanos / 1_000_000.0;
        double requestsPerSecond = elapsedMs > 0 ? safeRequests / (elapsedMs / 1000.0) : 0;

        long[] sorted = latencies.clone();
        Arrays.sort(sorted);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("path", target.path());
        data.put("url", target.uri().toString());
        data.put("peticionesTotales", safeRequests);
        data.put("concurrencia", safeConcurrency);
        data.put("msTranscurridos", round(elapsedMs));
        data.put("peticionesPorSegundo", round(requestsPerSecond));
        data.put("latenciaMinMs", toMs(sorted[0]));
        data.put("latenciaP50Ms", toMs(percentile(sorted, 50)));
        data.put("latenciaP95Ms", toMs(percentile(sorted, 95)));
        data.put("latenciaP99Ms", toMs(percentile(sorted, 99)));
        data.put("latenciaMaxMs", toMs(sorted[sorted.length - 1]));
        data.put("erroresDeRed", errors.get());
        data.put("respuestasConError", serverErrors.get());
        data.put("nota",
                "Los valores se recortan a " + MAX_REQUESTS + " peticiones y " + MAX_CONCURRENCY
                        + " de concurrencia para que la demo no se sobrecargue.");
        data.put("explicacion",
                "Esto es el BLOQUE LATERAL de la pirámide (carga y rendimiento) que, en el artículo, "
                        + "aparece al lado de la pirámide de unitarias/integración/UI y que se "
                        + "pregunta en el perfil senior. Se mide contra la aplicación real en "
                        + "marcha, con un cliente HTTP y hilos virtuales, y se reportan percentiles "
                        + "en lugar de la media: la media esconde los picos. El p99 es el número "
                        + "que mira un usuario que tiene la impresión de que la web va lenta.");

        return data;
    }

    /**
     * Resuelve la ruta pedida contra la URL base.
     *
     * <p>Si la ruta no forma una URI válida, se cae al endpoint por defecto: el objetivo es que
     * un parámetro mal escrito no rompa la demo.
     */
    private Target resolveTarget(String baseUrl, String path) {
        try {
            return new Target(path, URI.create(baseUrl + path));
        } catch (IllegalArgumentException e) {
            return new Target(DEFAULT_PATH, URI.create(baseUrl + DEFAULT_PATH));
        }
    }

    /**
     * URL base de la aplicación en marcha.
     *
     * <p>Se toma del contexto de la petición en curso, de forma que funciona igual en local, en
     * los tests (puerto aleatorio) y en AWS (IP elástica). Si no hay contexto de petición, se
     * cae al puerto configurado.
     */
    private String baseUrl() {
        try {
            return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        } catch (IllegalStateException e) {
            return "http://localhost:" + serverPort;
        }
    }

    /** Percentil sobre un array YA ORDENADO, con la convención "nearest-rank". */
    private long percentile(long[] sorted, int p) {
        if (sorted.length == 0) {
            return 0L;
        }
        int rank = (int) Math.ceil(p / 100.0 * sorted.length);
        int index = Math.min(Math.max(rank - 1, 0), sorted.length - 1);
        return sorted[index];
    }

    private double toMs(long nanos) {
        return Math.round(nanos / 1_000.0) / 100.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Destino final de la prueba de carga: la ruta efectiva y su URI. */
    private record Target(String path, URI uri) {
    }
}
