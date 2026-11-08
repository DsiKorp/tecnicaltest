package com.dsklabs.epam.tecnicaltest.q08redis;

import com.dsklabs.epam.tecnicaltest.support.web.ApiResponse;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de la pregunta 8 (datos enormes en una caché Redis).
 *
 * <ul>
 *   <li>{@code GET /q08/store} — qué almacén está activo y por qué (incluye el fallback).</li>
 *   <li>{@code POST /q08/blob/{id}} — guarda un payload grande (comprimido y troceado).</li>
 *   <li>{@code GET /q08/blob/{id}} — lo recupera y comprueba que el round-trip es idéntico.</li>
 *   <li>{@code GET /q08/benchmark?sizeMb=4} — mide tiempo de escritura/lectura y ratio de
 *       compresión.</li>
 * </ul>
 */
@RestController
@RequestMapping("/q08")
public class RedisDemoController {

    private final ChunkedBlobStore store;
    private final ChunkedBlobStoreConfiguration configuration;

    @Value("${epam.q08.max-payload-mb:32}")
    private int maxPayloadMb;

    public RedisDemoController(ChunkedBlobStore store, ChunkedBlobStoreConfiguration configuration) {
        this.store = store;
        this.configuration = configuration;
    }

    @GetMapping("/store")
    public ApiResponse<Map<String, Object>> store() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("almacenActivo", store.kind());
        data.put("modoConfigurado", configuration.mode());
        data.put("motivo", configuration.reason());
        data.put("chunkSizeBytes", configuration.chunkSize());
        data.put("redisDisponible", "redis".equals(store.kind()));
        data.put("limiteRedisPorItem", "512 MB — un solo item (hash) no puede superarlo");
        data.put("nota",
                "Si Redis no responde, la app NO se cae: cae al almacén en memoria. "
                        + "En AWS no hay Redis en la instancia, y ese es el comportamiento esperado.");

        return ApiResponse.of("q08-store", "Estado del almacén de blobs", data);
    }

    @PostMapping(value = "/blob/{id}", consumes = MediaType.ALL_VALUE)
    public ApiResponse<Map<String, Object>> save(@PathVariable String id,
                                                 @RequestBody(required = false) byte[] payload) {
        byte[] data = payload == null ? new byte[0] : payload;
        long start = System.nanoTime();
        BlobMetadata meta = store.store(id, data);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", meta.id());
        body.put("almacen", meta.storeKind());
        body.put("originalSizeBytes", meta.originalSize());
        body.put("storedSizeBytes", meta.storedSize());
        body.put("chunkCount", meta.chunkCount());
        body.put("comprimido", meta.compressed());
        body.put("ratioCompresion", meta.compressionRatio());
        body.put("tiempoEscrituraMs", elapsedMs);
        body.put("claveRedis", RedisChunkedBlobStore.keyFor(id));

        return ApiResponse.of("q08-save", "Payload comprimido y troceado", body);
    }

    @GetMapping("/blob/{id}")
    public ApiResponse<Map<String, Object>> load(@PathVariable String id) {
        long start = System.nanoTime();
        byte[] payload = store.load(id);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("almacen", store.kind());
        body.put("bytesRecuperados", payload.length);
        body.put("tiempoLecturaMs", elapsedMs);
        body.put("metadatos", store.metadata(id));
        body.put("previsualizacion", new String(payload, 0, Math.min(120, payload.length), StandardCharsets.UTF_8));

        return ApiResponse.of("q08-load", "Payload recuperado y reensamblado desde los chunks", body);
    }

    /**
     * Mide el ciclo completo de escritura/lectura con un payload sintético.
     *
     * <p>El payload es texto muy repetitivo a propósito: es el caso donde gzip gana. Con datos
     * aleatorios, la compresión no aporta nada y solo añade CPU.
     */
    @GetMapping("/benchmark")
    public ApiResponse<Map<String, Object>> benchmark(@RequestParam(defaultValue = "4") int sizeMb) {
        int safeMb = Math.max(1, Math.min(sizeMb, maxPayloadMb));
        byte[] payload = buildPayload(safeMb);

        String id = "demo-" + UUID.randomUUID();

        long storeStart = System.nanoTime();
        BlobMetadata meta = store.store(id, payload);
        long storeMs = (System.nanoTime() - storeStart) / 1_000_000;

        long loadStart = System.nanoTime();
        byte[] loaded = store.load(id);
        long loadMs = (System.nanoTime() - loadStart) / 1_000_000;

        boolean identical = java.util.Arrays.equals(payload, loaded);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("almacen", store.kind());
        data.put("payloadMB", safeMb);
        data.put("originalSizeBytes", meta.originalSize());
        data.put("storedSizeBytes", meta.storedSize());
        data.put("chunkCount", meta.chunkCount());
        data.put("comprimido", meta.compressed());
        data.put("ratioCompresion", meta.compressionRatio());
        data.put("tiempoEscrituraMs", storeMs);
        data.put("tiempoLecturaMs", loadMs);
        data.put("roundTripIdentico", identical);
        data.put("conclusion",
                "Trocear y comprimir hace que el blob quepa en Redis, pero no cambia el problema "
                        + "de fondo: estás usando una caché en RAM como almacén de datos. "
                        + "La solución correcta es un almacén de documentos (S3, MongoDB, Postgres...)");

        return ApiResponse.of("q08-benchmark", "Ciclo completo de escritura y lectura", data);
    }

    /** Payload sintético: texto repetitivo, que es donde la compresión sí rinde. */
    private byte[] buildPayload(int sizeMb) {
        String unit = """
                Pedido #1042: 3 x SKU-001, 2 x SKU-002, 1 x SKU-003.
                Estado: PENDIENTE DE PAGO. Cliente: Ana Martínez.
                """;
        StringBuilder sb = new StringBuilder();
        int target = sizeMb * 1024 * 1024;
        while (sb.length() < target) {
            sb.append(unit);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
